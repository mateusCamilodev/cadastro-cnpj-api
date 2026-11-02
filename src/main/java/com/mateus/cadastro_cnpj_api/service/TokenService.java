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
        Instant agora = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .subject(email)
                .issuedAt(agora)
                .expiresAt(agora.plus(expiracao))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", expiracao.toSeconds());
    }
}
