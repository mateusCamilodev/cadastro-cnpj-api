package com.mateus.cadastro_cnpj_api.qa.execucao;

import com.mateus.cadastro_cnpj_api.qa.catalogo.EndpointCatalogo;
import com.mateus.cadastro_cnpj_api.qa.catalogo.EndpointQa;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

// Tudo o que um cenário usa durante a execução: o cliente HTTP do ambiente, o token da conta de teste,
// helpers de preparação (criar solicitação, esperar status) e o registro das validações e evidências.
public class ContextoQa {

    // Pré-condição não atendida → o cenário fica SKIPPED (não é falha do endpoint testado)
    public static class PreCondicaoFalhou extends RuntimeException {
        public PreCondicaoFalhou(String mensagem) { super(mensagem); }
    }

    // A execução foi cancelada no meio de uma espera
    public static class Cancelado extends RuntimeException {
        public Cancelado() { super("Execução cancelada"); }
    }

    public record ValidacaoQa(String descricao, boolean ok, String esperado, String recebido) {
    }

    private final ClienteQa cliente;
    private final EndpointCatalogo endpoints;
    private final Supplier<String> tokenTeste;
    private final BooleanSupplier cancelado;
    private final ObjectMapper mapper;
    private final String prefixoDados;

    private final List<ChamadaQa> chamadas = new ArrayList<>();
    private final List<ValidacaoQa> validacoes = new ArrayList<>();
    private RespostaQa principal;

    public ContextoQa(ClienteQa cliente, EndpointCatalogo endpoints, Supplier<String> tokenTeste,
                      BooleanSupplier cancelado, ObjectMapper mapper, String prefixoDados) {
        this.cliente = cliente;
        this.endpoints = endpoints;
        this.tokenTeste = tokenTeste;
        this.cancelado = cancelado;
        this.mapper = mapper;
        this.prefixoDados = prefixoDados;
    }

    // ===== Chamadas =====

    public String token() {
        return tokenTeste.get();
    }

    private EndpointQa endpoint(String id) {
        return endpoints.buscar(id).orElseThrow(() -> new IllegalStateException("Endpoint fora do catálogo: " + id));
    }

    // A chamada que está sendo testada (a evidência principal do cenário)
    public RespostaQa testar(String endpointId, Map<String, String> variaveis, Map<String, String> query, Object corpo, String token) {
        return registrar(cliente.json("Requisição testada", true, endpoint(endpointId), variaveis, query, json(corpo), token), true);
    }

    public RespostaQa testarArquivo(String endpointId, String id, String nomeArquivo, byte[] conteudo, MediaType tipo) {
        return registrar(cliente.arquivo("Requisição testada", true, endpoint(endpointId), Map.of("id", id),
                nomeArquivo, conteudo, tipo, token()), true);
    }

    // Chamadas de preparação (ex.: criar a solicitação antes de testar o cancelamento)
    public RespostaQa preparar(String rotulo, String endpointId, Map<String, String> variaveis, Object corpo, String token) {
        return registrar(cliente.json(rotulo, false, endpoint(endpointId), variaveis, Map.of(), json(corpo), token), false);
    }

    public RespostaQa prepararArquivo(String rotulo, String id, String nomeArquivo, byte[] conteudo) {
        return registrar(cliente.arquivo(rotulo, false, endpoint("DOC_ENVIAR"), Map.of("id", id),
                nomeArquivo, conteudo, MediaType.APPLICATION_PDF, token()), false);
    }

    private RespostaQa registrar(ClienteQa.Resultado r, boolean principal) {
        chamadas.add(r.evidencia());
        if (principal) this.principal = r.resposta();
        return r.resposta();
    }

    private String json(Object corpo) {
        if (corpo == null) return null;
        if (corpo instanceof String s) return s;
        return mapper.writeValueAsString(corpo);
    }

    // ===== Helpers de preparação =====

    public DadosDeTeste.Solicitacao novaSolicitacao(String cenarioId, List<String> arquivos) {
        return DadosDeTeste.nova(prefixoDados + "-" + cenarioId, arquivos);
    }

    // Cria a solicitação (pré-condição): se a API não devolver 201, o cenário é pulado
    public String criarSolicitacao(DadosDeTeste.Solicitacao dados) {
        RespostaQa r = preparar("Preparação: criar solicitação", "SOL_CRIAR", Map.of(), dados.corpo(), token());
        String id = r.json().path("id").asString("");
        if (r.status() != 201 || id.isBlank()) {
            throw new PreCondicaoFalhou("Não foi possível criar a solicitação de teste (HTTP " + r.status() + ")");
        }
        return id;
    }

    // Consulta até a solicitação chegar a um dos status (o agendador da API muda o status sozinho)
    public String esperarStatus(String id, Set<String> alvos, Duration maximo) {
        long limite = System.currentTimeMillis() + maximo.toMillis();
        String status = "";
        while (System.currentTimeMillis() < limite) {
            if (cancelado.getAsBoolean()) throw new Cancelado();
            ClienteQa.Resultado r = cliente.json("Preparação: consultar status", false, endpoint("SOL_CONSULTAR"),
                    Map.of("id", id), Map.of(), null, token());
            status = r.resposta().json().path("status").asString("");
            if (alvos.contains(status)) {
                chamadas.add(r.evidencia());
                return status;
            }
            dormir(1000);
        }
        return status;
    }

    public void exigirStatus(String id, Set<String> alvos, Duration maximo) {
        String status = esperarStatus(id, alvos, maximo);
        if (!alvos.contains(status)) {
            throw new PreCondicaoFalhou("A solicitação não chegou a " + alvos + " em " + maximo.toSeconds() + "s (status: " + status + ")");
        }
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Cancelado();
        }
    }

    // ===== Validações =====

    public void verificar(String descricao, boolean ok, Object esperado, Object recebido) {
        validacoes.add(new ValidacaoQa(descricao, ok, String.valueOf(esperado), Mascarador.texto(String.valueOf(recebido))));
    }

    public void verificarStatus(RespostaQa r, int esperado) {
        verificar("Status HTTP " + esperado, r.status() == esperado, esperado, r.status());
    }

    public void verificarMensagemContem(RespostaQa r, String trecho) {
        String mensagem = r.mensagem();
        verificar("Mensagem contém \"" + trecho + "\"", mensagem.contains(trecho), trecho, mensagem.isBlank() ? "(sem mensagem)" : mensagem);
    }

    public static UUID uuidOuNulo(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public List<ChamadaQa> chamadas() { return chamadas; }
    public List<ValidacaoQa> validacoes() { return validacoes; }
    public RespostaQa principal() { return principal; }
}
