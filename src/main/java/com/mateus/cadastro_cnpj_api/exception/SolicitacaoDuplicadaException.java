package com.mateus.cadastro_cnpj_api.exception;

// 409 - IPR ou CNPJ já em uso numa solicitação ativa (a mensagem diz qual, sem mostrar o valor)
public class SolicitacaoDuplicadaException extends RuntimeException {

    public SolicitacaoDuplicadaException(String mensagem) {
        super(mensagem);
    }
}
