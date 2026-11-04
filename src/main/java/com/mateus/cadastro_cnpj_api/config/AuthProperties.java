package com.mateus.cadastro_cnpj_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// Lê do application(-local).properties a lista auth.usuarios[0].email, auth.usuarios[0].senha,
// auth.usuarios[0].permissoes (opcional, separadas por vírgula, ex.: qa.view,qa.execute) ...
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(List<Usuario> usuarios) {

    public record Usuario(String email, String senha, List<String> permissoes) {

        // Sem a propriedade "permissoes" no arquivo → lista vazia (usuário comum)
        public List<String> permissoes() {
            return permissoes == null ? List.of() : permissoes;
        }
    }
}
