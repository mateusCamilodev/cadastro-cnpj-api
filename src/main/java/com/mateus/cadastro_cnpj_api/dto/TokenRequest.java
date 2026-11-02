package com.mateus.cadastro_cnpj_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Body do POST /auth/token
public record TokenRequest(

        @NotBlank(message = "email é obrigatório")
        @Email(message = "email inválido")
        String email,

        @NotBlank(message = "senha é obrigatória")
        String senha
) {}
