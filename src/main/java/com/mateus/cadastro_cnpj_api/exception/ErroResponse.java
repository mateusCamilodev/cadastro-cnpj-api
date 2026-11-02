package com.mateus.cadastro_cnpj_api.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErroResponse(
        LocalDateTime timestamp,
        int status,
        String mensagem,
        List<CampoErro> detalhes
) {
    public record CampoErro(String campo, String mensagem) {
    }
}
