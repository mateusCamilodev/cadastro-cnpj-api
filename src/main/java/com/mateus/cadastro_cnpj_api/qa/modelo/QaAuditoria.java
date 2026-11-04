package com.mateus.cadastro_cnpj_api.qa.modelo;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Trilha de auditoria do QA Center: quem iniciou, cancelou, reexecutou ou configurou algo, e quando
@Entity
@Table(name = "qa_auditoria", indexes = @Index(name = "ix_qa_aud_data", columnList = "dataHora"))
public class QaAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String usuario;

    @Column(nullable = false, length = 40)
    private String acao;

    @Column(length = 80)
    private String alvo;

    @Column(length = 500)
    private String detalhe;

    @Column(nullable = false)
    private LocalDateTime dataHora = LocalDateTime.now();

    protected QaAuditoria() {
    }

    public QaAuditoria(String usuario, String acao, String alvo, String detalhe) {
        this.usuario = usuario;
        this.acao = acao;
        this.alvo = alvo;
        this.detalhe = detalhe != null && detalhe.length() > 500 ? detalhe.substring(0, 500) : detalhe;
    }

    public Long getId() { return id; }
    public String getUsuario() { return usuario; }
    public String getAcao() { return acao; }
    public String getAlvo() { return alvo; }
    public String getDetalhe() { return detalhe; }
    public LocalDateTime getDataHora() { return dataHora; }
}
