package com.mateus.cadastro_cnpj_api.service;

import com.mateus.cadastro_cnpj_api.model.DadosExtraidos;
import com.mateus.cadastro_cnpj_api.model.Solicitacao;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

// Compara, campo a campo, o que foi extraído do PDF com os dados gravados no POST
// Devolve APROVADO/REPROVADO para cada campo. Nunca devolve os valores (LGPD).
@Component
public class ComparadorDocumento {

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public List<ResultadoCampo> comparar(Solicitacao s, DadosExtraidos documento) {
        // PDF com texto mas sem nenhuma linha "Campo: valor" → todos os campos ausentes
        DadosExtraidos d = documento != null ? documento : DadosExtraidos.de(Map.of());

        return List.of(
                verificar("ipr", s.getIpr(), d.getIpr(), ComparadorDocumento::texto),

                verificar("nome", s.getNome(), d.getNome(), ComparadorDocumento::texto),
                verificar("cpf", s.getCpf(), d.getCpf(), ComparadorDocumento::digitos),
                verificar("email", s.getEmail(), d.getEmail(), ComparadorDocumento::texto),
                verificar("telefone", s.getTelefone(), d.getTelefone(), ComparadorDocumento::digitos),
                verificar("dataNascimento", s.getDataNascimento().toString(), d.getDataNascimento(),
                        ComparadorDocumento::data),

                verificar("cnpj", s.getCnpj(), d.getCnpj(), ComparadorDocumento::digitos),
                verificar("razaoSocial", s.getRazaoSocial(), d.getRazaoSocial(), ComparadorDocumento::texto),
                verificarNomeFantasia(s.getNomeFantasia(), d.getNomeFantasia())
                // dataAbertura não é comparada (o PDF não precisa trazer)
        );
    }

    private ResultadoCampo verificar(String campo, String esperado, String recebido,
                                     UnaryOperator<String> normalizar) {
        if (recebido == null || recebido.isBlank()) {
            return ResultadoCampo.reprovado(campo, "campo ausente no documento");
        }
        if (!normalizar.apply(esperado).equals(normalizar.apply(recebido))) {
            return ResultadoCampo.reprovado(campo, "não confere com a solicitação");
        }
        return ResultadoCampo.aprovado(campo);
    }

    // Opcional: se não veio no POST, o PDF também não pode trazer um valor
    private ResultadoCampo verificarNomeFantasia(String esperado, String recebido) {
        boolean esperadoVazio = esperado == null || esperado.isBlank();
        boolean recebidoVazio = recebido == null || recebido.isBlank();

        if (esperadoVazio && recebidoVazio) {
            return ResultadoCampo.aprovado("nomeFantasia");
        }
        if (esperadoVazio) {
            return ResultadoCampo.reprovado("nomeFantasia", "não confere com a solicitação");
        }
        return verificar("nomeFantasia", esperado, recebido, ComparadorDocumento::texto);
    }

    // ===== Normalizadores: deixam os dois lados no mesmo formato antes de comparar =====

    // Ignora maiúsculas/minúsculas e espaços extras
    private static String texto(String valor) {
        return valor.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    // Aceita com ou sem máscara: "123.456.789-09" == "12345678909"
    private static String digitos(String valor) {
        return valor.replaceAll("\\D", "");
    }

    // Aceita "15/01/2000" ou "2000-01-15"
    private static String data(String valor) {
        String v = valor.trim();
        for (DateTimeFormatter formato : List.of(DATA_BR, DateTimeFormatter.ISO_LOCAL_DATE)) {
            try {
                return LocalDate.parse(v, formato).toString();
            } catch (DateTimeParseException ignorado) {
                // tenta o próximo formato
            }
        }
        return v; // não é uma data válida: vai divergir
    }
}
