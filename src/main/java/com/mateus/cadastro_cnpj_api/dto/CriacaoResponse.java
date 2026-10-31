package com.mateus.cadastro_cnpj_api.dto;

import java.util.UUID;

// Resposta do POST /solicitacoes: só o id do processamento (status é consultado no GET)
public record CriacaoResponse(UUID id) {
}
