package com.mateus.cadastro_cnpj_api.qa.execucao;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

// Dados de teste isolados e controlados: tudo é gerado na hora, com marcação "QA-".
// Nunca usa documento ou dado real de usuário. Os PDFs são criados em memória com PDFBox.
public final class DadosDeTeste {

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private DadosDeTeste() {
    }

    // Uma solicitação de teste completa e coerente (o PDF gerado a partir dela é aprovado na análise)
    public record Solicitacao(String numero, String nome, String cpf, String email, String telefone,
                              LocalDate dataNascimento, String cnpj, String razaoSocial, String nomeFantasia,
                              List<String> arquivos) {

        // Corpo do POST /solicitacoes
        public Map<String, Object> corpo() {
            Map<String, Object> pessoa = new LinkedHashMap<>();
            pessoa.put("nome", nome);
            pessoa.put("cpf", cpf);
            pessoa.put("email", email);
            pessoa.put("telefone", telefone);
            pessoa.put("dataNascimento", dataNascimento.toString());
            Map<String, Object> empresa = new LinkedHashMap<>();
            empresa.put("cnpj", cnpj);
            empresa.put("razaoSocial", razaoSocial);
            empresa.put("nomeFantasia", nomeFantasia);
            empresa.put("dataAbertura", "2020-03-10");
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("solicitacao", numero);
            corpo.put("arquivos", arquivos);
            corpo.put("pessoa", pessoa);
            corpo.put("empresa", empresa);
            return corpo;
        }

        // Linhas "Campo: valor" no formato que o LeitorDocumentoPdf entende
        public List<String> linhasPdf() {
            return List.of(
                    "Documento da Solicitação de Cadastro (gerado pelo QA Center)",
                    "Solicitação: " + numero,
                    "Nome: " + nome,
                    "CPF: " + cpf,
                    "E-mail: " + email,
                    "Telefone: " + telefone,
                    "Data de Nascimento: " + dataNascimento.format(DATA_BR),
                    "CNPJ: " + cnpj,
                    "Razão Social: " + razaoSocial,
                    "Nome Fantasia: " + nomeFantasia);
        }

        public Solicitacao comArquivos(List<String> novos) {
            return new Solicitacao(numero, nome, cpf, email, telefone, dataNascimento, cnpj, razaoSocial, nomeFantasia, novos);
        }

        public Solicitacao comCnpj(String novoCnpj) {
            return new Solicitacao(numero, nome, cpf, email, telefone, dataNascimento, novoCnpj, razaoSocial, nomeFantasia, arquivos);
        }
    }

    public static Solicitacao nova(String prefixo, List<String> arquivos) {
        String sufixo = digitos(4);
        return new Solicitacao(
                ("QA-" + prefixo + "-" + sufixo).toUpperCase(),
                "Pessoa Teste QA",
                digitos(11),
                "qa." + sufixo + "@exemplo.test",
                "1199" + digitos(7),
                LocalDate.of(1990, 5, 20),
                cnpjUnico(),
                "Empresa Teste QA LTDA",
                "Empresa QA",
                arquivos);
    }

    // CNPJ de 14 dígitos começando com 9 (evita colidir com os exemplos do projeto)
    public static String cnpjUnico() {
        return "9" + digitos(13);
    }

    public static String digitos(int quantidade) {
        StringBuilder sb = new StringBuilder(quantidade);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < quantidade; i++) {
            sb.append(r.nextInt(10));
        }
        return sb.toString();
    }

    // PDF de uma página com as linhas informadas (fonte Helvetica, que aceita acentos do português)
    public static byte[] pdf(List<String> linhas) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage();
            doc.addPage(pagina);
            try (PDPageContentStream cs = new PDPageContentStream(doc, pagina)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.setLeading(18);
                cs.newLineAtOffset(60, 730);
                for (String linha : linhas) {
                    cs.showText(linha);
                    cs.newLine();
                }
                cs.endText();
            }
            doc.save(saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gerar o PDF de teste", e);
        }
    }
}
