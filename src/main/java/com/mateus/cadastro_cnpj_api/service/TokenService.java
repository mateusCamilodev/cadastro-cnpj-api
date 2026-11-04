package com.mateus.cadastro_cnpj_api.service;

import com.mateus.cadastro_cnpj_api.dto.TokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

// Gera o JWT: quem é o usuário (subject), quando foi emitido e quando expira, tudo assinado com o segredo
@Service
public class TokenService {

    public static final String EMISSOR = "cadastro-cnpj-api";

    private final JwtEncoder encoder;
    private final Duration expiracao;

    public TokenService(JwtEncoder encoder, @Value("${auth.jwt.expiracao}") Duration expiracao) {
        this.encoder = encoder;
        this.expiracao = expiracao;
    }

    public TokenResponse gerar(String email) {
        return gerar(email, List.of());
    }

    // As permissões vão na claim "scope" (separadas por espaço). O Spring Security transforma
    // cada uma em uma authority "SCOPE_<permissao>" (ex.: SCOPE_qa.view) ao validar o token.
    public TokenResponse gerar(String email, List<String> permissoes) {
        Instant agora = Instant.now();

        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .subject(email)
                .issuedAt(agora)
                .expiresAt(agora.plus(expiracao));
        if (!permissoes.isEmpty()) {
            builder.claim("scope", String.join(" ", permissoes));
        }
        JwtClaimsSet claims = builder.build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", expiracao.toSeconds());
    }
}
