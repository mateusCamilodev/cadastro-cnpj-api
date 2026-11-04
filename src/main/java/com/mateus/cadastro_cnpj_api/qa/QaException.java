package com.mateus.cadastro_cnpj_api.qa;

import org.springframework.http.HttpStatus;

// Erro de negócio do QA Center com o status HTTP certo (400, 404, 409, 429, 503).
// Tratado no GlobalExceptionHandler, no mesmo formato JSON dos outros erros.
public class QaException extends RuntimeException {

    private final HttpStatus status;

    public QaException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
