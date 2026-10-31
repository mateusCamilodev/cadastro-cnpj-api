package com.mateus.cadastro_cnpj_api.exception;

public class SolicitacaoDuplicadaException extends RuntimeException {

    public SolicitacaoDuplicadaException() {
        super("Já existe uma solicitação ativa para este CPF e CNPJ");
    }
}