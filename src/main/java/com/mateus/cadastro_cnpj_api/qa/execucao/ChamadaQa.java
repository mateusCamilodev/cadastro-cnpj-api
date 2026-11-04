package com.mateus.cadastro_cnpj_api.qa.execucao;

import java.util.Map;

// Evidência de uma chamada HTTP feita por um cenário (já mascarada).
// "principal" = a chamada que está sendo testada; as demais são preparação (ex.: criar a solicitação antes).
public record ChamadaQa(
        String rotulo,
        boolean principal,
        String metodo,
        String caminho,
        Map<String, String> headers,
        String corpoEnviado,
        Integer status,
        Long duracaoMs,
        String corpoRecebido
) {
}
