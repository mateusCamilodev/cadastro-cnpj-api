package com.mateus.cadastro_cnpj_api.qa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// Configurações do QA Center (application.properties + application-local.properties).
// As URLs dos ambientes NÃO ficam aqui: são lidas na hora (AmbientesQa), para aceitar ${local.server.port} nos testes.
@ConfigurationProperties(prefix = "qa")
public record QaProperties(Limites limites, ContaTeste contaTeste) {

    public record Limites(Integer simultaneas, Integer porUsuario, Duration timeoutRequisicao, Duration timeoutExecucao) {
        public int simultaneasOuPadrao() { return simultaneas == null ? 2 : simultaneas; }
        public int porUsuarioOuPadrao() { return porUsuario == null ? 1 : porUsuario; }
        public Duration timeoutRequisicaoOuPadrao() { return timeoutRequisicao == null ? Duration.ofSeconds(10) : timeoutRequisicao; }
        public Duration timeoutExecucaoOuPadrao() { return timeoutExecucao == null ? Duration.ofMinutes(10) : timeoutExecucao; }
    }

    // Conta usada pelo executor para chamar a API testada (nunca é exposta ao navegador)
    public record ContaTeste(String email, String senha) {
    }

    public Limites limitesOuPadrao() {
        return limites == null ? new Limites(null, null, null, null) : limites;
    }
}
