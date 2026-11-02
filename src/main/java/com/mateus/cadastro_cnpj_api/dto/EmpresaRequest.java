package com.mateus.cadastro_cnpj_api.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record EmpresaRequest(

        @NotBlank(message = "cnpj é obrigatório")
        @Pattern(regexp = "\\d{14}", message = "cnpj deve conter 14 dígitos numéricos")
        String cnpj,

        @NotBlank(message = "razaoSocial é obrigatória")
        @Size(max = 200, message = "razaoSocial deve ter no máximo 200 caracteres")
        String razaoSocial,

        @Size(max = 200, message = "nomeFantasia deve ter no máximo 200 caracteres")
        String nomeFantasia,

        // Opcional e sem validação de data
        LocalDate dataAbertura
) {}