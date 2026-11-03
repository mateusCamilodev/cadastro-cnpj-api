package com.mateus.cadastro_cnpj_api.dto;

import com.mateus.cadastro_cnpj_api.model.Solicitacao;
import com.mateus.cadastro_cnpj_api.model.StatusSolicitacao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// Um item do GET /solicitacoes (listagem). Sem dados pessoais (CPF, CNPJ, e-mail)
public record ResumoSolicitacaoResponse(
        UUID id,
        String solicitacao,
        StatusSolicitacao status,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        LocalDateTime prazoDocumento,
        int documentosEsperados,
        long documentosRecebidos,
        List<ArquivoResumo> arquivos
) {
    public record ArquivoResumo(String nomeArquivo, boolean recebido) {
    }

    public static ResumoSolicitacaoResponse de(Solicitacao s) {
        List<ArquivoResumo> arquivos = s.getDocumentos().stream()
                .map(d -> new ArquivoResumo(d.getNomeArquivo(), d.isRecebido()))
                .toList();
        return new ResumoSolicitacaoResponse(
                s.getId(),
                s.getSolicitacao(),
                s.getStatus(),
                s.getDataCriacao(),
                s.getDataAtualizacao(),
                s.getPrazoDocumento(),
                arquivos.size(),
                s.getTotalRecebidos(),
                arquivos
        );
    }
}
