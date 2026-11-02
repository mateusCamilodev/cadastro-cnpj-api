package com.mateus.cadastro_cnpj_api.dto;

// Resposta do POST /auth/token. expiresIn = segundos até o token expirar
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
