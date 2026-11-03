package com.mateus.cadastro_cnpj_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SolicitacaoRequest(

        @NotBlank(message = "solicitação é obrigatória")
        @Size(max = 50, message = "solicitação deve ter no máximo 50 caracteres")
        String solicitacao,

        // Nomes exatos dos PDFs que serão enviados depois em POST /solicitacoes/{id}/documento
        @NotEmpty(message = "arquivos deve ter pelo menos 1 arquivo")
        @Size(max = 10, message = "arquivos deve ter no máximo 10 arquivos")
        List<
                @NotBlank(message = "nome do arquivo é obrigatório")
                @Size(max = 255, message = "nome do arquivo deve ter no máximo 255 caracteres")
                @Pattern(regexp = "^[^\\\\/:*?\"<>|]+\\.[pP][dD][fF]$",
                        message = "deve ser um nome de arquivo .pdf, sem caminho (ex.: documento-solicitacao-0001.pdf)")
                String> arquivos,

        @NotNull(message = "pessoa é obrigatória")
        @Valid
        PessoaRequest pessoa,

        @NotNull(message = "empresa é obrigatória")
        @Valid
        EmpresaRequest empresa
) {}