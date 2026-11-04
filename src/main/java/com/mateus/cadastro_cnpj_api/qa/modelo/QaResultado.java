package com.mateus.cadastro_cnpj_api.qa.modelo;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

// Resultado de um cenário dentro de uma execução. As evidências (chamadas HTTP e validações)
// ficam em JSON já MASCARADAS e truncadas — nada de token, senha ou documento em texto puro.
@Entity
@Table(name = "qa_resultados", indexes = {
        @Index(name = "ix_qa_res_execucao", columnList = "execucao_id"),
        @Index(name = "ix_qa_res_status", columnList = "status"),
        @Index(name = "ix_qa_res_endpoint", columnList = "endpointId")
})
public class QaResultado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execucao_id", nullable = false)
    private UUID execucaoId;

    @Column(nullable = false, length = 20)
    private String cenarioId;

    @Column(nullable = false, length = 150)
    private String cenarioNome;

    @Column(nullable = false, length = 40)
    private String endpointId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusTeste status = StatusTeste.PENDING;

    private Integer httpStatus;
    private Long duracaoMs;

    @Column(length = 1000)
    private String mensagem;

    @Lob
    private String evidencias;

    private LocalDateTime iniciadoEm;
    private LocalDateTime finalizadoEm;

    protected QaResultado() {
    }

    public QaResultado(UUID execucaoId, String cenarioId, String cenarioNome, String endpointId, Categoria categoria) {
        this.execucaoId = execucaoId;
        this.cenarioId = cenarioId;
        this.cenarioNome = cenarioNome;
        this.endpointId = endpointId;
        this.categoria = categoria;
    }

    public void iniciar() {
        this.status = StatusTeste.RUNNING;
        this.iniciadoEm = LocalDateTime.now();
    }

    public void concluir(StatusTeste status, Integer httpStatus, Long duracaoMs, String mensagem, String evidencias) {
        this.status = status;
        this.httpStatus = httpStatus;
        this.duracaoMs = duracaoMs;
        this.mensagem = mensagem != null && mensagem.length() > 1000 ? mensagem.substring(0, 997) + "..." : mensagem;
        this.evidencias = evidencias;
        this.finalizadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public UUID getExecucaoId() { return execucaoId; }
    public String getCenarioId() { return cenarioId; }
    public String getCenarioNome() { return cenarioNome; }
    public String getEndpointId() { return endpointId; }
    public Categoria getCategoria() { return categoria; }
    public StatusTeste getStatus() { return status; }
    public Integer getHttpStatus() { return httpStatus; }
    public Long getDuracaoMs() { return duracaoMs; }
    public String getMensagem() { return mensagem; }
    public String getEvidencias() { return evidencias; }
    public LocalDateTime getIniciadoEm() { return iniciadoEm; }
    public LocalDateTime getFinalizadoEm() { return finalizadoEm; }
}
