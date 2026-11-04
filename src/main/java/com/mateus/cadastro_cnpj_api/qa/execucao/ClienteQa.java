package com.mateus.cadastro_cnpj_api.qa.execucao;

import com.mateus.cadastro_cnpj_api.qa.catalogo.EndpointQa;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

// Cliente HTTP do executor. Segurança (anti-SSRF): o destino é SEMPRE
//   URL base do ambiente autorizado (configuração do servidor) + caminho fixo do catálogo de endpoints.
// Nenhuma URL ou caminho vem do navegador. Erros HTTP (4xx/5xx) não viram exceção: são o resultado do teste.
public class ClienteQa {

    private final String urlBase;
    private final RestClient rest;
    private final ObjectMapper mapper;

    public ClienteQa(String urlBase, Duration timeout, ObjectMapper mapper) {
        this.urlBase = urlBase;
        this.mapper = mapper;
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(timeout);
        this.rest = RestClient.builder().requestFactory(fabrica).build();
    }

    // Chamada com corpo JSON (texto cru: permite mandar campos inválidos de propósito)
    public Resultado json(String rotulo, boolean principal, EndpointQa endpoint, Map<String, String> variaveis,
                          Map<String, String> query, String corpoJson, String token) {
        URI uri = montarUri(endpoint, variaveis, query);
        HttpHeaders headers = new HttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        if (corpoJson != null) headers.setContentType(MediaType.APPLICATION_JSON);

        RestClient.RequestBodySpec req = rest.method(HttpMethod.valueOf(endpoint.metodo())).uri(uri)
                .headers(h -> h.addAll(headers));
        if (corpoJson != null) req.body(corpoJson.getBytes(StandardCharsets.UTF_8));
        return executar(rotulo, principal, endpoint, uri, headers, corpoJson, req);
    }

    // Upload multipart (campo "arquivo"), como o POST /solicitacoes/{id}/documento espera
    public Resultado arquivo(String rotulo, boolean principal, EndpointQa endpoint, Map<String, String> variaveis,
                             String nomeArquivo, byte[] conteudo, MediaType tipo, String token) {
        URI uri = montarUri(endpoint, variaveis, Map.of());
        ByteArrayResource recurso = new ByteArrayResource(conteudo) {
            @Override
            public String getFilename() {
                return nomeArquivo;
            }
        };
        HttpHeaders parteHeaders = new HttpHeaders();
        parteHeaders.setContentType(tipo);
        MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
        partes.add("arquivo", new HttpEntity<>(recurso, parteHeaders));

        HttpHeaders headers = new HttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        RestClient.RequestBodySpec req = rest.method(HttpMethod.valueOf(endpoint.metodo())).uri(uri)
                .headers(h -> h.addAll(headers)).body(partes);
        String descricaoCorpo = "multipart: arquivo=\"" + nomeArquivo + "\" (" + conteudo.length + " bytes, " + tipo + ")";
        return executar(rotulo, principal, endpoint, uri, headers, descricaoCorpo, req);
    }

    private Resultado executar(String rotulo, boolean principal, EndpointQa endpoint, URI uri, HttpHeaders headers,
                               String corpoEnviado, RestClient.RequestBodySpec req) {
        long inicio = System.nanoTime();
        RespostaQa resposta = req.exchange((pedido, retorno) -> {
            byte[] bytes;
            try {
                bytes = retorno.getBody().readAllBytes();
            } catch (IOException e) {
                bytes = new byte[0];
            }
            Map<String, String> h = new LinkedHashMap<>();
            retorno.getHeaders().forEach((k, v) -> h.put(k, String.join(", ", v)));
            long duracao = (System.nanoTime() - inicio) / 1_000_000;
            return new RespostaQa(retorno.getStatusCode().value(), h, new String(bytes, StandardCharsets.UTF_8), duracao, mapper);
        });

        Map<String, String> headersVisiveis = new LinkedHashMap<>();
        headersVisiveis.put("Authorization", Mascarador.autorizacao(headers.getFirst(HttpHeaders.AUTHORIZATION)));
        if (headers.getContentType() != null) headersVisiveis.put("Content-Type", headers.getContentType().toString());
        String caminho = uri.getRawPath() + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");

        ChamadaQa evidencia = new ChamadaQa(rotulo, principal, endpoint.metodo(), caminho, headersVisiveis,
                Mascarador.texto(corpoEnviado), resposta.status(), resposta.duracaoMs(), Mascarador.texto(resposta.corpo()));
        return new Resultado(resposta, evidencia);
    }

    private URI montarUri(EndpointQa endpoint, Map<String, String> variaveis, Map<String, String> query) {
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(urlBase).path(endpoint.caminho());
        query.forEach(b::queryParam);
        return b.buildAndExpand(variaveis).encode().toUri();
    }

    public record Resultado(RespostaQa resposta, ChamadaQa evidencia) {
    }
}
