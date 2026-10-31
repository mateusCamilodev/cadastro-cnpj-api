package com.mateus.cadastro_cnpj_api.exception;

// 400 - Arquivo vazio, que não é PDF, corrompido ou sem texto legível
public class ArquivoInvalidoException extends RuntimeException {

    public ArquivoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
