package com.mateus.cadastro_cnpj_api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

public record ErroResponse(
        LocalDateTime timestamp,
        int status,
        String mensagem,
        // Só aparece no JSON quando tem itens (ex.: campos inválidos no 400)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<CampoErro> detalhes
) {
    public record CampoErro(String campo, String mensagem) {
    }
}
