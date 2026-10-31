package com.mateus.cadastro_cnpj_api.exception;

public class SolicitacaoDuplicadaException extends RuntimeException {

    public SolicitacaoDuplicadaException(String cpf, String cnpj) {
        super("Já existe uma solicitação ativa para o CPF " + cpf + " e CNPJ " + cnpj);
    }
}
