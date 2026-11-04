package com.mateus.cadastro_cnpj_api.qa.api;

import com.mateus.cadastro_cnpj_api.qa.modelo.Ambiente;
import com.mateus.cadastro_cnpj_api.qa.modelo.Categoria;
import com.mateus.cadastro_cnpj_api.qa.modelo.StatusExecucao;
import com.mateus.cadastro_cnpj_api.qa.modelo.StatusTeste;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// Contratos (entrada e saída) da API do QA Center
public final class QaDtos {

    private QaDtos() {
    }

    // ===== Entrada =====

    // Seleção da execução — uma destas formas:
    //  - suiteId (uma Test Suite);
    //  - cenarioIds (cenários escolhidos um a um);
    //  - endpointIds + categorias.
    // timeoutRequisicaoSegundos é opcional (limitado ao máximo permitido; sem ele vale a configuração).
    public record NovaExecucaoRequest(Ambiente ambiente, List<String> endpointIds, List<Categoria> categorias,
                                      List<String> cenarioIds, UUID suiteId, Integer timeoutRequisicaoSegundos) {
    }

    // O que fica salvo na execução para permitir "reexecutar"
    public record Configuracao(List<String> endpointIds, List<Categoria> categorias, List<String> cenarioIds,
                               UUID suiteId, String suiteNome, Integer timeoutRequisicaoSegundos) {
    }

    public record SuiteRequest(String nome, String descricao, List<String> cenarioIds) {
    }

    public record ConfiguracaoRequest(Integer timeoutRequisicaoSegundos, Integer timeoutExecucaoMinutos,
                                      Integer cenariosParalelos, Integer execucoesPorUsuario) {
    }

    // ===== Catálogo =====

    public record EndpointDto(String id, String nome, String metodo, String caminho, String grupo, String descricao,
                              boolean modificaDados, List<Ambiente> ambientes, List<Categoria> categorias,
                              int cenarios, long execucoes, StatusTeste ultimoResultado, LocalDateTime ultimoEm) {
    }

    public record CenarioDto(String id, String nome, String descricao, String endpointId, String endpointNome,
                             String metodo, String caminho, Categoria categoria, String categoriaRotulo,
                             String criticidade, int timeoutSegundos, String preCondicoes, String esperado,
                             StatusTeste ultimoResultado, LocalDateTime ultimoEm) {
    }

    public record CategoriaDto(Categoria id, String rotulo, int cenarios) {
    }

    public record AmbienteDto(Ambiente id, String rotulo, boolean disponivel) {
    }

    public record SuiteDto(UUID id, String nome, String descricao, List<String> cenarioIds, boolean padrao,
                           String criadaPor, LocalDateTime criadaEm, String atualizadaPor, LocalDateTime atualizadaEm,
                           long execucoes, StatusExecucao ultimoStatus, LocalDateTime ultimaEm) {
    }

    // ===== Configurações e auditoria =====

    public record ConfiguracaoDto(int timeoutRequisicaoSegundos, int timeoutExecucaoMinutos, int cenariosParalelos,
                                  int execucoesPorUsuario, Limites limites, String atualizadaPor,
                                  LocalDateTime atualizadaEm) {
    }

    // Faixas aceitas na tela (o backend recusa fora delas)
    public record Limites(int timeoutRequisicaoMax, int timeoutExecucaoMax, int cenariosParalelosMax,
                          int execucoesPorUsuarioMax, int execucoesSimultaneas) {
    }

    public record AuditoriaDto(Long id, String usuario, String acao, String alvo, String detalhe, LocalDateTime dataHora) {
    }

    // ===== Execuções =====

    public record ExecucaoDto(UUID id, String usuario, Ambiente ambiente, String ambienteRotulo, StatusExecucao status,
                              int total, int aprovados, int reprovados, int erros, int pulados, int cancelados,
                              int concluidos, LocalDateTime criadaEm, LocalDateTime iniciadaEm,
                              LocalDateTime finalizadaEm, Long duracaoMs, String mensagem, String canceladaPor,
                              UUID origemId, UUID suiteId, String suiteNome, Configuracao configuracao) {
    }

    public record ResultadoDto(Long id, String cenarioId, String cenarioNome, String endpointId, String metodo,
                               String caminho, Categoria categoria, StatusTeste status, Integer httpStatus,
                               Long duracaoMs, String mensagem, LocalDateTime iniciadoEm, LocalDateTime finalizadoEm,
                               Object evidencias) {
    }

    public record EventoDto(String tipo, String mensagem, LocalDateTime dataHora) {
    }

    // Tudo o que a tela de execução precisa (snapshot inicial do SSE e GET /qa/execucoes/{id})
    public record ExecucaoDetalheDto(ExecucaoDto execucao, List<ResultadoDto> resultados, List<EventoDto> eventos) {
    }

    public record Pagina<T>(List<T> itens, long total, int pagina, int tamanho) {
    }
}
