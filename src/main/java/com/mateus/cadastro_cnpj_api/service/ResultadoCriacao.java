package com.mateus.cadastro_cnpj_api.service;

import com.mateus.cadastro_cnpj_api.model.Solicitacao;

public record ResultadoCriacao(Solicitacao solicitacao, boolean nova) {
}