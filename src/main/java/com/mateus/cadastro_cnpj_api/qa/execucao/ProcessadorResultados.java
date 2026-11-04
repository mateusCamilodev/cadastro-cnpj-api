package com.mateus.cadastro_cnpj_api.qa.execucao;

import com.mateus.cadastro_cnpj_api.qa.api.QaMapeador;
import com.mateus.cadastro_cnpj_api.qa.modelo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// Result Processor: grava cada mudança em uma transação PRÓPRIA (REQUIRES_NEW) — a falha de um cenário
// nunca desfaz o resultado dos outros. Depois de gravar, publica o evento em tempo real (SSE).
@Service
public class ProcessadorResultados {

    private final QaExecucaoRepository execucoes;
    private final QaResultadoRepository resultados;
    private final QaEventoRepository eventos;
    private final EventosExecucao tempoReal;
    private final QaMapeador mapeador;

    public ProcessadorResultados(QaExecucaoRepository execucoes, QaResultadoRepository resultados,
                                 QaEventoRepository eventos, EventosExecucao tempoReal, QaMapeador mapeador) {
        this.execucoes = execucoes;
        this.resultados = resultados;
        this.eventos = eventos;
        this.tempoReal = tempoReal;
        this.mapeador = mapeador;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void iniciarExecucao(UUID execucaoId) {
        QaExecucao e = execucoes.findById(execucaoId).orElseThrow();
        e.iniciar();
        execucoes.save(e);
        registrar(execucaoId, "INICIO", "Execução iniciada no ambiente " + e.getAmbiente().rotulo() + " com " + e.getTotal() + " cenário(s)");
        tempoReal.publicar(execucaoId, "execucao", mapeador.execucao(e));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void iniciarTeste(Long resultadoId) {
        QaResultado r = resultados.findById(resultadoId).orElseThrow();
        if (r.getStatus().terminado()) {
            return;
        }
        r.iniciar();
        resultados.save(r);
        tempoReal.publicar(r.getExecucaoId(), "resultado", mapeador.resultado(r, false));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void concluirTeste(Long resultadoId, StatusTeste status, Integer httpStatus, Long duracaoMs,
                              String mensagem, String evidencias) {
        QaResultado r = resultados.findById(resultadoId).orElseThrow();
        if (r.getStatus().terminado()) {
            return;   // já fechado (ex.: execução encerrada por tempo máximo): não sobrescreve
        }
        r.concluir(status, httpStatus, duracaoMs, mensagem, evidencias);
        resultados.save(r);
        QaExecucao e = atualizarPlacar(r.getExecucaoId());

        String detalhe = status + (httpStatus != null ? " (HTTP " + httpStatus + ")" : "")
                + (duracaoMs != null ? " em " + duracaoMs + " ms" : "")
                + (mensagem != null && status != StatusTeste.PASSED ? " — " + mensagem : "");
        registrar(r.getExecucaoId(), "CENARIO", r.getCenarioId() + " " + r.getCenarioNome() + ": " + detalhe);
        tempoReal.publicar(r.getExecucaoId(), "resultado", mapeador.resultado(r, false));
        tempoReal.publicar(r.getExecucaoId(), "execucao", mapeador.execucao(e));
    }

    // Fecha a execução. Antes, qualquer cenário que tenha ficado sem terminar recebe um estado final:
    // assim uma execução nunca aparece como concluída com trabalhos pendentes.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizarExecucao(UUID execucaoId, StatusExecucao status, String mensagem) {
        List<QaResultado> abertos = resultados.findByExecucaoIdAndStatusIn(execucaoId, List.of(StatusTeste.PENDING, StatusTeste.RUNNING));
        for (QaResultado r : abertos) {
            StatusTeste destino = r.getStatus() == StatusTeste.RUNNING ? StatusTeste.ERROR
                    : status == StatusExecucao.CANCELLED ? StatusTeste.CANCELLED : StatusTeste.SKIPPED;
            r.concluir(destino, null, null, mensagem != null ? mensagem : "Não executado", null);
            resultados.save(r);
        }
        QaExecucao e = atualizarPlacar(execucaoId);
        e.finalizar(status, mensagem);
        execucoes.save(e);
        registrar(execucaoId, "FIM", "Execução " + status + ": " + e.getAprovados() + " aprovado(s), " + e.getReprovados()
                + " reprovado(s), " + e.getErros() + " erro(s), " + e.getPulados() + " bloqueado(s), " + e.getCancelados() + " cancelado(s)" + (mensagem != null ? " — " + mensagem : ""));
        tempoReal.encerrar(execucaoId, Map.of("execucao", mapeador.execucao(e)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evento(UUID execucaoId, String tipo, String mensagem) {
        registrar(execucaoId, tipo, mensagem);
    }

    private QaExecucao atualizarPlacar(UUID execucaoId) {
        QaExecucao e = execucoes.findById(execucaoId).orElseThrow();
        List<QaResultado> todos = resultados.findByExecucaoIdOrderByIdAsc(execucaoId);
        e.atualizarPlacar(
                contar(todos, StatusTeste.PASSED),
                contar(todos, StatusTeste.FAILED),
                contar(todos, StatusTeste.ERROR),
                contar(todos, StatusTeste.SKIPPED),
                contar(todos, StatusTeste.CANCELLED));
        return execucoes.save(e);
    }

    private static int contar(List<QaResultado> lista, StatusTeste status) {
        return (int) lista.stream().filter(r -> r.getStatus() == status).count();
    }

    private void registrar(UUID execucaoId, String tipo, String mensagem) {
        QaEvento evento = eventos.save(new QaEvento(execucaoId, tipo, mensagem));
        tempoReal.publicar(execucaoId, "log", mapeador.evento(evento));
    }
}
