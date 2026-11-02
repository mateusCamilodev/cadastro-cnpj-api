package com.mateus.cadastro_cnpj_api.model;

import java.util.List;

public enum StatusSolicitacao {
    RECEBIDO,
    AGUARDANDO_DOCUMENTO,
    EM_PROCESSAMENTO,
    APROVADO,
    REPROVADO,
    CANCELADO,
    EXPIRADO;

    // Status que não contam como "ativa": não bloqueiam CPF+CNPJ e liberam o ipr para ser criado de novo
    public static final List<StatusSolicitacao> INATIVOS = List.of(CANCELADO, EXPIRADO, REPROVADO);

    // Status em que o PATCH de cancelamento é permitido
    public static final List<StatusSolicitacao> CANCELAVEIS = List.of(RECEBIDO, AGUARDANDO_DOCUMENTO, EM_PROCESSAMENTO);
}
