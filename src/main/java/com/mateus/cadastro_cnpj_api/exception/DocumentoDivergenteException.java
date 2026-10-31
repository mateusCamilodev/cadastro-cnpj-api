package com.mateus.cadastro_cnpj_api.exception;

import java.util.List;

// 422 - PDF lido, mas os dados não batem com os do POST
public class DocumentoDivergenteException extends RuntimeException {

    private final List<ErroResponse.CampoErro> divergencias;

    public DocumentoDivergenteException(List<ErroResponse.CampoErro> divergencias) {
        super("Os dados do documento não conferem com os da solicitação");
        this.divergencias = divergencias;
    }

    public List<ErroResponse.CampoErro> getDivergencias() {
        return divergencias;
    }
}
