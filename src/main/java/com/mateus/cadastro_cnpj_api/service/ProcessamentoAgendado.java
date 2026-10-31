package com.mateus.cadastro_cnpj_api.service;

import com.mateus.cadastro_cnpj_api.model.Solicitacao;
import com.mateus.cadastro_cnpj_api.model.StatusSolicitacao;
import com.mateus.cadastro_cnpj_api.repository.SolicitacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Roda sozinho a cada X segundos e faz o status "andar":
//   RECEBIDO             → AGUARDANDO_DOCUMENTO  (após tempo-recebido)
//   AGUARDANDO_DOCUMENTO → EXPIRADO              (prazo do documento venceu)
//   EM_PROCESSAMENTO     → APROVADO / REPROVADO  (após tempo-processamento, via AnaliseSolicitacao)
// Os tempos ficam no application.properties.
@Component
public class ProcessamentoAgendado {

    private static final Logger log = LoggerFactory.getLogger(ProcessamentoAgendado.class);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final SolicitacaoRepository repository;
    private final AnaliseSolicitacao analise;
    private final Duration tempoRecebido;
    private final Duration prazoDocumento;
    private final Duration tempoProcessamento;

    public ProcessamentoAgendado(SolicitacaoRepository repository,
                                 AnaliseSolicitacao analise,
                                 @Value("${solicitacao.tempo-recebido}") Duration tempoRecebido,
                                 @Value("${solicitacao.prazo-documento}") Duration prazoDocumento,
                                 @Value("${solicitacao.tempo-processamento}") Duration tempoProcessamento) {
        this.repository = repository;
        this.analise = analise;
        this.tempoRecebido = tempoRecebido;
        this.prazoDocumento = prazoDocumento;
        this.tempoProcessamento = tempoProcessamento;
    }

    // Dentro de @Transactional, alterar a entidade já basta: o JPA grava as mudanças no fim do método
    @Scheduled(fixedDelayString = "${solicitacao.intervalo-agendador-ms}")
    @Transactional
    public void executar() {
        LocalDateTime agora = LocalDateTime.now();

        for (Solicitacao s : repository.findByStatusAndDataCriacaoBefore(
                StatusSolicitacao.RECEBIDO, agora.minus(tempoRecebido))) {
            s.setStatus(StatusSolicitacao.AGUARDANDO_DOCUMENTO);
            s.setPrazoDocumento(agora.plus(prazoDocumento));
            s.registrarEvento("Aguardando documentos até " + s.getPrazoDocumento().format(HORA)
                    + ". Documentos declarados: " + s.getDocumentos().size()
                    + ". Enviados até o momento: " + s.getTotalRecebidos() + " de " + s.getDocumentos().size());
            log.info("Solicitação {} → AGUARDANDO_DOCUMENTO (prazo até {})", s.getId(), s.getPrazoDocumento());
        }

        for (Solicitacao s : repository.findByStatusAndPrazoDocumentoBefore(
                StatusSolicitacao.AGUARDANDO_DOCUMENTO, agora)) {
            s.setStatus(StatusSolicitacao.EXPIRADO);
            s.registrarEvento("Prazo para envio dos documentos expirou. Enviados: " + s.getTotalRecebidos()
                    + " de " + s.getDocumentos().size());
            log.info("Solicitação {} → EXPIRADO (documento não enviado no prazo)", s.getId());
        }

        for (Solicitacao s : repository.findByStatusAndDataAtualizacaoBefore(
                StatusSolicitacao.EM_PROCESSAMENTO, agora.minus(tempoProcessamento))) {
            analise.analisar(s);
            log.info("Solicitação {} → {}", s.getId(), s.getStatus());
        }
    }
}
