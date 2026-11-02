package com.mateus.cadastro_cnpj_api.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "solicitacoes")
public class Solicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Não é unique: um ipr cancelado pode ser criado de novo (novo registro, o cancelado fica como histórico)
    @Column(nullable = false, length = 50)
    private String ipr;

    // ===== Dados da pessoa =====
    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 11)
    private String cpf;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 11)
    private String telefone;

    @Column(nullable = false)
    private LocalDate dataNascimento;

    // ===== Dados da empresa =====
    @Column(nullable = false, length = 14)
    private String cnpj;

    @Column(nullable = false, length = 200)
    private String razaoSocial;

    @Column(length = 200)
    private String nomeFantasia;

    // Opcional (pode vir null no POST)
    private LocalDate dataAbertura;

    // ===== Controle do processamento =====
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSolicitacao status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    private LocalDateTime dataAtualizacao;

    // Até quando os PDFs podem ser enviados (preenchido ao entrar em AGUARDANDO_DOCUMENTO)
    private LocalDateTime prazoDocumento;

    // ===== Filhos (tabelas próprias). cascade = ALL: salvar a solicitação já salva os filhos =====
    @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DocumentoSolicitacao> documentos = new ArrayList<>();

    @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id DESC")
    private List<EventoHistorico> historico = new ArrayList<>();

    @PrePersist
    void aoCriar() {
        this.dataCriacao = LocalDateTime.now();
        this.dataAtualizacao = this.dataCriacao;
        if (this.status == null) {
            this.status = StatusSolicitacao.RECEBIDO;
        }
    }

    @PreUpdate
    void aoAtualizar() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    public Solicitacao() {
    }

    // ===== Documentos e histórico =====
    public void adicionarDocumento(String nomeArquivo) {
        documentos.add(new DocumentoSolicitacao(this, nomeArquivo));
    }

    // Comparação exata do nome (diferencia maiúsculas/minúsculas)
    public Optional<DocumentoSolicitacao> buscarDocumento(String nomeArquivo) {
        return documentos.stream()
                .filter(d -> d.getNomeArquivo().equalsIgnoreCase(nomeArquivo))
                .findFirst();
    }

    public long getTotalRecebidos() {
        return documentos.stream().filter(DocumentoSolicitacao::isRecebido).count();
    }

    // Registra o evento com o status atual: mude o status ANTES de chamar, se for o caso
    public void registrarEvento(String descricao) {
        historico.add(new EventoHistorico(this, status, descricao));
    }

    // ===== Getters e Setters =====
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getIpr() { return ipr; }
    public void setIpr(String ipr) { this.ipr = ipr; }


    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }

    public String getNomeFantasia() { return nomeFantasia; }
    public void setNomeFantasia(String nomeFantasia) { this.nomeFantasia = nomeFantasia; }

    public LocalDate getDataAbertura() { return dataAbertura; }
    public void setDataAbertura(LocalDate dataAbertura) { this.dataAbertura = dataAbertura; }

    public StatusSolicitacao getStatus() { return status; }
    public void setStatus(StatusSolicitacao status) { this.status = status; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }

    public LocalDateTime getPrazoDocumento() { return prazoDocumento; }
    public void setPrazoDocumento(LocalDateTime prazoDocumento) { this.prazoDocumento = prazoDocumento; }

    public List<DocumentoSolicitacao> getDocumentos() { return documentos; }

    public List<EventoHistorico> getHistorico() { return historico; }
}