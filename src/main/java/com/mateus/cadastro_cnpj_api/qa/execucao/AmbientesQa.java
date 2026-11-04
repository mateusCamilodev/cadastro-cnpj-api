package com.mateus.cadastro_cnpj_api.qa.execucao;

import com.mateus.cadastro_cnpj_api.qa.modelo.Ambiente;
import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

// Resolve a URL de cada ambiente (lida na hora, para aceitar ${local.server.port} nos testes)
// e verifica se ele está no ar. Os endereços vêm só da configuração do servidor.
@Component
public class AmbientesQa {

    private final Environment environment;
    private final RestClient sonda;

    public AmbientesQa(Environment environment) {
        this.environment = environment;
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        fabrica.setReadTimeout(Duration.ofSeconds(3));
        this.sonda = RestClient.builder().requestFactory(fabrica).build();
    }

    public String url(Ambiente ambiente) {
        String url = environment.getProperty(ambiente.propriedadeUrl());
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("URL do ambiente " + ambiente.rotulo() + " não configurada (" + ambiente.propriedadeUrl() + ")");
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    // Disponível = respondeu qualquer status HTTP (sem token, /solicitacoes devolve 401: o servidor está de pé)
    public boolean disponivel(Ambiente ambiente) {
        try {
            sonda.get().uri(url(ambiente) + "/solicitacoes").exchange((pedido, resposta) -> resposta.getStatusCode().value());
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
