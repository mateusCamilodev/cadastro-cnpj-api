package com.mateus.cadastro_cnpj_api.qa.modelo;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

// Uma execução do QA Center: quem iniciou, em qual ambiente, com qual seleção e o placar.
// A seleção (endpoints, categorias e cenários) fica em JSON para permitir "reexecutar".
@Entity
@Table(name = "qa_execucoes", indexes = {
        @Index(name = "ix_qa_exec_status", columnList = "status"),
        @Index(name = "ix_qa_exec_criada", columnList = "criadaEm"),
        @Index(name = "ix_qa_exec_usuario", columnList = "usuario")
})
public class QaExecucao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Ambiente ambiente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusExecucao status = StatusExecucao.PENDING;

    // Seleção usada (JSON) e sua assinatura (para barrar execução duplicada em andamento)
    @Lob
    @Column(nullable = false)
    private String configuracao;

    @Column(nullable = false, length = 64)
    private String assinatura;

    // Execução de origem quando foi criada por "reexecutar"
    private UUID origemId;

    private int total;
    private int aprovados;
    private int reprovados;
    private int erros;
    private int pulados;
    private int cancelados;

    // Suíte usada (quando a execução foi criada a partir de uma Test Suite)
    private UUID suiteId;

    @Column(length = 120)
    private String suiteNome;

    @Column(nullable = false)
    private LocalDateTime criadaEm = LocalDateTime.now();
    private LocalDateTime iniciadaEm;
    private LocalDateTime finalizadaEm;
    private Long duracaoMs;

    @Column(length = 500)
    private String mensagem;

    @Column(length = 150)
    private String canceladaPor;

    protected QaExecucao() {
    }

    public QaExecucao(String usuario, Ambiente ambiente, String configuracao, String assinatura, int total, UUID origemId,
                      UUID suiteId, String suiteNome) {
        this.suiteId = suiteId;
        this.suiteNome = suiteNome;
        this.usuario = usuario;
        this.ambiente = ambiente;
        this.configuracao = configuracao;
        this.assinatura = assinatura;
        this.total = total;
        this.origemId = origemId;
    }

    public void atualizarPlacar(int aprovados, int reprovados, int erros, int pulados, int cancelados) {
        this.cancelados = cancelados;
        this.aprovados = aprovados;
        this.reprovados = reprovados;
        this.erros = erros;
        this.pulados = pulados;
    }

    public void iniciar() {
        this.status = StatusExecucao.RUNNING;
        this.iniciadaEm = LocalDateTime.now();
    }

    public void finalizar(StatusExecucao status, String mensagem) {
        this.status = status;
        this.mensagem = mensagem;
        this.finalizadaEm = LocalDateTime.now();
        LocalDateTime inicio = iniciadaEm != null ? iniciadaEm : criadaEm;
        this.duracaoMs = java.time.Duration.between(inicio, finalizadaEm).toMillis();
    }

    public void marcarCancelamento(String usuario) {
        this.canceladaPor = usuario;
    }

    public UUID getId() { return id; }
    public String getUsuario() { return usuario; }
    public Ambiente getAmbiente() { return ambiente; }
    public StatusExecucao getStatus() { return status; }
    public String getConfiguracao() { return configuracao; }
    public String getAssinatura() { return assinatura; }
    public UUID getOrigemId() { return origemId; }
    public int getTotal() { return total; }
    public int getAprovados() { return aprovados; }
    public int getReprovados() { return reprovados; }
    public int getErros() { return erros; }
    public int getPulados() { return pulados; }
    public int getCancelados() { return cancelados; }
    public UUID getSuiteId() { return suiteId; }
    public String getSuiteNome() { return suiteNome; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public LocalDateTime getIniciadaEm() { return iniciadaEm; }
    public LocalDateTime getFinalizadaEm() { return finalizadaEm; }
    public Long getDuracaoMs() { return duracaoMs; }
    public String getMensagem() { return mensagem; }
    public String getCanceladaPor() { return canceladaPor; }
}
