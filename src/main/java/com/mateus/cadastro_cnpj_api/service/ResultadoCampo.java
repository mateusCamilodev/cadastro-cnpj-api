package com.mateus.cadastro_cnpj_api.service;

import com.mateus.cadastro_cnpj_api.model.ResultadoValidacao;

// Resultado da comparação de um campo (devolvido pelo ComparadorDocumento)
public record ResultadoCampo(String campo, ResultadoValidacao resultado, String motivo) {

    public static ResultadoCampo aprovado(String campo) {
        return new ResultadoCampo(campo, ResultadoValidacao.APROVADO, null);
    }

    public static ResultadoCampo reprovado(String campo, String motivo) {
        return new ResultadoCampo(campo, ResultadoValidacao.REPROVADO, motivo);
    }

    public boolean reprovado() {
        return resultado == ResultadoValidacao.REPROVADO;
    }
}
