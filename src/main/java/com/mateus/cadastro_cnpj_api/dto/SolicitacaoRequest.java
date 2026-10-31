package com.mateus.cadastro_cnpj_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SolicitacaoRequest(

        @NotNull(message = "ipr é obrigatório")
        @Size(max = 50, message = "ipr deve ter no máximo 50 caracteres")
        String ipr,

        // Nomes exatos dos PDFs que serão enviados depois em POST /solicitacoes/{id}/documento
        @NotEmpty(message = "arquivos deve ter pelo menos 1 arquivo")
        @Size(max = 10, message = "arquivos deve ter no máximo 10 arquivos")
        List<
                @Size(max = 255, message = "nome do arquivo deve ter no máximo 255 caracteres")
                @Pattern(regexp = "^[^\\\\/:*?\"<>|]+\\.[pP][dD][fF]$",
                        message = "deve ser um nome de arquivo .pdf, sem caminho (ex.: documento-ipr-0001.pdf)")
                String> arquivos,

        @Valid
        PessoaRequest pessoa,

        @NotNull(message = "empresa é obrigatória")
        @Valid
        EmpresaRequest empresa
) {}
