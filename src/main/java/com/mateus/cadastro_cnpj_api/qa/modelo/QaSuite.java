package com.mateus.cadastro_cnpj_api.qa.modelo;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

// Test Suite: coleção reutilizável de cenários. As suítes "padrão" vêm do código e não podem ser alteradas;
// as demais são criadas por quem tem a permissão qa.configure.
@Entity
@Table(name = "qa_suites", uniqueConstraints = @UniqueConstraint(name = "uk_qa_suite_nome", columnNames = "nome"))
public class QaSuite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 500)
    private String descricao;

    // Ids dos cenários, em JSON (ordem preservada)
    @Lob
    @Column(nullable = false)
    private String cenarioIds;

    @Column(nullable = false)
    private boolean padrao;

    @Column(nullable = false, length = 150)
    private String criadaPor;

    @Column(nullable = false)
    private LocalDateTime criadaEm = LocalDateTime.now();

    @Column(length = 150)
    private String atualizadaPor;

    private LocalDateTime atualizadaEm;

    protected QaSuite() {
    }

    public QaSuite(String nome, String descricao, String cenarioIds, boolean padrao, String criadaPor) {
        this.nome = nome;
        this.descricao = descricao;
        this.cenarioIds = cenarioIds;
        this.padrao = padrao;
        this.criadaPor = criadaPor;
    }

    public void alterar(String nome, String descricao, String cenarioIds, String usuario) {
        this.nome = nome;
        this.descricao = descricao;
        this.cenarioIds = cenarioIds;
        this.atualizadaPor = usuario;
        this.atualizadaEm = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getCenarioIds() { return cenarioIds; }
    public boolean isPadrao() { return padrao; }
    public String getCriadaPor() { return criadaPor; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public String getAtualizadaPor() { return atualizadaPor; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
}
