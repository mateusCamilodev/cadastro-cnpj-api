package com.mateus.cadastro_cnpj_api.dto;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CNPJ;

import java.time.LocalDate;

public record EmpresaRequest(

        @NotBlank(message = "cnpj é obrigatório")
        @Pattern(regexp = "\\d{14}", message = "cnpj deve conter 14 dígitos numéricos")
        @CNPJ(message = "cnpj inválido")
        String cnpj,

        @NotBlank(message = "razaoSocial é obrigatória")
        @Size(max = 200, message = "razaoSocial deve ter no máximo 200 caracteres")
        String razaoSocial,

        @Size(max = 200, message = "nomeFantasia deve ter no máximo 200 caracteres")
        String nomeFantasia,

        @NotNull(message = "dataAbertura é obrigatória")
        @PastOrPresent(message = "dataAbertura não pode ser futura")
        LocalDate dataAbertura
) {}