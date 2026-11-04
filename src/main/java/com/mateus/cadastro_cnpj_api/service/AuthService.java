package com.mateus.cadastro_cnpj_api.service;

import com.mateus.cadastro_cnpj_api.config.AuthProperties;
import com.mateus.cadastro_cnpj_api.dto.TokenRequest;
import com.mateus.cadastro_cnpj_api.dto.TokenResponse;
import com.mateus.cadastro_cnpj_api.exception.CredenciaisInvalidasException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Confere e-mail + senha contra os usuários fixos (application.properties) e, se baterem, gera o token
@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    // e-mail em minúsculas → hash BCrypt da senha (a senha em texto puro não fica guardada no serviço)
    private final Map<String, String> hashPorEmail = new HashMap<>();

    // e-mail em minúsculas → permissões do usuário (ex.: qa.view); vão para o token
    private final Map<String, List<String>> permissoesPorEmail = new HashMap<>();

    // Hash usado quando o e-mail não existe, só para a conferência levar o mesmo tempo
    private final String hashFalso;

    public AuthService(PasswordEncoder passwordEncoder, TokenService tokenService, AuthProperties properties) {
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        for (AuthProperties.Usuario usuario : properties.usuarios()) {
            String email = usuario.email().trim().toLowerCase();
            hashPorEmail.put(email, passwordEncoder.encode(usuario.senha()));
            permissoesPorEmail.put(email, usuario.permissoes().stream().map(String::trim).filter(p -> !p.isEmpty()).toList());
        }
        this.hashFalso = passwordEncoder.encode("usuario-inexistente");
    }

    public TokenResponse autenticar(TokenRequest request) {
        String email = request.email().trim().toLowerCase();
        String hash = hashPorEmail.get(email);

        // Confere a senha mesmo com e-mail inexistente: assim a resposta demora o mesmo tempo nos dois casos
        boolean senhaConfere = passwordEncoder.matches(request.senha(), hash != null ? hash : hashFalso);

        if (hash == null || !senhaConfere) {
            throw new CredenciaisInvalidasException();
        }
        return tokenService.gerar(email, permissoesPorEmail.getOrDefault(email, List.of()));
    }
}
