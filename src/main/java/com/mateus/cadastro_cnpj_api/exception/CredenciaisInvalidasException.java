package com.mateus.cadastro_cnpj_api.exception;

// 401 - E-mail ou senha errados no POST /auth/token
// A mensagem não diz qual dos dois errou, para não ajudar quem tenta adivinhar usuários
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("E-mail ou senha inválidos");
    }
}
