package com.mateus.cadastro_cnpj_api.qa.modelo;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

// Linha do log de uma execução (início, cada cenário, cancelamento, fim...)
@Entity
@Table(name = "qa_eventos", indexes = @Index(name = "ix_qa_evt_execucao", columnList = "execucao_id"))
public class QaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execucao_id", nullable = false)
    private UUID execucaoId;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 500)
    private String mensagem;

    @Column(nullable = false)
    private LocalDateTime dataHora = LocalDateTime.now();

    protected QaEvento() {
    }

    public QaEvento(UUID execucaoId, String tipo, String mensagem) {
        this.execucaoId = execucaoId;
        this.tipo = tipo;
        this.mensagem = mensagem.length() > 500 ? mensagem.substring(0, 497) + "..." : mensagem;
    }

    public Long getId() { return id; }
    public UUID getExecucaoId() { return execucaoId; }
    public String getTipo() { return tipo; }
    public String getMensagem() { return mensagem; }
    public LocalDateTime getDataHora() { return dataHora; }
}
