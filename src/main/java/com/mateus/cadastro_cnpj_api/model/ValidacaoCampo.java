package com.mateus.cadastro_cnpj_api.model;

import jakarta.persistence.*;

// Resultado da análise de UM campo de UM documento (ex.: documento-valido.pdf / cpf / REPROVADO)
@Entity
@Table(name = "validacoes_documento")
public class ValidacaoCampo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id")
    private DocumentoSolicitacao documento;

    @Column(nullable = false, length = 30)
    private String campo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ResultadoValidacao resultado;

    // Só preenchido quando REPROVADO ("não confere com a solicitação" / "campo ausente no documento")
    @Column(length = 100)
    private String motivo;

    protected ValidacaoCampo() {
    }

    public ValidacaoCampo(DocumentoSolicitacao documento, String campo, ResultadoValidacao resultado, String motivo) {
        this.documento = documento;
        this.campo = campo;
        this.resultado = resultado;
        this.motivo = motivo;
    }

    // ===== Getters =====
    public Long getId() { return id; }
    public DocumentoSolicitacao getDocumento() { return documento; }
    public String getCampo() { return campo; }
    public ResultadoValidacao getResultado() { return resultado; }
    public String getMotivo() { return motivo; }
}
