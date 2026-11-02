package com.mateus.cadastro_cnpj_api.config;

import com.mateus.cadastro_cnpj_api.service.TokenService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

// Quem pode acessar o quê + como o token é assinado e validado
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

    private final SecretKey chave;

    public SecurityConfig(@Value("${auth.jwt.segredo}") String segredo) {
        this.chave = new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationEntryPoint entryPoint) throws Exception {
        http
                // API sem sessão nem formulário: cada requisição traz o próprio token
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // O H2 Console abre dentro de um frame
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, "/auth/token").permitAll()
                        .requestMatchers("/h2-console/**", "/error").permitAll()
                        .anyRequest().authenticated())
                // Lê o header "Authorization: Bearer <token>" e valida o JWT
                .oauth2ResourceServer(o -> o
                        .jwt(jwt -> jwt.decoder(jwtDecoder()))
                        .authenticationEntryPoint(entryPoint))
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint));
        return http.build();
    }

    // 401 sai no mesmo formato JSON dos outros erros: repassa a exceção para o GlobalExceptionHandler
    @Bean
    public AuthenticationEntryPoint entryPoint(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        return (request, response, ex) -> resolver.resolveException(request, response, null, ex);
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Duration.ZERO: expira exatamente no horário do token (o padrão do Spring dá 60s de tolerância)
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO),
                new JwtIssuerValidator(TokenService.EMISSOR)));
        return decoder;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
