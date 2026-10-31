package com.mateus.cadastro_cnpj_api.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Um arquivo declarado na criação da solicitação (e se já foi recebido)
@Entity
@Table(name = "documentos_solicitacao")
public class DocumentoSolicitacao {

    // Long sequencial: a ordem do id = ordem em que foram declarados
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id")
    private Solicitacao solicitacao;

    @Column(nullable = false)
    private String nomeArquivo;

    @Column(nullable = false)
    private boolean recebido;

    private LocalDateTime dataRecebimento;

    // Preenchido só quando o PDF é aceito (colunas pdf_* nesta mesma tabela)
    @Embedded
    private DadosExtraidos dadosExtraidos;

    // Resultado campo a campo, preenchido na análise (EM_PROCESSAMENTO → APROVADO/REPROVADO)
    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ValidacaoCampo> validacoes = new ArrayList<>();

    protected DocumentoSolicitacao() {
    }

    public DocumentoSolicitacao(Solicitacao solicitacao, String nomeArquivo) {
        this.solicitacao = solicitacao;
        this.nomeArquivo = nomeArquivo;
        this.recebido = false;
    }

    public void marcarRecebido(DadosExtraidos dadosExtraidos) {
        this.recebido = true;
        this.dataRecebimento = LocalDateTime.now();
        this.dadosExtraidos = dadosExtraidos;
    }

    public void adicionarValidacao(String campo, ResultadoValidacao resultado, String motivo) {
        validacoes.add(new ValidacaoCampo(this, campo, resultado, motivo));
    }

    // ===== Getters =====
    public Long getId() { return id; }
    public Solicitacao getSolicitacao() { return solicitacao; }
    public String getNomeArquivo() { return nomeArquivo; }
    public boolean isRecebido() { return recebido; }
    public LocalDateTime getDataRecebimento() { return dataRecebimento; }
    public DadosExtraidos getDadosExtraidos() { return dadosExtraidos; }
    public List<ValidacaoCampo> getValidacoes() { return validacoes; }
}
