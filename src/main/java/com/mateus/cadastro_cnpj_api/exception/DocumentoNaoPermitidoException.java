package com.mateus.cadastro_cnpj_api.exception;

// 409 - Solicitação não está aguardando documento (ou o prazo já passou)
public class DocumentoNaoPermitidoException extends RuntimeException {

    public DocumentoNaoPermitidoException(String mensagem) {
        super(mensagem);
    }
}
