package com.mateus.cadastro_cnpj_api.dto;

import com.mateus.cadastro_cnpj_api.model.Solicitacao;
import com.mateus.cadastro_cnpj_api.model.StatusSolicitacao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// Resposta do GET / PATCH / upload: status atual + histórico do processamento em ordem cronológica
public record SolicitacaoResponse(
        UUID id,
        StatusSolicitacao status,
        List<EventoResponse> historico
) {
    public record EventoResponse(LocalDateTime dataHora, StatusSolicitacao status, String descricao) {
    }

    public static SolicitacaoResponse de(Solicitacao s) {
        List<EventoResponse> historico = s.getHistorico().stream()
                .map(e -> new EventoResponse(e.getDataHora(), e.getStatus(), e.getDescricao()))
                .toList();

        return new SolicitacaoResponse(s.getId(), s.getStatus(), historico);
    }
}
