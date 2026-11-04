package com.mateus.cadastro_cnpj_api.qa.api;

import com.mateus.cadastro_cnpj_api.qa.QaException;
import com.mateus.cadastro_cnpj_api.qa.catalogo.EndpointCatalogo;
import com.mateus.cadastro_cnpj_api.qa.catalogo.EndpointQa;
import com.mateus.cadastro_cnpj_api.qa.cenarios.CenarioCatalogo;
import com.mateus.cadastro_cnpj_api.qa.cenarios.CenarioQa;
import com.mateus.cadastro_cnpj_api.qa.execucao.AmbientesQa;
import com.mateus.cadastro_cnpj_api.qa.modelo.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

// Leituras do QA Center: catálogo (com estatísticas reais), histórico de execuções e detalhes
@Service
public class ConsultaQaService {

    private final EndpointCatalogo endpoints;
    private final CenarioCatalogo cenarios;
    private final AmbientesQa ambientes;
    private final QaExecucaoRepository execucoes;
    private final QaResultadoRepository resultados;
    private final QaEventoRepository eventos;
    private final QaMapeador mapeador;

    public ConsultaQaService(EndpointCatalogo endpoints, CenarioCatalogo cenarios, AmbientesQa ambientes,
                             QaExecucaoRepository execucoes, QaResultadoRepository resultados,
                             QaEventoRepository eventos, QaMapeador mapeador) {
        this.endpoints = endpoints;
        this.cenarios = cenarios;
        this.ambientes = ambientes;
        this.execucoes = execucoes;
        this.resultados = resultados;
        this.eventos = eventos;
        this.mapeador = mapeador;
    }

    // ===== Catálogo =====

    public List<QaDtos.EndpointDto> endpoints() {
        List<QaResultado> todos = resultados.findAll();
        return endpoints.todos().stream().map(ep -> endpoint(ep, todos)).toList();
    }

    public QaDtos.EndpointDto endpoint(String id) {
        EndpointQa ep = endpoints.buscar(id).orElseThrow(() -> new QaException(HttpStatus.NOT_FOUND, "Endpoint não encontrado no catálogo"));
        return endpoint(ep, resultados.findAll());
    }

    private QaDtos.EndpointDto endpoint(EndpointQa ep, List<QaResultado> todos) {
        List<CenarioQa> doEndpoint = cenarios.todos().stream().filter(c -> c.endpointId().equals(ep.id())).toList();
        List<Categoria> categorias = doEndpoint.stream().map(CenarioQa::categoria).distinct().toList();
        List<QaResultado> doHistorico = todos.stream()
                .filter(r -> r.getEndpointId().equals(ep.id()) && r.getStatus().terminado() && r.getFinalizadoEm() != null)
                .sorted(Comparator.comparing(QaResultado::getFinalizadoEm).reversed())
                .toList();
        QaResultado ultimo = doHistorico.isEmpty() ? null : doHistorico.get(0);
        List<Ambiente> permitidos = Arrays.stream(Ambiente.values()).filter(ep.ambientesPermitidos()::contains).toList();
        return new QaDtos.EndpointDto(ep.id(), ep.nome(), ep.metodo(), ep.caminho(), ep.grupo(), ep.descricao(),
                ep.modificaDados(), permitidos, categorias, doEndpoint.size(), doHistorico.size(),
                ultimo != null ? ultimo.getStatus() : null, ultimo != null ? ultimo.getFinalizadoEm() : null);
    }

    public List<QaDtos.CenarioDto> cenarios() {
        List<QaResultado> todos = resultados.findAll();
        return cenarios.todos().stream().map(c -> cenario(c, todos)).toList();
    }

    public QaDtos.CenarioDto cenario(String id) {
        return cenario(cenarios.buscar(id).orElseThrow(() -> new QaException(HttpStatus.NOT_FOUND, "Cenário não encontrado no catálogo")),
                resultados.findAll());
    }

    private QaDtos.CenarioDto cenario(CenarioQa c, List<QaResultado> todos) {
        EndpointQa ep = endpoints.buscar(c.endpointId()).orElseThrow();
        QaResultado ultimo = todos.stream()
                .filter(r -> r.getCenarioId().equals(c.id()) && r.getStatus().terminado() && r.getFinalizadoEm() != null)
                .max(Comparator.comparing(QaResultado::getFinalizadoEm)).orElse(null);
        return new QaDtos.CenarioDto(c.id(), c.nome(), c.descricao(), ep.id(), ep.nome(), ep.metodo(), ep.caminho(),
                c.categoria(), c.categoria().rotulo(), c.criticidade(), c.timeoutSegundos(), c.preCondicoes(), c.esperado(),
                ultimo != null ? ultimo.getStatus() : null, ultimo != null ? ultimo.getFinalizadoEm() : null);
    }

    public List<QaDtos.CategoriaDto> categorias() {
        return Arrays.stream(Categoria.values())
                .map(cat -> new QaDtos.CategoriaDto(cat, cat.rotulo(),
                        (int) cenarios.todos().stream().filter(c -> c.categoria() == cat).count()))
                .toList();
    }

    public List<QaDtos.AmbienteDto> ambientes() {
        return Arrays.stream(Ambiente.values())
                .map(a -> new QaDtos.AmbienteDto(a, a.rotulo(), ambientes.disponivel(a)))
                .toList();
    }

    // ===== Execuções =====

    // Filtros do histórico. "resultado": COM_FALHAS (algum reprovado/erro) ou SEM_FALHAS (concluída sem reprovado/erro).
    // "ativas": só PENDING/RUNNING (tela "Em andamento").
    public QaDtos.Pagina<QaDtos.ExecucaoDto> execucoes(StatusExecucao status, Ambiente ambiente, String usuario,
                                                        LocalDate de, LocalDate ate, String busca, String endpointId,
                                                        UUID suiteId, String resultado, boolean ativas,
                                                        int pagina, int tamanho) {
        int tam = Math.max(1, Math.min(tamanho, 100));
        int pag = Math.max(0, pagina);
        String termo = busca == null ? "" : busca.trim().toLowerCase();
        if (resultado != null && !resultado.isBlank() && !Set.of("COM_FALHAS", "SEM_FALHAS").contains(resultado)) {
            throw new QaException(HttpStatus.BAD_REQUEST, "Filtro de resultado inválido. Valores aceitos: COM_FALHAS, SEM_FALHAS");
        }
        if (endpointId != null && !endpointId.isBlank() && endpoints.buscar(endpointId).isEmpty()) {
            throw new QaException(HttpStatus.BAD_REQUEST, "Endpoint desconhecido: " + endpointId);
        }
        Set<UUID> comEndpoint = endpointId == null || endpointId.isBlank() ? null : resultados.findAll().stream()
                .filter(r -> r.getEndpointId().equals(endpointId)).map(QaResultado::getExecucaoId).collect(Collectors.toSet());
        List<QaExecucao> filtradas = execucoes.findAllByOrderByCriadaEmDesc().stream()
                .filter(e -> status == null || e.getStatus() == status)
                .filter(e -> !ativas || e.getStatus().ativo())
                .filter(e -> ambiente == null || e.getAmbiente() == ambiente)
                .filter(e -> usuario == null || usuario.isBlank() || e.getUsuario().equalsIgnoreCase(usuario.trim()))
                .filter(e -> de == null || !e.getCriadaEm().toLocalDate().isBefore(de))
                .filter(e -> ate == null || !e.getCriadaEm().toLocalDate().isAfter(ate))
                .filter(e -> termo.isEmpty() || e.getId().toString().contains(termo) || e.getUsuario().toLowerCase().contains(termo))
                .filter(e -> comEndpoint == null || comEndpoint.contains(e.getId()))
                .filter(e -> suiteId == null || suiteId.equals(e.getSuiteId()))
                .filter(e -> resultado == null || resultado.isBlank()
                        || ("COM_FALHAS".equals(resultado) ? e.getReprovados() + e.getErros() > 0
                        : e.getStatus() == StatusExecucao.COMPLETED && e.getReprovados() + e.getErros() == 0))
                .toList();
        List<QaDtos.ExecucaoDto> itens = filtradas.stream().skip((long) pag * tam).limit(tam).map(mapeador::execucao).toList();
        return new QaDtos.Pagina<>(itens, filtradas.size(), pag, tam);
    }

    public QaDtos.ExecucaoDetalheDto detalhe(UUID id) {
        QaExecucao e = execucoes.findById(id).orElseThrow(() -> new QaException(HttpStatus.NOT_FOUND, "Execução não encontrada"));
        return new QaDtos.ExecucaoDetalheDto(mapeador.execucao(e),
                resultados.findByExecucaoIdOrderByIdAsc(id).stream().map(r -> mapeador.resultado(r, false)).toList(),
                eventos.findByExecucaoIdOrderByIdAsc(id).stream().map(mapeador::evento).toList());
    }

    public List<QaDtos.ResultadoDto> resultados(UUID id) {
        execucoes.findById(id).orElseThrow(() -> new QaException(HttpStatus.NOT_FOUND, "Execução não encontrada"));
        return resultados.findByExecucaoIdOrderByIdAsc(id).stream().map(r -> mapeador.resultado(r, true)).toList();
    }

    public boolean ativa(UUID id) {
        return execucoes.findById(id).map(e -> e.getStatus().ativo()).orElse(false);
    }
}
