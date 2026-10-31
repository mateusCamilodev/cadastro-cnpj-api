package com.mateus.cadastro_cnpj_api.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Uma linha do histórico da solicitação (o que aconteceu, quando, e com qual status)
@Entity
@Table(name = "historico_solicitacao")
public class EventoHistorico {

    // Long sequencial: a ordem do id = ordem cronológica dos eventos
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id")
    private Solicitacao solicitacao;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSolicitacao status;

    @Column(nullable = false, length = 500)
    private String descricao;

    protected EventoHistorico() {
    }

    public EventoHistorico(Solicitacao solicitacao, StatusSolicitacao status, String descricao) {
        this.solicitacao = solicitacao;
        this.dataHora = LocalDateTime.now();
        this.status = status;
        this.descricao = descricao;
    }

    // ===== Getters =====
    public Long getId() { return id; }
    public Solicitacao getSolicitacao() { return solicitacao; }
    public LocalDateTime getDataHora() { return dataHora; }
    public StatusSolicitacao getStatus() { return status; }
    public String getDescricao() { return descricao; }
}
