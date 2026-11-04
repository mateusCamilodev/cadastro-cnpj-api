package com.mateus.cadastro_cnpj_api.config;

import com.mateus.cadastro_cnpj_api.service.TokenService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.DispatcherType;
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
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationEntryPoint entryPoint,
                                           @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {
        http
                // API sem sessão nem formulário: cada requisição traz o próprio token
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // O H2 Console abre dentro de um frame
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(a -> a
                        // Despacho assíncrono do SSE (QA Center): a autorização já foi feita na requisição original
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/token").permitAll()
                        .requestMatchers("/h2-console/**", "/error").permitAll()
                        // QA Center: cada operação exige a permissão própria (claim "scope" do token)
                        .requestMatchers(HttpMethod.GET, "/qa/relatorios/**").hasAuthority("SCOPE_qa.reports.view")
                        .requestMatchers(HttpMethod.GET, "/qa/auditoria").hasAuthority("SCOPE_qa.configure")
                        .requestMatchers(HttpMethod.PUT, "/qa/configuracoes").hasAuthority("SCOPE_qa.configure")
                        .requestMatchers(HttpMethod.POST, "/qa/suites").hasAuthority("SCOPE_qa.configure")
                        .requestMatchers(HttpMethod.PUT, "/qa/suites/*").hasAuthority("SCOPE_qa.configure")
                        .requestMatchers(HttpMethod.DELETE, "/qa/suites/*").hasAuthority("SCOPE_qa.configure")
                        .requestMatchers(HttpMethod.POST, "/qa/execucoes/*/cancelar").hasAuthority("SCOPE_qa.cancel")
                        .requestMatchers(HttpMethod.POST, "/qa/execucoes", "/qa/execucoes/*/reexecutar").hasAuthority("SCOPE_qa.execute")
                        .requestMatchers(HttpMethod.GET, "/qa/**").hasAuthority("SCOPE_qa.view")
                        .requestMatchers("/qa/**").denyAll()
                        .anyRequest().authenticated())
                // Lê o header "Authorization: Bearer <token>" e valida o JWT
                .oauth2ResourceServer(o -> o
                        .jwt(jwt -> jwt.decoder(jwtDecoder()))
                        .authenticationEntryPoint(entryPoint))
                // 403 também sai no formato JSON padrão: repassa a exceção para o GlobalExceptionHandler
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler((request, response, ex) -> resolver.resolveException(request, response, null, ex)));
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
