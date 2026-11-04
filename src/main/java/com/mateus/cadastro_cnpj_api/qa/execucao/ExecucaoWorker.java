package com.mateus.cadastro_cnpj_api.qa.execucao;

import com.mateus.cadastro_cnpj_api.qa.catalogo.EndpointCatalogo;
import com.mateus.cadastro_cnpj_api.qa.cenarios.CenarioCatalogo;
import com.mateus.cadastro_cnpj_api.qa.cenarios.CenarioQa;
import com.mateus.cadastro_cnpj_api.qa.api.QaMapeador;
import com.mateus.cadastro_cnpj_api.qa.config.QaProperties;
import com.mateus.cadastro_cnpj_api.qa.gestao.ConfiguracaoQaService;
import com.mateus.cadastro_cnpj_api.qa.modelo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.time.Duration;
import java.util.function.Supplier;

// Execution Worker: roda os cenários de UMA execução, fora da requisição HTTP que a criou.
// Cenários em paralelo conforme as Configurações (máx. 4); cada um com seus próprios dados de teste; respeita cancelamento e tempo máximo.
@Component
public class ExecucaoWorker {

    private static final Logger log = LoggerFactory.getLogger(ExecucaoWorker.class);
    private final QaExecucaoRepository execucoes;
    private final QaResultadoRepository resultados;
    private final CenarioCatalogo cenarios;
    private final EndpointCatalogo endpoints;
    private final ProcessadorResultados processador;
    private final AmbientesQa ambientes;
    private final QaProperties properties;
    private final ObjectMapper mapper;
    private final ThreadPoolTaskExecutor poolCenarios;
    private final ConfiguracaoQaService configuracoes;
    private final QaMapeador mapeador;

    public ExecucaoWorker(QaExecucaoRepository execucoes, QaResultadoRepository resultados, CenarioCatalogo cenarios,
                          EndpointCatalogo endpoints, ProcessadorResultados processador, AmbientesQa ambientes,
                          QaProperties properties, ObjectMapper mapper,
                          @Qualifier("qaCenarios") ThreadPoolTaskExecutor poolCenarios,
                          ConfiguracaoQaService configuracoes, QaMapeador mapeador) {
        this.configuracoes = configuracoes;
        this.mapeador = mapeador;
        this.execucoes = execucoes;
        this.resultados = resultados;
        this.cenarios = cenarios;
        this.endpoints = endpoints;
        this.processador = processador;
        this.ambientes = ambientes;
        this.properties = properties;
        this.mapper = mapper;
        this.poolCenarios = poolCenarios;
    }

    public void executar(UUID execucaoId, AtomicBoolean cancelamento) {
        try {
            QaExecucao execucao = execucoes.findById(execucaoId).orElseThrow();
            processador.iniciarExecucao(execucaoId);

            // Tempos e paralelismo vêm das Configurações (tela), com o tempo por requisição da própria execução se informado
            Duration timeoutRequisicao = configuracoes.timeoutRequisicao(mapeador.configuracao(execucao).timeoutRequisicaoSegundos());
            Duration timeoutExecucao = configuracoes.timeoutExecucao();
            ClienteQa cliente = new ClienteQa(ambientes.url(execucao.getAmbiente()), timeoutRequisicao, mapper);
            Supplier<String> token = tokenDaContaDeTeste(cliente);
            String prefixo = execucaoId.toString().substring(0, 8);

            Semaphore vagas = new Semaphore(configuracoes.cenariosParalelos());
            List<CompletableFuture<Void>> tarefas = resultados.findByExecucaoIdOrderByIdAsc(execucaoId).stream()
                    .map(r -> CompletableFuture.runAsync(() -> {
                        vagas.acquireUninterruptibly();
                        try {
                            rodarCenario(r, cliente, token, cancelamento::get, prefixo);
                        } finally {
                            vagas.release();
                        }
                    }, poolCenarios))
                    .toList();

            try {
                CompletableFuture.allOf(tarefas.toArray(CompletableFuture[]::new))
                        .get(timeoutExecucao.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                cancelamento.set(true);
                processador.finalizarExecucao(execucaoId, StatusExecucao.ERROR,
                        "Tempo máximo da execução excedido (" + timeoutExecucao.toMinutes() + " min)");
                return;
            }

            if (cancelamento.get()) {
                processador.finalizarExecucao(execucaoId, StatusExecucao.CANCELLED, "Execução cancelada");
            } else {
                processador.finalizarExecucao(execucaoId, StatusExecucao.COMPLETED, null);
            }
        } catch (Exception e) {
            log.error("Falha na execução QA {}", execucaoId, e);
            try {
                processador.finalizarExecucao(execucaoId, StatusExecucao.ERROR, "Falha interna do executor: " + e.getClass().getSimpleName());
            } catch (Exception ignorada) {
                log.error("Não foi possível finalizar a execução QA {}", execucaoId, ignorada);
            }
        }
    }

    private void rodarCenario(QaResultado resultado, ClienteQa cliente, Supplier<String> token,
                              BooleanSupplier cancelado, String prefixo) {
        if (cancelado.getAsBoolean()) {
            processador.concluirTeste(resultado.getId(), StatusTeste.CANCELLED, null, null, "Execução cancelada antes deste cenário começar", null);
            return;
        }
        CenarioQa cenario = cenarios.buscar(resultado.getCenarioId()).orElse(null);
        if (cenario == null) {
            processador.concluirTeste(resultado.getId(), StatusTeste.ERROR, null, null, "Cenário não existe mais no catálogo", null);
            return;
        }

        processador.iniciarTeste(resultado.getId());
        ContextoQa ctx = new ContextoQa(cliente, endpoints, token, cancelado, mapper, prefixo);
        long inicio = System.nanoTime();
        StatusTeste status;
        String mensagem;
        try {
            cenario.logica().executar(ctx, cenario.id());
            long segundos = (System.nanoTime() - inicio) / 1_000_000_000;
            ContextoQa.ValidacaoQa falha = ctx.validacoes().stream().filter(v -> !v.ok()).findFirst().orElse(null);
            if (segundos > cenario.timeoutSegundos()) {
                status = StatusTeste.ERROR;
                mensagem = "Tempo limite do cenário excedido (" + segundos + " s de " + cenario.timeoutSegundos() + " s)";
            } else if (ctx.validacoes().isEmpty()) {
                status = StatusTeste.ERROR;
                mensagem = "O cenário não executou nenhuma validação";
            } else if (falha != null) {
                status = StatusTeste.FAILED;
                mensagem = falha.descricao() + ": esperado " + falha.esperado() + ", recebido " + falha.recebido();
            } else {
                status = StatusTeste.PASSED;
                mensagem = null;
            }
        } catch (ContextoQa.Cancelado e) {
            status = StatusTeste.CANCELLED;
            mensagem = "Execução cancelada";
        } catch (ContextoQa.PreCondicaoFalhou e) {
            status = StatusTeste.SKIPPED;
            mensagem = "Pré-condição não atendida: " + e.getMessage();
        } catch (Exception e) {
            status = StatusTeste.ERROR;
            mensagem = "Erro ao executar: " + (e.getMessage() != null ? Mascarador.texto(e.getMessage()) : e.getClass().getSimpleName());
        }

        RespostaQa principal = ctx.principal();
        Integer http = principal != null ? principal.status() : null;
        Long duracao = principal != null ? principal.duracaoMs() : null;
        processador.concluirTeste(resultado.getId(), status, http, duracao, mensagem, evidencias(cenario, ctx, inicio));
    }

    private String evidencias(CenarioQa cenario, ContextoQa ctx, long inicio) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("descricao", cenario.descricao());
        e.put("preCondicoes", cenario.preCondicoes());
        e.put("esperado", cenario.esperado());
        e.put("criticidade", cenario.criticidade());
        e.put("timeoutSegundos", cenario.timeoutSegundos());
        e.put("duracaoTotalMs", (System.nanoTime() - inicio) / 1_000_000);
        e.put("validacoes", ctx.validacoes());
        e.put("chamadas", ctx.chamadas());
        return mapper.writeValueAsString(e);
    }

    // Login da conta de teste, feito uma vez por execução e compartilhado entre os cenários.
    // Se falhar, os cenários que precisam de token ficam SKIPPED (pré-condição).
    private Supplier<String> tokenDaContaDeTeste(ClienteQa cliente) {
        return new Supplier<>() {
            private String token;
            private String erro;

            @Override
            public synchronized String get() {
                if (token != null) return token;
                if (erro != null) throw new ContextoQa.PreCondicaoFalhou(erro);
                QaProperties.ContaTeste conta = properties.contaTeste();
                if (conta == null || conta.email() == null || conta.email().isBlank()) {
                    erro = "Conta de teste do QA Center não configurada (qa.conta-teste.email/senha)";
                    throw new ContextoQa.PreCondicaoFalhou(erro);
                }
                String corpo = mapper.writeValueAsString(Map.of("email", conta.email(), "senha", conta.senha()));
                RespostaQa r = cliente.json("Login da conta de teste", false, endpoints.buscar("AUTH_TOKEN").orElseThrow(),
                        Map.of(), Map.of(), corpo, null).resposta();
                String valor = r.json().path("accessToken").asString("");
                if (r.status() != 200 || valor.isBlank()) {
                    erro = "Login da conta de teste falhou (HTTP " + r.status() + ")";
                    throw new ContextoQa.PreCondicaoFalhou(erro);
                }
                token = valor;
                return token;
            }
        };
    }
}
