package com.mateus.cadastro_cnpj_api.qa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// Testes do QA Center com a aplicação de verdade (porta aleatória): o executor chama a própria API,
// nada é simulado. Tempos do agendador encurtados no perfil "test".
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class QaCenterIntegracaoTest {

    private static final List<String> RAPIDOS_ENDPOINTS = List.of("AUTH_TOKEN", "SOL_CRIAR", "SOL_LISTAR", "SOL_CONSULTAR", "SOL_CANCELAR");
    private static final List<String> RAPIDAS_CATEGORIAS = List.of("CONTRATO", "STATUS_HTTP", "AUTENTICACAO",
            "CAMPOS_OBRIGATORIOS", "DADOS_INVALIDOS", "INTEGRACAO", "REGRAS_NEGOCIO");

    @LocalServerPort
    int porta;

    @Autowired
    ObjectMapper mapper;

    RestClient http;
    String admin;
    String semPermissao;

    @BeforeEach
    void preparar() {
        http = RestClient.builder().baseUrl("http://localhost:" + porta).build();
        admin = token("qa-admin@teste.com", "senha-admin");
        semPermissao = token("sem-permissao@teste.com", "senha-comum");
    }

    // ===================== Permissões =====================

    @Test
    void permissoesSaoValidadasNoBackend() {
        assertEquals(401, status("GET", "/qa/endpoints", null, null));
        assertEquals(403, status("GET", "/qa/endpoints", semPermissao, null));
        assertEquals(403, status("POST", "/qa/execucoes", semPermissao, Map.of("ambiente", "DESENVOLVIMENTO")));
        assertEquals(403, status("GET", "/qa/relatorios/resumo", semPermissao, null));
        assertEquals(200, status("GET", "/qa/endpoints", admin, null));
        // O fluxo normal do Carimbô continua aberto para quem não tem permissão de QA
        assertEquals(200, status("GET", "/solicitacoes", semPermissao, null));
    }

    @Test
    void tokenDeQuemTemPermissaoCarregaOScope() {
        String payload = new String(java.util.Base64.getUrlDecoder().decode(admin.split("\\.")[1]), StandardCharsets.UTF_8);
        assertTrue(payload.contains("qa.view") && payload.contains("qa.execute"), payload);
        String comum = new String(java.util.Base64.getUrlDecoder().decode(semPermissao.split("\\.")[1]), StandardCharsets.UTF_8);
        assertFalse(comum.contains("scope"), comum);
    }

    // ===================== Catálogo =====================

    @Test
    void catalogoListaEndpointsECenariosReais() {
        JsonNode endpoints = get("/qa/endpoints", admin);
        assertEquals(7, endpoints.size());
        assertTrue(endpoints.values().stream().anyMatch(e -> "DOC_ENVIAR".equals(e.path("id").asString())
                && "POST".equals(e.path("metodo").asString()) && "/solicitacoes/{id}/documento".equals(e.path("caminho").asString())));

        JsonNode cenarios = get("/qa/cenarios", admin);
        assertTrue(cenarios.size() >= 20, "cenários: " + cenarios.size());
        assertEquals("AUTH_TOKEN", get("/qa/cenarios/AUTH-001", admin).path("endpointId").asString());
        assertEquals(404, status("GET", "/qa/cenarios/NAO-EXISTE", admin, null));

        // Categoria sem cenário real aparece com 0 (o front a mostra desabilitada; nada é inventado)
        JsonNode categorias = get("/qa/categorias", admin);
        assertTrue(categorias.values().stream().anyMatch(c -> "AUTORIZACAO".equals(c.path("id").asString()) && c.path("cenarios").asInt() == 0));
    }

    // ===================== Execução assíncrona =====================

    @Test
    void execucaoCompletaRodaEmSegundoPlanoEGuardaOsResultados() throws Exception {
        JsonNode criada = post("/qa/execucoes", admin, Map.of("ambiente", "DESENVOLVIMENTO",
                "endpointIds", RAPIDOS_ENDPOINTS, "categorias", RAPIDAS_CATEGORIAS));
        String id = criada.path("id").asString();
        int total = criada.path("total").asInt();
        assertTrue(Set.of("PENDING", "RUNNING").contains(criada.path("status").asString()), "volta antes de rodar: " + criada);
        assertTrue(total >= 15);

        JsonNode fim = esperarFim(id, 90);
        assertEquals("COMPLETED", fim.path("execucao").path("status").asString());
        assertEquals(total, fim.path("execucao").path("concluidos").asInt(), "nenhum cenário pode ficar pendente");
        assertEquals(total, fim.path("execucao").path("aprovados").asInt(), "falhas: " + falhas(fim));

        JsonNode resultados = get("/qa/execucoes/" + id + "/resultados", admin);
        assertEquals(total, resultados.size());
        for (JsonNode r : resultados.values()) {
            assertEquals("PASSED", r.path("status").asString(), r.toString());
            assertTrue(r.path("httpStatus").asInt() >= 200, r.toString());
            assertTrue(r.path("evidencias").path("validacoes").size() > 0, r.toString());
        }
        // Evidências sem segredos
        String texto = resultados.toString();
        assertFalse(texto.matches("(?s).*eyJ[\\w-]+\\.[\\w-]+\\.[\\w-]+.*"), "JWT cru nas evidências");
        assertFalse(texto.contains("senha-executor"), "senha crua nas evidências");

        // Histórico e relatórios com dados reais
        assertTrue(get("/qa/execucoes", admin).path("total").asInt() >= 1);
        assertTrue(get("/qa/relatorios/resumo", admin).path("testes").asInt() >= total);
        String csv = http.get().uri("/qa/relatorios/exportar.csv?execucao=" + id).header("Authorization", "Bearer " + admin)
                .retrieve().body(String.class);
        assertTrue(csv.contains("execucao;criada_em;usuario") && csv.contains("AUTH-001"), csv);
    }

    @Test
    void cenariosDeDocumentoEAnaliseRodamDeVerdade() throws Exception {
        JsonNode criada = post("/qa/execucoes", admin, Map.of("ambiente", "DESENVOLVIMENTO",
                "cenarioIds", List.of("DOC-001", "DOC-003", "VAL-001", "VAL-002")));
        JsonNode fim = esperarFim(criada.path("id").asString(), 120);
        assertEquals("COMPLETED", fim.path("execucao").path("status").asString());
        assertEquals(4, fim.path("execucao").path("aprovados").asInt(), "falhas: " + falhas(fim));
    }

    @Test
    void duplicadaLimiteECancelamento() throws Exception {
        Map<String, Object> lenta = Map.of("ambiente", "DESENVOLVIMENTO", "cenarioIds", List.of("VAL-001", "VAL-002", "DOC-001", "DOC-002"));
        JsonNode criada = post("/qa/execucoes", admin, lenta);
        String id = criada.path("id").asString();

        // Mesma seleção em andamento → 409; outra seleção, mesmo usuário (limite 1) → 429
        assertEquals(409, status("POST", "/qa/execucoes", admin, lenta));
        assertEquals(429, status("POST", "/qa/execucoes", admin, Map.of("ambiente", "DESENVOLVIMENTO", "cenarioIds", List.of("AUTH-001"))));

        assertEquals(200, status("POST", "/qa/execucoes/" + id + "/cancelar", admin, null));
        JsonNode fim = esperarFim(id, 60);
        assertEquals("CANCELLED", fim.path("execucao").path("status").asString());
        assertEquals(fim.path("execucao").path("total").asInt(), fim.path("execucao").path("concluidos").asInt());
        assertEquals(409, status("POST", "/qa/execucoes/" + id + "/cancelar", admin, null), "cancelar de novo");
        // Os cenários interrompidos pelo cancelamento ficam CANCELLED (não "bloqueados")
        assertTrue(fim.path("execucao").path("cancelados").asInt() >= 1, "cancelados: " + fim.path("execucao"));
        assertTrue(fim.path("resultados").values().stream().anyMatch(r -> "CANCELLED".equals(r.path("status").asString())));
        // E o cancelamento fica na auditoria, com quem cancelou
        assertTrue(get("/qa/auditoria?quantidade=50", admin).values().stream().anyMatch(a ->
                "EXECUCAO_CANCELADA".equals(a.path("acao").asString()) && id.equals(a.path("alvo").asString())
                        && "qa-admin@teste.com".equals(a.path("usuario").asString())));
    }

    @Test
    void reexecutarCriaNovaExecucaoPreservandoAOriginal() throws Exception {
        JsonNode original = post("/qa/execucoes", admin, Map.of("ambiente", "DESENVOLVIMENTO", "cenarioIds", List.of("AUTH-001", "SOL-008")));
        String id = original.path("id").asString();
        esperarFim(id, 60);

        JsonNode nova = post("/qa/execucoes/" + id + "/reexecutar", admin, null);
        assertNotEquals(id, nova.path("id").asString());
        assertEquals(id, nova.path("origemId").asString());
        esperarFim(nova.path("id").asString(), 60);
        assertEquals("COMPLETED", get("/qa/execucoes/" + id, admin).path("execucao").path("status").asString(), "a original não muda");
    }

    @Test
    void selecaoInvalidaEhRecusada() {
        assertEquals(400, status("POST", "/qa/execucoes", admin, Map.of("ambiente", "DESENVOLVIMENTO", "endpointIds", List.of("SOL_CRIAR"))));
        assertEquals(400, status("POST", "/qa/execucoes", admin, Map.of("ambiente", "DESENVOLVIMENTO", "cenarioIds", List.of("NAO-EXISTE"))));
        assertEquals(400, status("POST", "/qa/execucoes", admin, Map.of("endpointIds", List.of("SOL_CRIAR"), "categorias", List.of("INTEGRACAO"))));
        // Homologação aponta para uma porta sem servidor → indisponível
        assertEquals(503, status("POST", "/qa/execucoes", admin, Map.of("ambiente", "HOMOLOGACAO", "cenarioIds", List.of("AUTH-001"))));
        // Não existe ambiente "PRODUCAO"
        assertEquals(400, status("POST", "/qa/execucoes", admin, Map.of("ambiente", "PRODUCAO", "cenarioIds", List.of("AUTH-001"))));
    }

    // ===================== Test Suites =====================

    @Test
    void suitesPadraoECriacaoComPermissaoQaConfigure() throws Exception {
        String operador = token("operador@teste.com", "senha-operador");
        JsonNode suites = get("/qa/suites", admin);
        List<String> padrao = suites.values().stream().filter(s -> s.path("padrao").asBoolean()).map(s -> s.path("nome").asString()).toList();
        assertTrue(padrao.containsAll(List.of("Autenticação", "Solicitações", "Documentos", "Fluxo completo")), padrao.toString());
        String idPadrao = suites.values().stream().filter(s -> s.path("padrao").asBoolean()).findFirst().orElseThrow().path("id").asString();

        Map<String, Object> nova = Map.of("nome", "Regressão rápida QA", "descricao", "teste automatizado",
                "cenarioIds", List.of("AUTH-001", "AUTH-006", "SOL-008"));
        // Sem qa.configure → 403 (validado no backend, não só na tela)
        assertEquals(200, status("GET", "/qa/suites", operador, null));
        assertEquals(403, status("POST", "/qa/suites", operador, nova));
        assertEquals(403, status("DELETE", "/qa/suites/" + idPadrao, operador, null));
        // Validações
        assertEquals(400, status("POST", "/qa/suites", admin, Map.of("nome", "Sem cenários", "cenarioIds", List.of())));
        assertEquals(400, status("POST", "/qa/suites", admin, Map.of("nome", "Cenário inventado", "cenarioIds", List.of("XYZ-999"))));
        assertEquals(409, status("PUT", "/qa/suites/" + idPadrao, admin, nova), "suíte padrão é somente leitura");

        JsonNode criada = post("/qa/suites", admin, nova);
        String suiteId = criada.path("id").asString();
        assertEquals(3, criada.path("cenarioIds").size());
        assertEquals(409, status("POST", "/qa/suites", admin, nova), "nome repetido");

        // Executa a suíte (inclui AUTH-006, token expirado) e confere o vínculo no histórico
        JsonNode exec = post("/qa/execucoes", admin, Map.of("ambiente", "DESENVOLVIMENTO", "suiteId", suiteId));
        assertEquals("Regressão rápida QA", exec.path("suiteNome").asString());
        JsonNode fim = esperarFim(exec.path("id").asString(), 60);
        assertEquals("COMPLETED", fim.path("execucao").path("status").asString());
        assertEquals(3, fim.path("execucao").path("aprovados").asInt(), "falhas: " + falhas(fim));

        JsonNode porSuite = get("/qa/execucoes?suite=" + suiteId, admin);
        assertEquals(1, porSuite.path("total").asInt());
        assertTrue(get("/qa/execucoes?endpoint=AUTH_TOKEN&resultado=SEM_FALHAS", admin).path("itens").values().stream()
                .anyMatch(e -> e.path("id").asString().equals(exec.path("id").asString())));
        assertEquals(0, get("/qa/execucoes?suite=" + suiteId + "&resultado=COM_FALHAS", admin).path("total").asInt());
        assertEquals(400, status("GET", "/qa/execucoes?resultado=QUALQUER", admin, null));
        assertEquals(1, get("/qa/suites/" + suiteId, admin).path("execucoes").asInt());

        // Exclui: a execução antiga mantém o nome da suíte
        assertEquals(204, status("DELETE", "/qa/suites/" + suiteId, admin, null));
        assertEquals(404, status("GET", "/qa/suites/" + suiteId, admin, null));
        assertEquals("Regressão rápida QA", get("/qa/execucoes/" + exec.path("id").asString(), admin).path("execucao").path("suiteNome").asString());

        List<String> acoes = get("/qa/auditoria?quantidade=100", admin).values().stream().map(a -> a.path("acao").asString()).toList();
        assertTrue(acoes.containsAll(List.of("SUITE_CRIADA", "SUITE_EXCLUIDA", "EXECUCAO_CRIADA")), acoes.toString());
    }

    // ===================== Configurações =====================

    @Test
    void configuracoesSoComQaConfigureEDentroDosLimites() {
        String operador = token("operador@teste.com", "senha-operador");
        JsonNode atual = get("/qa/configuracoes", operador);
        int original = atual.path("timeoutRequisicaoSegundos").asInt();
        assertEquals(60, atual.path("limites").path("timeoutRequisicaoMax").asInt());

        assertEquals(403, status("PUT", "/qa/configuracoes", operador, Map.of("timeoutRequisicaoSegundos", 20)));
        assertEquals(403, status("GET", "/qa/auditoria", operador, null));
        assertEquals(400, status("PUT", "/qa/configuracoes", admin, Map.of("timeoutRequisicaoSegundos", 0)));
        assertEquals(400, status("PUT", "/qa/configuracoes", admin, Map.of("cenariosParalelos", 9)));
        // Timeout pedido numa execução também é limitado
        assertEquals(400, status("POST", "/qa/execucoes", admin, Map.of("ambiente", "DESENVOLVIMENTO",
                "cenarioIds", List.of("AUTH-001"), "timeoutRequisicaoSegundos", 999)));

        int novo = original == 20 ? 21 : 20;
        JsonNode alterada = put("/qa/configuracoes", admin, Map.of("timeoutRequisicaoSegundos", novo));
        assertEquals(novo, alterada.path("timeoutRequisicaoSegundos").asInt());
        assertEquals("qa-admin@teste.com", alterada.path("atualizadaPor").asString());
        assertTrue(get("/qa/auditoria", admin).values().stream().anyMatch(a -> "CONFIGURACAO_ALTERADA".equals(a.path("acao").asString())));
        put("/qa/configuracoes", admin, Map.of("timeoutRequisicaoSegundos", original));
    }

    @Test
    void resumoTrazCoberturaReal() throws Exception {
        JsonNode exec = post("/qa/execucoes", admin, Map.of("ambiente", "DESENVOLVIMENTO", "cenarioIds", List.of("SOL-006", "SOL-009")));
        esperarFim(exec.path("id").asString(), 60);
        JsonNode r = get("/qa/relatorios/resumo", admin);
        assertEquals(7, r.path("endpointsTotal").asInt());
        assertTrue(r.path("endpointsTestados").asInt() >= 2, r.toString());
        assertTrue(r.path("coberturaEndpoints").asDouble() > 0, r.toString());
        assertTrue(r.path("cenariosTotal").asInt() >= 25, r.toString());
        JsonNode cmp = get("/qa/relatorios/comparacao?a=" + exec.path("id").asString() + "&b=" + exec.path("id").asString(), admin);
        assertEquals(0, cmp.path("regressoes").asInt());
        assertEquals(0, cmp.path("mudancas").asInt());
    }

    // ===================== Utilitários =====================

    private String token(String email, String senha) {
        JsonNode r = mapper.readTree(http.post().uri("/auth/token").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "senha", senha)).retrieve().body(String.class));
        return r.path("accessToken").asString();
    }

    private int status(String metodo, String caminho, String token, Object corpo) {
        RestClient.RequestBodySpec req = http.method(org.springframework.http.HttpMethod.valueOf(metodo)).uri(caminho);
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (corpo != null) req.contentType(MediaType.APPLICATION_JSON).body(corpo);
        return req.exchange((pedido, resposta) -> resposta.getStatusCode().value());
    }

    private JsonNode get(String caminho, String token) {
        return mapper.readTree(http.get().uri(caminho).header("Authorization", "Bearer " + token).retrieve().body(String.class));
    }

    private JsonNode post(String caminho, String token, Object corpo) {
        RestClient.RequestBodySpec req = http.post().uri(caminho).header("Authorization", "Bearer " + token);
        if (corpo != null) req.contentType(MediaType.APPLICATION_JSON).body(corpo);
        return mapper.readTree(req.retrieve().body(String.class));
    }

    private JsonNode put(String caminho, String token, Object corpo) {
        return mapper.readTree(http.put().uri(caminho).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).body(corpo).retrieve().body(String.class));
    }

    private JsonNode esperarFim(String id, int segundos) throws InterruptedException {
        long limite = System.currentTimeMillis() + segundos * 1000L;
        JsonNode detalhe = null;
        while (System.currentTimeMillis() < limite) {
            detalhe = get("/qa/execucoes/" + id, admin);
            String s = detalhe.path("execucao").path("status").asString();
            if (!s.equals("PENDING") && !s.equals("RUNNING")) return detalhe;
            Thread.sleep(500);
        }
        fail("Execução não terminou em " + segundos + "s: " + detalhe);
        return detalhe;
    }

    private static String falhas(JsonNode detalhe) {
        StringBuilder sb = new StringBuilder();
        detalhe.path("resultados").values().stream().filter(r -> !"PASSED".equals(r.path("status").asString()))
                .forEach(r -> sb.append(r.path("cenarioId").asString()).append('=').append(r.path("status").asString())
                        .append(" (").append(r.path("mensagem").asString("")).append("); "));
        return sb.toString();
    }
}
