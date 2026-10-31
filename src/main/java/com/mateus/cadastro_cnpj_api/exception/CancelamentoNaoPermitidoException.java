package com.mateus.cadastro_cnpj_api.exception;

import com.mateus.cadastro_cnpj_api.model.StatusSolicitacao;

public class CancelamentoNaoPermitidoException extends RuntimeException {

    public CancelamentoNaoPermitidoException(StatusSolicitacao status) {
        super("Não é possível cancelar uma solicitação com status " + status);
    }
}