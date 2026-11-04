package com.mateus.cadastro_cnpj_api.qa.execucao;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

// Resposta crua de uma chamada (status, headers e corpo) com leitura do JSON sob demanda
public record RespostaQa(int status, Map<String, String> headers, String corpo, long duracaoMs, ObjectMapper mapper) {

    public JsonNode json() {
        try {
            return corpo == null || corpo.isBlank() ? mapper.missingNode() : mapper.readTree(corpo);
        } catch (RuntimeException e) {
            return mapper.missingNode();
        }
    }

    public String header(String nome) {
        return headers.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(nome))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }

    public String mensagem() {
        return json().path("mensagem").asString("");
    }
}
