package com.mateus.cadastro_cnpj_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Map;

// Campos lidos do PDF aceito, do jeito que vieram no arquivo (com máscara, data dd/MM/yyyy etc.)
// @Embeddable: não é uma tabela própria, as colunas ficam dentro de "documentos_solicitacao"
@Embeddable
public class DadosExtraidos {

    @Column(name = "pdf_ipr")
    private String ipr;

    @Column(name = "pdf_nome")
    private String nome;

    @Column(name = "pdf_cpf")
    private String cpf;

    @Column(name = "pdf_email")
    private String email;

    @Column(name = "pdf_telefone")
    private String telefone;

    @Column(name = "pdf_data_nascimento")
    private String dataNascimento;

    @Column(name = "pdf_cnpj")
    private String cnpj;

    @Column(name = "pdf_razao_social")
    private String razaoSocial;

    @Column(name = "pdf_nome_fantasia")
    private String nomeFantasia;

    @Column(name = "pdf_data_abertura")
    private String dataAbertura;

    protected DadosExtraidos() {
    }

    // Recebe o Map do LeitorDocumentoPdf (chaves já normalizadas: "razaosocial", "datadenascimento"...)
    public static DadosExtraidos de(Map<String, String> campos) {
        DadosExtraidos d = new DadosExtraidos();
        d.ipr = campos.get("ipr");
        d.nome = campos.get("nome");
        d.cpf = campos.get("cpf");
        d.email = campos.get("email");
        d.telefone = campos.get("telefone");
        d.dataNascimento = campos.get("datadenascimento");
        d.cnpj = campos.get("cnpj");
        d.razaoSocial = campos.get("razaosocial");
        d.nomeFantasia = campos.get("nomefantasia");
        d.dataAbertura = campos.get("datadeabertura");
        return d;
    }

    // ===== Getters =====
    public String getIpr() { return ipr; }
    public String getNome() { return nome; }
    public String getCpf() { return cpf; }
    public String getEmail() { return email; }
    public String getTelefone() { return telefone; }
    public String getDataNascimento() { return dataNascimento; }
    public String getCnpj() { return cnpj; }
    public String getRazaoSocial() { return razaoSocial; }
    public String getNomeFantasia() { return nomeFantasia; }
    public String getDataAbertura() { return dataAbertura; }
}
