package com.mateus.cadastro_cnpj_api.qa.catalogo;

import com.mateus.cadastro_cnpj_api.qa.modelo.Ambiente;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Endpoint Registry: o projeto não tem Swagger/OpenAPI, então o catálogo é explícito e espelha
// os controllers reais (AuthController e SolicitacaoController). Mudou a API? Atualize aqui.
@Component
public class EndpointCatalogo {

    private static final EnumSet<Ambiente> TODOS = EnumSet.allOf(Ambiente.class);

    private final Map<String, EndpointQa> porId = new LinkedHashMap<>();

    public EndpointCatalogo() {
        registrar(new EndpointQa("AUTH_TOKEN", "Gerar token", "POST", "/auth/token", "Autenticação",
                "Login com e-mail e senha; devolve o JWT usado nas demais rotas.", false, TODOS));

        registrar(new EndpointQa("SOL_CRIAR", "Criar solicitação", "POST", "/solicitacoes", "Solicitações",
                "Cadastra a solicitação com os dados da pessoa e da empresa e os PDFs que serão enviados.", true, TODOS));
        registrar(new EndpointQa("SOL_LISTAR", "Listar solicitações", "GET", "/solicitacoes", "Solicitações",
                "Lista todas as solicitações (filtro opcional ?status=).", false, TODOS));
        registrar(new EndpointQa("SOL_CONSULTAR", "Consultar solicitação", "GET", "/solicitacoes/{id}", "Solicitações",
                "Devolve o status e o histórico de uma solicitação.", false, TODOS));
        registrar(new EndpointQa("SOL_CANCELAR", "Cancelar solicitação", "PATCH", "/solicitacoes/{id}/cancelamento", "Solicitações",
                "Cancela uma solicitação ainda em andamento.", true, TODOS));

        registrar(new EndpointQa("DOC_ENVIAR", "Enviar documento", "POST", "/solicitacoes/{id}/documento", "Documentos",
                "Envia um PDF declarado na criação (form-data, campo \"arquivo\").", true, TODOS));

        registrar(new EndpointQa("ANALISE_RESULTADO", "Resultado da análise", "GET", "/solicitacoes/{id}", "Análise",
                "Mesmo endpoint de consulta, verificando o resultado da análise automática dos PDFs (APROVADO/REPROVADO).", false, TODOS));
    }

    private void registrar(EndpointQa endpoint) {
        porId.put(endpoint.id(), endpoint);
    }

    public List<EndpointQa> todos() {
        return List.copyOf(porId.values());
    }

    public Optional<EndpointQa> buscar(String id) {
        return Optional.ofNullable(porId.get(id));
    }
}
