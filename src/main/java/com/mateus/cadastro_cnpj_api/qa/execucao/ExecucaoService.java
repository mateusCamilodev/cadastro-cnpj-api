package com.mateus.cadastro_cnpj_api.qa.execucao;

import com.mateus.cadastro_cnpj_api.qa.QaException;
import com.mateus.cadastro_cnpj_api.qa.api.QaDtos;
import com.mateus.cadastro_cnpj_api.qa.api.QaMapeador;
import com.mateus.cadastro_cnpj_api.qa.catalogo.EndpointCatalogo;
import com.mateus.cadastro_cnpj_api.qa.catalogo.EndpointQa;
import com.mateus.cadastro_cnpj_api.qa.cenarios.CenarioCatalogo;
import com.mateus.cadastro_cnpj_api.qa.cenarios.CenarioQa;
import com.mateus.cadastro_cnpj_api.qa.gestao.AuditoriaQa;
import com.mateus.cadastro_cnpj_api.qa.gestao.ConfiguracaoQaService;
import com.mateus.cadastro_cnpj_api.qa.gestao.SuiteService;
import com.mateus.cadastro_cnpj_api.qa.modelo.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

// Execution Manager (orquestrador): valida a seleção, aplica limites e proteção contra duplicidade, cria a execução
// (com todos os cenários em PENDING), registra a auditoria e entrega ao worker. A requisição HTTP volta na hora (202).
@Service
public class ExecucaoService {

    private final QaExecucaoRepository execucoes;
    private final QaResultadoRepository resultados;
    private final EndpointCatalogo endpoints;
    private final CenarioCatalogo cenarios;
    private final AmbientesQa ambientes;
    private final ExecucaoWorker worker;
    private final ProcessadorResultados processador;
    private final QaMapeador mapeador;
    private final ConfiguracaoQaService configuracoes;
    private final SuiteService suites;
    private final AuditoriaQa auditoria;
    private final ThreadPoolTaskExecutor poolExecucoes;

    // Pedido de cancelamento de cada execução em andamento (o worker consulta antes de cada cenário)
    private final Map<UUID, AtomicBoolean> cancelamentos = new ConcurrentHashMap<>();

    public ExecucaoService(QaExecucaoRepository execucoes, QaResultadoRepository resultados, EndpointCatalogo endpoints,
                           CenarioCatalogo cenarios, AmbientesQa ambientes, ExecucaoWorker worker,
                           ProcessadorResultados processador, QaMapeador mapeador, ConfiguracaoQaService configuracoes,
                           SuiteService suites, AuditoriaQa auditoria,
                           @Qualifier("qaExecucoes") ThreadPoolTaskExecutor poolExecucoes) {
        this.execucoes = execucoes;
        this.resultados = resultados;
        this.endpoints = endpoints;
        this.cenarios = cenarios;
        this.ambientes = ambientes;
        this.worker = worker;
        this.processador = processador;
        this.mapeador = mapeador;
        this.configuracoes = configuracoes;
        this.suites = suites;
        this.auditoria = auditoria;
        this.poolExecucoes = poolExecucoes;
    }

    public QaDtos.ExecucaoDto criar(QaDtos.NovaExecucaoRequest pedido, String usuario) {
        return criar(pedido, usuario, null);
    }

    // Reexecuta exatamente os mesmos cenários, no mesmo ambiente e com o mesmo tempo limite (a original não muda)
    public QaDtos.ExecucaoDto reexecutar(UUID origemId, String usuario) {
        QaExecucao origem = buscar(origemId);
        QaDtos.Configuracao c = mapeador.configuracao(origem);
        Selecao selecao = new Selecao(cenariosPorId(c.cenarioIds()), c.endpointIds(), c.categorias(), c.suiteId(), c.suiteNome());
        return iniciar(origem.getAmbiente(), selecao, c.timeoutRequisicaoSegundos(), usuario, origemId);
    }

    private QaDtos.ExecucaoDto criar(QaDtos.NovaExecucaoRequest pedido, String usuario, UUID origemId) {
        if (pedido == null || pedido.ambiente() == null) {
            throw new QaException(HttpStatus.BAD_REQUEST, "Informe o ambiente (DESENVOLVIMENTO ou HOMOLOGACAO)");
        }
        Integer timeout = configuracoes.validarTimeoutDaExecucao(pedido.timeoutRequisicaoSegundos());
        return iniciar(pedido.ambiente(), selecionar(pedido), timeout, usuario, origemId);
    }

    private QaDtos.ExecucaoDto iniciar(Ambiente ambiente, Selecao selecao, Integer timeoutRequisicao, String usuario, UUID origemId) {
        validarAmbiente(ambiente, selecao.cenarios());
        if (!ambientes.disponivel(ambiente)) {
            throw new QaException(HttpStatus.SERVICE_UNAVAILABLE, "O ambiente " + ambiente.rotulo() + " está indisponível agora");
        }

        List<String> ids = selecao.cenarios().stream().map(CenarioQa::id).toList();
        QaDtos.Configuracao configuracao = new QaDtos.Configuracao(selecao.endpointIds(), selecao.categorias(), ids,
                selecao.suiteId(), selecao.suiteNome(), timeoutRequisicao);
        String assinatura = assinatura(ambiente, ids);

        QaExecucao execucao;
        AtomicBoolean cancelamento = new AtomicBoolean(false);
        // Bloco sincronizado: a checagem dos limites e a criação acontecem juntas (sem corrida entre dois cliques)
        synchronized (this) {
            int porUsuario = configuracoes.execucoesPorUsuario();
            if (execucoes.existsByAssinaturaAndStatusIn(assinatura, StatusExecucao.ATIVOS)) {
                throw new QaException(HttpStatus.CONFLICT, "Já existe uma execução em andamento com esta mesma seleção");
            }
            if (execucoes.countByUsuarioAndStatusIn(usuario, StatusExecucao.ATIVOS) >= porUsuario) {
                throw new QaException(HttpStatus.TOO_MANY_REQUESTS,
                        "Você já tem " + porUsuario + " execução(ões) em andamento. Aguarde terminar ou cancele.");
            }
            if (execucoes.countByStatusIn(StatusExecucao.ATIVOS) >= configuracoes.execucoesSimultaneas()) {
                throw new QaException(HttpStatus.TOO_MANY_REQUESTS, "Limite de execuções simultâneas atingido. Tente em instantes.");
            }
            execucao = execucoes.save(new QaExecucao(usuario, ambiente, mapeador.json(configuracao), assinatura, ids.size(),
                    origemId, selecao.suiteId(), selecao.suiteNome()));
            for (CenarioQa c : selecao.cenarios()) {
                resultados.save(new QaResultado(execucao.getId(), c.id(), c.nome(), c.endpointId(), c.categoria()));
            }
            cancelamentos.put(execucao.getId(), cancelamento);
        }

        UUID id = execucao.getId();
        String curto = id.toString().substring(0, 8);
        String origem = origemId != null ? " (reexecução de " + origemId.toString().substring(0, 8) + ")" : "";
        String suite = selecao.suiteNome() != null ? " · suíte " + selecao.suiteNome() : "";
        processador.evento(id, "CRIADA", "Execução criada por " + usuario + origem + suite);
        auditoria.registrar(usuario, origemId != null ? AuditoriaQa.EXECUCAO_REEXECUTADA : AuditoriaQa.EXECUCAO_CRIADA, id.toString(),
                "#" + curto + " em " + ambiente.rotulo() + " com " + ids.size() + " cenário(s)" + suite + origem);
        try {
            poolExecucoes.execute(() -> {
                try {
                    worker.executar(id, cancelamento);
                } finally {
                    cancelamentos.remove(id);
                }
            });
        } catch (TaskRejectedException e) {
            cancelamentos.remove(id);
            processador.finalizarExecucao(id, StatusExecucao.ERROR, "Fila do executor cheia");
            throw new QaException(HttpStatus.TOO_MANY_REQUESTS, "Fila do executor cheia. Tente em instantes.");
        }
        return mapeador.execucao(buscar(id));
    }

    public QaDtos.ExecucaoDto cancelar(UUID id, String usuario) {
        QaExecucao execucao = buscar(id);
        AtomicBoolean pedido = cancelamentos.get(id);
        if (!execucao.getStatus().ativo() || pedido == null) {
            throw new QaException(HttpStatus.CONFLICT, "A execução não está em andamento (status " + execucao.getStatus() + ")");
        }
        if (pedido.compareAndSet(false, true)) {
            execucao.marcarCancelamento(usuario);
            execucoes.save(execucao);
            processador.evento(id, "CANCELAMENTO", "Cancelamento solicitado por " + usuario + ": os cenários que ainda não começaram serão cancelados");
            auditoria.registrar(usuario, AuditoriaQa.EXECUCAO_CANCELADA, id.toString(), "#" + id.toString().substring(0, 8));
        }
        return mapeador.execucao(buscar(id));
    }

    // ===== Seleção =====

    private record Selecao(List<CenarioQa> cenarios, List<String> endpointIds, List<Categoria> categorias,
                           UUID suiteId, String suiteNome) {
    }

    // Seleção → cenários. Prioridade: suíte; cenários escolhidos um a um; endpoints × categorias.
    private Selecao selecionar(QaDtos.NovaExecucaoRequest pedido) {
        if (pedido.suiteId() != null) {
            QaSuite suite = suites.buscar(pedido.suiteId());
            return new Selecao(cenariosPorId(suites.cenarios(suite)), List.of(), List.of(), suite.getId(), suite.getNome());
        }
        List<String> idsCenarios = pedido.cenarioIds() == null ? List.of() : pedido.cenarioIds();
        if (!idsCenarios.isEmpty()) {
            return new Selecao(cenariosPorId(idsCenarios), List.of(), List.of(), null, null);
        }
        List<String> idsEndpoints = pedido.endpointIds() == null ? List.of() : pedido.endpointIds();
        List<Categoria> categorias = pedido.categorias() == null ? List.of() : pedido.categorias();
        if (idsEndpoints.isEmpty()) throw new QaException(HttpStatus.BAD_REQUEST, "Selecione pelo menos um endpoint, cenário ou Test Suite");
        if (categorias.isEmpty()) throw new QaException(HttpStatus.BAD_REQUEST, "Selecione pelo menos uma categoria de teste");
        for (String id : idsEndpoints) {
            endpoints.buscar(id).orElseThrow(() -> new QaException(HttpStatus.BAD_REQUEST, "Endpoint desconhecido: " + id));
        }
        List<CenarioQa> escolhidos = cenarios.todos().stream()
                .filter(c -> idsEndpoints.contains(c.endpointId()) && categorias.contains(c.categoria()))
                .toList();
        if (escolhidos.isEmpty()) {
            throw new QaException(HttpStatus.BAD_REQUEST, "Nenhum cenário corresponde a esta seleção de endpoints e categorias");
        }
        return new Selecao(escolhidos, idsEndpoints, categorias, null, null);
    }

    private List<CenarioQa> cenariosPorId(List<String> ids) {
        if (ids == null || ids.isEmpty()) throw new QaException(HttpStatus.BAD_REQUEST, "Nenhum cenário selecionado");
        List<CenarioQa> lista = new ArrayList<>();
        for (String id : new LinkedHashSet<>(ids)) {
            lista.add(cenarios.buscar(id).orElseThrow(() -> new QaException(HttpStatus.BAD_REQUEST, "Cenário desconhecido: " + id)));
        }
        return lista;
    }

    private void validarAmbiente(Ambiente ambiente, List<CenarioQa> escolhidos) {
        for (CenarioQa c : escolhidos) {
            EndpointQa ep = endpoints.buscar(c.endpointId()).orElseThrow();
            if (!ep.ambientesPermitidos().contains(ambiente)) {
                throw new QaException(HttpStatus.BAD_REQUEST, "O endpoint " + ep.nome() + " não pode ser testado em " + ambiente.rotulo());
            }
        }
    }

    private QaExecucao buscar(UUID id) {
        return execucoes.findById(id).orElseThrow(() -> new QaException(HttpStatus.NOT_FOUND, "Execução não encontrada"));
    }

    private static String assinatura(Ambiente ambiente, List<String> cenarioIds) {
        List<String> ordenados = new ArrayList<>(cenarioIds);
        Collections.sort(ordenados);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((ambiente + "|" + String.join(",", ordenados)).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
