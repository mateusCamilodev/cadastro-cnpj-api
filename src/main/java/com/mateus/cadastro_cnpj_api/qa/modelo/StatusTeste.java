package com.mateus.cadastro_cnpj_api.qa.modelo;

// Estado de cada cenário dentro de uma execução
public enum StatusTeste {
    PENDING,    // ainda não começou
    RUNNING,    // em execução
    PASSED,     // todas as validações passaram
    FAILED,     // alguma validação não bateu com o esperado
    SKIPPED,    // bloqueado: uma pré-condição falhou (ex.: não conseguiu criar a solicitação de teste)
    ERROR,      // erro inesperado (ex.: API fora do ar, tempo esgotado)
    CANCELLED;  // não rodou porque a execução foi cancelada

    public boolean terminado() {
        return this != PENDING && this != RUNNING;
    }
}
