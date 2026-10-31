package com.mateus.cadastro_cnpj_api.exception;

// 400 - Lista "arquivos" do POST com o mesmo nome mais de uma vez
public class ArquivosRepetidosException extends RuntimeException {

    public ArquivosRepetidosException() {
        super("A lista de arquivos não pode ter nomes repetidos");
    }
}
