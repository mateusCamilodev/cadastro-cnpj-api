package com.mateus.cadastro_cnpj_api.exception;

// 401 - E-mail ou senha errados no POST /auth/token
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException(String mensagem) {
        super(mensagem);
    }
}
