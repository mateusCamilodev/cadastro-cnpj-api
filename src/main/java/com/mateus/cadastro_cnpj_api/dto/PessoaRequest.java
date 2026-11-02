package com.mateus.cadastro_cnpj_api.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record PessoaRequest(

        @NotBlank(message = "nome é obrigatório")
        @Size(max = 150, message = "nome deve ter no máximo 150 caracteres")
        String nome,

        @NotBlank(message = "cpf é obrigatório")
        @Pattern(regexp = "\\d{11}", message = "cpf deve conter 11 dígitos numéricos")
        String cpf,

        @NotBlank(message = "email é obrigatório")
        @Email(message = "email inválido")
        String email,

        @NotBlank(message = "telefone é obrigatório")
        @Pattern(regexp = "\\d{10,11}", message = "telefone deve conter 10 ou 11 dígitos numéricos")
        String telefone,

        @NotNull(message = "dataNascimento é obrigatória")
        @Past(message = "dataNascimento deve ser uma data no passado")
        LocalDate dataNascimento
) {}