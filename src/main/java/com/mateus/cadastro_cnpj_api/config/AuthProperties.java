package com.mateus.cadastro_cnpj_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// Lê do application.properties a lista auth.usuarios[0].email, auth.usuarios[0].senha, ...
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(List<Usuario> usuarios) {

    public record Usuario(String email, String senha) {
    }
}
