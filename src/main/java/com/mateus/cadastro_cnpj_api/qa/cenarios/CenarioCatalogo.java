package com.mateus.cadastro_cnpj_api.qa.cenarios;

import com.mateus.cadastro_cnpj_api.qa.config.QaProperties;
import com.mateus.cadastro_cnpj_api.service.TokenService;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import java.time.Instant;
import com.mateus.cadastro_cnpj_api.qa.execucao.ContextoQa;
import com.mateus.cadastro_cnpj_api.qa.execucao.DadosDeTeste;
import com.mateus.cadastro_cnpj_api.qa.execucao.RespostaQa;
import com.mateus.cadastro_cnpj_api.qa.modelo.Categoria;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.mateus.cadastro_cnpj_api.qa.modelo.Categoria.*;

// Test Scenario Registry: cenários reais contra a API do Carimbô. Os códigos HTTP e as mensagens
// esperadas foram conferidos no código (GlobalExceptionHandler, SolicitacaoService, LeitorDocumentoPdf...).
// Cada cenário cria os próprios dados (marcados "QA-") e não depende de outro cenário.
@Component
public class CenarioCatalogo {

    private static final Duration ATE_AGUARDANDO = Duration.ofSeconds(40);   // tempo-recebido (10s) + agendador
    private static final Duration ATE_RESULTADO = Duration.ofSeconds(90);    // tempo-processamento (30s) + agendador
    private static final Set<String> AGUARDANDO = Set.of("AGUARDANDO_DOCUMENTO");
    private static final Set<String> FINAIS = Set.of("APROVADO", "REPROVADO");

    private final Map<String, CenarioQa> porId = new LinkedHashMap<>();
    private final QaProperties.ContaTeste conta;
    private final JwtEncoder jwtEncoder;

    public CenarioCatalogo(QaProperties properties, JwtEncoder jwtEncoder) {
        this.conta = properties.contaTeste() != null ? properties.contaTeste() : new QaProperties.ContaTeste("", "");
        this.jwtEncoder = jwtEncoder;
        autenticacao();
        solicitacoes();
        documentos();
        analise();
    }

    private void cenario(String id, String nome, String descricao, String endpointId, Categoria categoria,
                         String criticidade, int timeout, String pre, String esperado, CenarioQa.Logica logica) {
        porId.put(id, new CenarioQa(id, nome, descricao, endpointId, categoria, criticidade, timeout, pre, esperado, logica));
    }

    // ===================== Autenticação =====================
    private void autenticacao() {
        cenario("AUTH-001", "Login válido", "Gera token com a conta de teste do QA Center.",
                "AUTH_TOKEN", AUTENTICACAO, "ALTA", 15, "Conta de teste configurada.",
                "200 com accessToken, tokenType Bearer e expiresIn > 0", (ctx, id) -> {
                    RespostaQa r = ctx.testar("AUTH_TOKEN", Map.of(), Map.of(), Map.of("email", conta.email(), "senha", conta.senha()), null);
                    ctx.verificarStatus(r, 200);
                    JsonNode j = r.json();
                    ctx.verificar("Tem accessToken", !j.path("accessToken").asString("").isBlank(), "presente", j.has("accessToken") ? "presente" : "ausente");
                    ctx.verificar("tokenType = Bearer", "Bearer".equals(j.path("tokenType").asString("")), "Bearer", j.path("tokenType").asString(""));
                    ctx.verificar("expiresIn > 0", j.path("expiresIn").asInt(0) > 0, "> 0", j.path("expiresIn").asInt(0));
                });

        cenario("AUTH-002", "Login com senha inválida", "Senha errada não pode gerar token.",
                "AUTH_TOKEN", AUTENTICACAO, "ALTA", 15, "Nenhuma.",
                "401 \"E-mail ou senha inválidos\"", (ctx, id) -> {
                    RespostaQa r = ctx.testar("AUTH_TOKEN", Map.of(), Map.of(), Map.of("email", conta.email(), "senha", "senha-errada-qa"), null);
                    ctx.verificarStatus(r, 401);
                    ctx.verificarMensagemContem(r, "E-mail ou senha inválidos");
                });

        cenario("AUTH-003", "Login sem e-mail e senha", "Corpo vazio deve ser recusado na validação.",
                "AUTH_TOKEN", CAMPOS_OBRIGATORIOS, "MEDIA", 15, "Nenhuma.",
                "400 com detalhes dos campos email e senha", (ctx, id) -> {
                    RespostaQa r = ctx.testar("AUTH_TOKEN", Map.of(), Map.of(), "{}", null);
                    ctx.verificarStatus(r, 400);
                    ctx.verificar("Detalhe do campo email", temCampo(r, "email"), "email", r.json().path("detalhes"));
                    ctx.verificar("Detalhe do campo senha", temCampo(r, "senha"), "senha", r.json().path("detalhes"));
                });

        cenario("AUTH-004", "Rota protegida sem token", "GET /solicitacoes sem header Authorization.",
                "SOL_LISTAR", AUTENTICACAO, "ALTA", 15, "Nenhuma.",
                "401 \"Token ausente\" + header WWW-Authenticate", (ctx, id) -> {
                    RespostaQa r = ctx.testar("SOL_LISTAR", Map.of(), Map.of(), null, null);
                    ctx.verificarStatus(r, 401);
                    ctx.verificarMensagemContem(r, "Token ausente");
                    ctx.verificar("Header WWW-Authenticate", r.header("WWW-Authenticate") != null, "presente", r.header("WWW-Authenticate"));
                });

        cenario("AUTH-005", "Token adulterado", "Token com a assinatura alterada deve ser recusado.",
                "SOL_LISTAR", AUTENTICACAO, "ALTA", 15, "Login da conta de teste.",
                "401 \"Token inválido ou expirado\"", (ctx, id) -> {
                    String token = ctx.token();
                    String adulterado = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");
                    RespostaQa r = ctx.testar("SOL_LISTAR", Map.of(), Map.of(), null, adulterado);
                    ctx.verificarStatus(r, 401);
                    ctx.verificarMensagemContem(r, "Token inválido ou expirado");
                });

        cenario("AUTH-006", "Token expirado", "Token com assinatura válida, mas com o prazo vencido, deve ser recusado.",
                "SOL_LISTAR", AUTENTICACAO, "ALTA", 15,
                "O ambiente usa o mesmo segredo de assinatura do executor (conferido com um token de controle).",
                "Token de controle aceito (200); o mesmo token vencido recusado com 401 \"Token inválido ou expirado\"", (ctx, id) -> {
                    Instant agora = Instant.now();
                    // Controle: mesmo emissor, mesmo usuário e mesma assinatura, ainda dentro do prazo.
                    // Se o ambiente não aceitar, a assinatura dele é outra e o teste não teria valor: fica bloqueado.
                    RespostaQa controle = ctx.preparar("Preparação: token de controle (válido)", "SOL_LISTAR", Map.of(), null,
                            tokenAssinado(agora.minusSeconds(60), agora.plusSeconds(300)));
                    if (controle.status() != 200) {
                        throw new ContextoQa.PreCondicaoFalhou("o ambiente não aceitou um token válido assinado pelo executor (HTTP "
                                + controle.status() + "); ele usa outro segredo de assinatura");
                    }
                    String vencido = tokenAssinado(agora.minusSeconds(7200), agora.minusSeconds(3600));
                    RespostaQa r = ctx.testar("SOL_LISTAR", Map.of(), Map.of(), null, vencido);
                    ctx.verificarStatus(r, 401);
                    ctx.verificarMensagemContem(r, "Token inválido ou expirado");
                });
    }

    // Token da conta de teste com datas escolhidas (só para os cenários de expiração; nunca sai do backend)
    private String tokenAssinado(Instant emitidoEm, Instant expiraEm) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(TokenService.EMISSOR)
                .subject(conta.email())
                .issuedAt(emitidoEm)
                .expiresAt(expiraEm)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    // ===================== Solicitações =====================
    private void solicitacoes() {
        cenario("SOL-001", "Criar solicitação válida", "Cadastro completo com pessoa, empresa e 1 PDF declarado.",
                "SOL_CRIAR", INTEGRACAO, "ALTA", 20, "Login da conta de teste.",
                "201 com id (UUID) e header Location /solicitacoes/{id}", (ctx, id) -> {
                    DadosDeTeste.Solicitacao dados = ctx.novaSolicitacao(id, List.of("documento-qa.pdf"));
                    RespostaQa r = ctx.testar("SOL_CRIAR", Map.of(), Map.of(), dados.corpo(), ctx.token());
                    ctx.verificarStatus(r, 201);
                    String novoId = r.json().path("id").asString("");
                    ctx.verificar("id é um UUID", ContextoQa.uuidOuNulo(novoId) != null, "UUID", novoId);
                    String location = r.header("Location");
                    ctx.verificar("Location aponta para o recurso", location != null && location.endsWith("/solicitacoes/" + novoId),
                            "/solicitacoes/" + novoId, location);
                });

        cenario("SOL-002", "Criar sem o campo solicitacao", "O número da solicitação é obrigatório.",
                "SOL_CRIAR", CAMPOS_OBRIGATORIOS, "MEDIA", 20, "Login da conta de teste.",
                "400 com detalhe no campo solicitacao", (ctx, id) -> {
                    Map<String, Object> corpo = new LinkedHashMap<>(ctx.novaSolicitacao(id, List.of("a.pdf")).corpo());
                    corpo.remove("solicitacao");
                    RespostaQa r = ctx.testar("SOL_CRIAR", Map.of(), Map.of(), corpo, ctx.token());
                    ctx.verificarStatus(r, 400);
                    ctx.verificar("Detalhe do campo solicitacao", temCampo(r, "solicitacao"), "solicitacao", r.json().path("detalhes"));
                });

        cenario("SOL-003", "Criar com campo desconhecido (ipr)", "O antigo campo \"ipr\" foi renomeado e deve ser recusado.",
                "SOL_CRIAR", DADOS_INVALIDOS, "MEDIA", 20, "Login da conta de teste.",
                "400 \"Campo não reconhecido: ipr\"", (ctx, id) -> {
                    Map<String, Object> corpo = new LinkedHashMap<>(ctx.novaSolicitacao(id, List.of("a.pdf")).corpo());
                    corpo.put("ipr", "IPR-QA");
                    RespostaQa r = ctx.testar("SOL_CRIAR", Map.of(), Map.of(), corpo, ctx.token());
                    ctx.verificarStatus(r, 400);
                    ctx.verificarMensagemContem(r, "Campo não reconhecido: ipr");
                });

        cenario("SOL-004", "Número de solicitação duplicado", "Não pode existir duas solicitações ativas com o mesmo número.",
                "SOL_CRIAR", REGRAS_NEGOCIO, "ALTA", 25, "Uma solicitação ativa criada antes com o mesmo número.",
                "409 \"Já existe uma solicitação ativa com este número de solicitação\"", (ctx, id) -> {
                    DadosDeTeste.Solicitacao primeira = ctx.novaSolicitacao(id, List.of("a.pdf"));
                    ctx.criarSolicitacao(primeira);
                    DadosDeTeste.Solicitacao segunda = primeira.comCnpj(DadosDeTeste.cnpjUnico());
                    RespostaQa r = ctx.testar("SOL_CRIAR", Map.of(), Map.of(), segunda.corpo(), ctx.token());
                    ctx.verificarStatus(r, 409);
                    ctx.verificarMensagemContem(r, "Já existe uma solicitação ativa com este número de solicitação");
                });

        cenario("SOL-005", "CPF fora do formato", "CPF precisa ter 11 dígitos numéricos.",
                "SOL_CRIAR", DADOS_INVALIDOS, "MEDIA", 20, "Login da conta de teste.",
                "400 com detalhe no campo cpf", (ctx, id) -> {
                    Map<String, Object> corpo = new LinkedHashMap<>(ctx.novaSolicitacao(id, List.of("a.pdf")).corpo());
                    @SuppressWarnings("unchecked")
                    Map<String, Object> pessoa = new LinkedHashMap<>((Map<String, Object>) corpo.get("pessoa"));
                    pessoa.put("cpf", "123");
                    corpo.put("pessoa", pessoa);
                    RespostaQa r = ctx.testar("SOL_CRIAR", Map.of(), Map.of(), corpo, ctx.token());
                    ctx.verificarStatus(r, 400);
                    ctx.verificar("Detalhe do campo cpf", temCampo(r, "cpf"), "cpf", r.json().path("detalhes"));
                });

        cenario("SOL-006", "Contrato da listagem", "A solicitação criada aparece na lista com todos os campos do contrato.",
                "SOL_LISTAR", CONTRATO, "ALTA", 25, "Uma solicitação criada pelo cenário.",
                "200; item com id, solicitacao, status, datas, documentos e arquivos", (ctx, id) -> {
                    DadosDeTeste.Solicitacao dados = ctx.novaSolicitacao(id, List.of("a.pdf"));
                    String criada = ctx.criarSolicitacao(dados);
                    RespostaQa r = ctx.testar("SOL_LISTAR", Map.of(), Map.of(), null, ctx.token());
                    ctx.verificarStatus(r, 200);
                    ctx.verificar("Resposta é uma lista", r.json().isArray(), "array", r.json().getNodeType());
                    Optional<JsonNode> item = r.json().values().stream().filter(n -> criada.equals(n.path("id").asString(""))).findFirst();
                    ctx.verificar("Solicitação criada está na lista", item.isPresent(), criada, item.isPresent() ? "encontrada" : "não encontrada");
                    item.ifPresent(n -> {
                        for (String campo : List.of("id", "solicitacao", "status", "dataCriacao", "dataAtualizacao",
                                "documentosEsperados", "documentosRecebidos", "arquivos")) {
                            ctx.verificar("Campo \"" + campo + "\" presente", n.has(campo), "presente", n.has(campo) ? "presente" : "ausente");
                        }
                        ctx.verificar("Número da solicitação correto", dados.numero().equals(n.path("solicitacao").asString("")),
                                dados.numero(), n.path("solicitacao").asString(""));
                    });
                });

        cenario("SOL-007", "Filtro de status inválido", "?status com valor que não existe.",
                "SOL_LISTAR", STATUS_HTTP, "BAIXA", 15, "Login da conta de teste.",
                "400 com a lista de \"Valores aceitos\"", (ctx, id) -> {
                    RespostaQa r = ctx.testar("SOL_LISTAR", Map.of(), Map.of("status", "XYZ"), null, ctx.token());
                    ctx.verificarStatus(r, 400);
                    ctx.verificarMensagemContem(r, "Valores aceitos");
                });

        cenario("SOL-008", "Consultar id inexistente", "UUID válido que não existe.",
                "SOL_CONSULTAR", STATUS_HTTP, "MEDIA", 15, "Login da conta de teste.",
                "404 com mensagem", (ctx, id) -> {
                    RespostaQa r = ctx.testar("SOL_CONSULTAR", Map.of("id", UUID.randomUUID().toString()), Map.of(), null, ctx.token());
                    ctx.verificarStatus(r, 404);
                    ctx.verificar("Tem mensagem de erro", !r.mensagem().isBlank(), "mensagem", r.mensagem());
                });

        cenario("SOL-009", "Consultar id malformado", "Identificador que não é UUID.",
                "SOL_CONSULTAR", STATUS_HTTP, "BAIXA", 15, "Login da conta de teste.",
                "400", (ctx, id) -> {
                    RespostaQa r = ctx.testar("SOL_CONSULTAR", Map.of("id", "nao-e-um-uuid"), Map.of(), null, ctx.token());
                    ctx.verificarStatus(r, 400);
                });

        cenario("SOL-010", "Contrato da consulta", "Consulta devolve status e histórico.",
                "SOL_CONSULTAR", CONTRATO, "ALTA", 20, "Uma solicitação criada pelo cenário.",
                "200 com id, status RECEBIDO e histórico com dataHora/status/descricao", (ctx, id) -> {
                    String criada = ctx.criarSolicitacao(ctx.novaSolicitacao(id, List.of("a.pdf")));
                    RespostaQa r = ctx.testar("SOL_CONSULTAR", Map.of("id", criada), Map.of(), null, ctx.token());
                    ctx.verificarStatus(r, 200);
                    JsonNode j = r.json();
                    ctx.verificar("id igual ao criado", criada.equals(j.path("id").asString("")), criada, j.path("id").asString(""));
                    ctx.verificar("Status inicial RECEBIDO", "RECEBIDO".equals(j.path("status").asString("")), "RECEBIDO", j.path("status").asString(""));
                    JsonNode evento = j.path("historico").path(0);
                    ctx.verificar("Histórico com o primeiro evento", evento.has("dataHora") && evento.has("status") && evento.has("descricao"),
                            "dataHora, status, descricao", evento);
                });

        cenario("SOL-011", "Cancelar solicitação", "Solicitação recém-criada pode ser cancelada.",
                "SOL_CANCELAR", INTEGRACAO, "ALTA", 20, "Uma solicitação em andamento criada pelo cenário.",
                "200 com status CANCELADO", (ctx, id) -> {
                    String criada = ctx.criarSolicitacao(ctx.novaSolicitacao(id, List.of("a.pdf")));
                    RespostaQa r = ctx.testar("SOL_CANCELAR", Map.of("id", criada), Map.of(), null, ctx.token());
                    ctx.verificarStatus(r, 200);
                    ctx.verificar("Status CANCELADO", "CANCELADO".equals(r.json().path("status").asString("")), "CANCELADO", r.json().path("status").asString(""));
                });

        cenario("SOL-012", "Cancelar duas vezes", "Solicitação já cancelada não pode ser cancelada de novo.",
                "SOL_CANCELAR", REGRAS_NEGOCIO, "MEDIA", 20, "Uma solicitação já cancelada.",
                "409", (ctx, id) -> {
                    String criada = ctx.criarSolicitacao(ctx.novaSolicitacao(id, List.of("a.pdf")));
                    RespostaQa primeiro = ctx.preparar("Preparação: primeiro cancelamento", "SOL_CANCELAR", Map.of("id", criada), null, ctx.token());
                    if (primeiro.status() != 200) throw new ContextoQa.PreCondicaoFalhou("O primeiro cancelamento falhou (HTTP " + primeiro.status() + ")");
                    RespostaQa r = ctx.testar("SOL_CANCELAR", Map.of("id", criada), Map.of(), null, ctx.token());
                    ctx.verificarStatus(r, 409);
                });
    }

    // ===================== Documentos =====================
    private void documentos() {
        cenario("DOC-001", "Upload de PDF válido", "Envia o PDF declarado quando a solicitação está aguardando documentos.",
                "DOC_ENVIAR", UPLOAD_PDF, "ALTA", 60, "Solicitação em AGUARDANDO_DOCUMENTO (≈ 10 a 15 s após a criação).",
                "200 \"Arquivo <nome> enviado\"", (ctx, id) -> {
                    DadosDeTeste.Solicitacao dados = ctx.novaSolicitacao(id, List.of("documento-qa.pdf", "segundo-qa.pdf"));
                    String criada = ctx.criarSolicitacao(dados);
                    ctx.exigirStatus(criada, AGUARDANDO, ATE_AGUARDANDO);
                    RespostaQa r = ctx.testarArquivo("DOC_ENVIAR", criada, "documento-qa.pdf", DadosDeTeste.pdf(dados.linhasPdf()), MediaType.APPLICATION_PDF);
                    ctx.verificarStatus(r, 200);
                    ctx.verificarMensagemContem(r, "Arquivo documento-qa.pdf enviado");
                });

        cenario("DOC-002", "Upload de arquivo que não é PDF", "Conteúdo de texto com nome .pdf.",
                "DOC_ENVIAR", DADOS_INVALIDOS, "ALTA", 60, "Solicitação em AGUARDANDO_DOCUMENTO.",
                "400 \"O arquivo enviado não é um PDF\"", (ctx, id) -> {
                    String criada = ctx.criarSolicitacao(ctx.novaSolicitacao(id, List.of("documento-qa.pdf")));
                    ctx.exigirStatus(criada, AGUARDANDO, ATE_AGUARDANDO);
                    byte[] texto = "isto não é um PDF".getBytes(StandardCharsets.UTF_8);
                    RespostaQa r = ctx.testarArquivo("DOC_ENVIAR", criada, "documento-qa.pdf", texto, MediaType.APPLICATION_PDF);
                    ctx.verificarStatus(r, 400);
                    ctx.verificarMensagemContem(r, "não é um PDF");
                });

        cenario("DOC-003", "Upload de arquivo não declarado", "Nome que não foi declarado na criação.",
                "DOC_ENVIAR", REGRAS_NEGOCIO, "MEDIA", 60, "Solicitação em AGUARDANDO_DOCUMENTO.",
                "422 com detalhe no campo nomeArquivo", (ctx, id) -> {
                    DadosDeTeste.Solicitacao dados = ctx.novaSolicitacao(id, List.of("documento-qa.pdf"));
                    String criada = ctx.criarSolicitacao(dados);
                    ctx.exigirStatus(criada, AGUARDANDO, ATE_AGUARDANDO);
                    RespostaQa r = ctx.testarArquivo("DOC_ENVIAR", criada, "nao-declarado.pdf", DadosDeTeste.pdf(dados.linhasPdf()), MediaType.APPLICATION_PDF);
                    ctx.verificarStatus(r, 422);
                    ctx.verificar("Detalhe do campo nomeArquivo", temCampo(r, "nomeArquivo"), "nomeArquivo", r.json().path("detalhes"));
                });

        cenario("DOC-004", "Upload repetido", "O mesmo arquivo declarado enviado duas vezes.",
                "DOC_ENVIAR", REGRAS_NEGOCIO, "MEDIA", 60, "Solicitação com 2 arquivos e o primeiro já enviado.",
                "409 \"já foi recebido\"", (ctx, id) -> {
                    DadosDeTeste.Solicitacao dados = ctx.novaSolicitacao(id, List.of("documento-qa.pdf", "segundo-qa.pdf"));
                    String criada = ctx.criarSolicitacao(dados);
                    ctx.exigirStatus(criada, AGUARDANDO, ATE_AGUARDANDO);
                    byte[] pdf = DadosDeTeste.pdf(dados.linhasPdf());
                    RespostaQa primeiro = ctx.prepararArquivo("Preparação: primeiro envio", criada, "documento-qa.pdf", pdf);
                    if (primeiro.status() != 200) throw new ContextoQa.PreCondicaoFalhou("O primeiro envio falhou (HTTP " + primeiro.status() + ")");
                    RespostaQa r = ctx.testarArquivo("DOC_ENVIAR", criada, "documento-qa.pdf", pdf, MediaType.APPLICATION_PDF);
                    ctx.verificarStatus(r, 409);
                    ctx.verificarMensagemContem(r, "já foi recebido");
                });

        cenario("DOC-005", "Upload antes de liberar", "Envio enquanto a solicitação ainda está RECEBIDO.",
                "DOC_ENVIAR", REGRAS_NEGOCIO, "MEDIA", 20, "Solicitação recém-criada (status RECEBIDO).",
                "409 informando o status RECEBIDO", (ctx, id) -> {
                    DadosDeTeste.Solicitacao dados = ctx.novaSolicitacao(id, List.of("documento-qa.pdf"));
                    String criada = ctx.criarSolicitacao(dados);
                    RespostaQa r = ctx.testarArquivo("DOC_ENVIAR", criada, "documento-qa.pdf", DadosDeTeste.pdf(dados.linhasPdf()), MediaType.APPLICATION_PDF);
                    ctx.verificarStatus(r, 409);
                    ctx.verificarMensagemContem(r, "RECEBIDO");
                });
    }

    // ===================== Análise (validação da empresa) =====================
    private void analise() {
        cenario("VAL-001", "Análise aprovada", "PDF com os mesmos dados da solicitação deve ser APROVADO pela análise automática.",
                "ANALISE_RESULTADO", VALIDACAO_EMPRESA, "ALTA", 150, "Solicitação aguardando documentos e PDF coerente (≈ 45 s pelo agendador).",
                "Status APROVADO e histórico \"todos os campos conferem\"", (ctx, id) -> {
                    DadosDeTeste.Solicitacao dados = ctx.novaSolicitacao(id, List.of("documento-qa.pdf"));
                    String criada = ctx.criarSolicitacao(dados);
                    ctx.exigirStatus(criada, AGUARDANDO, ATE_AGUARDANDO);
                    RespostaQa envio = ctx.prepararArquivo("Preparação: enviar PDF coerente", criada, "documento-qa.pdf", DadosDeTeste.pdf(dados.linhasPdf()));
                    if (envio.status() != 200) throw new ContextoQa.PreCondicaoFalhou("O envio do PDF falhou (HTTP " + envio.status() + ")");
                    ctx.esperarStatus(criada, FINAIS, ATE_RESULTADO);
                    RespostaQa r = ctx.testar("ANALISE_RESULTADO", Map.of("id", criada), Map.of(), null, ctx.token());
                    ctx.verificarStatus(r, 200);
                    ctx.verificar("Status APROVADO", "APROVADO".equals(r.json().path("status").asString("")), "APROVADO", r.json().path("status").asString(""));
                    ctx.verificar("Histórico registra a aprovação", historicoContem(r, "todos os campos conferem"),
                            "\"todos os campos conferem\"", ultimaDescricao(r));
                });

        cenario("VAL-002", "Análise reprovada por divergência", "PDF com CNPJ diferente do informado deve ser REPROVADO.",
                "ANALISE_RESULTADO", VALIDACAO_EMPRESA, "ALTA", 150, "Solicitação aguardando documentos e PDF com CNPJ divergente.",
                "Status REPROVADO e histórico citando o campo cnpj", (ctx, id) -> {
                    DadosDeTeste.Solicitacao dados = ctx.novaSolicitacao(id, List.of("documento-qa.pdf"));
                    String criada = ctx.criarSolicitacao(dados);
                    ctx.exigirStatus(criada, AGUARDANDO, ATE_AGUARDANDO);
                    byte[] pdfDivergente = DadosDeTeste.pdf(dados.comCnpj(DadosDeTeste.cnpjUnico()).linhasPdf());
                    RespostaQa envio = ctx.prepararArquivo("Preparação: enviar PDF com CNPJ divergente", criada, "documento-qa.pdf", pdfDivergente);
                    if (envio.status() != 200) throw new ContextoQa.PreCondicaoFalhou("O envio do PDF falhou (HTTP " + envio.status() + ")");
                    ctx.esperarStatus(criada, FINAIS, ATE_RESULTADO);
                    RespostaQa r = ctx.testar("ANALISE_RESULTADO", Map.of("id", criada), Map.of(), null, ctx.token());
                    ctx.verificarStatus(r, 200);
                    ctx.verificar("Status REPROVADO", "REPROVADO".equals(r.json().path("status").asString("")), "REPROVADO", r.json().path("status").asString(""));
                    ctx.verificar("Histórico cita o campo cnpj", historicoContem(r, "cnpj"), "cnpj", ultimaDescricao(r));
                });
    }

    // ===================== Utilitários =====================

    private static boolean temCampo(RespostaQa r, String campo) {
        return r.json().path("detalhes").values().stream().anyMatch(d -> campo.equals(d.path("campo").asString("")));
    }

    private static boolean historicoContem(RespostaQa r, String trecho) {
        return r.json().path("historico").values().stream().anyMatch(e -> e.path("descricao").asString("").contains(trecho));
    }

    private static String ultimaDescricao(RespostaQa r) {
        JsonNode historico = r.json().path("historico");
        return historico.size() == 0 ? "(sem histórico)" : historico.path(historico.size() - 1).path("descricao").asString("");
    }

    public List<CenarioQa> todos() {
        return List.copyOf(porId.values());
    }

    public Optional<CenarioQa> buscar(String id) {
        return Optional.ofNullable(porId.get(id));
    }
}
