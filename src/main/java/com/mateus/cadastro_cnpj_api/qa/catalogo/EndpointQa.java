package com.mateus.cadastro_cnpj_api.qa.catalogo;

import com.mateus.cadastro_cnpj_api.qa.modelo.Ambiente;

import java.util.Set;

// Um endpoint da API do Carimbô que o QA Center pode testar.
// O caminho é um modelo FIXO (ex.: /solicitacoes/{id}): o executor só chama ambiente autorizado + este caminho.
public record EndpointQa(
        String id,
        String nome,
        String metodo,
        String caminho,
        String grupo,
        String descricao,
        boolean modificaDados,
        Set<Ambiente> ambientesPermitidos
) {
}
