package com.mateus.cadastro_cnpj_api.qa.modelo;

import java.util.Set;

// Ciclo de vida de uma execução
public enum StatusExecucao {
    PENDING,     // criada, aguardando o executor
    RUNNING,     // cenários sendo executados
    COMPLETED,   // todos os cenários terminaram (passaram ou não)
    CANCELLED,   // cancelada pelo usuário; o que não rodou fica CANCELLED
    ERROR;       // falha da própria execução (ex.: tempo máximo excedido)

    public static final Set<StatusExecucao> ATIVOS = Set.of(PENDING, RUNNING);

    public boolean ativo() {
        return ATIVOS.contains(this);
    }
}
