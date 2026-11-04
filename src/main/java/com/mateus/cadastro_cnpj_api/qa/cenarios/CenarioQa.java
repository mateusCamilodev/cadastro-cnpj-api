package com.mateus.cadastro_cnpj_api.qa.cenarios;

import com.mateus.cadastro_cnpj_api.qa.execucao.ContextoQa;
import com.mateus.cadastro_cnpj_api.qa.modelo.Categoria;

// Um cenário automatizado do catálogo. A "logica" faz as chamadas e registra as validações no contexto.
public record CenarioQa(
        String id,
        String nome,
        String descricao,
        String endpointId,
        Categoria categoria,
        String criticidade,      // ALTA, MEDIA ou BAIXA
        int timeoutSegundos,
        String preCondicoes,
        String esperado,
        Logica logica
) {
    @FunctionalInterface
    public interface Logica {
        void executar(ContextoQa ctx, String cenarioId);
    }
}
