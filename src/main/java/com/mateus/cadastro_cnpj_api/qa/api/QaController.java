package com.mateus.cadastro_cnpj_api.qa.api;

import com.mateus.cadastro_cnpj_api.qa.execucao.EventosExecucao;
import com.mateus.cadastro_cnpj_api.qa.gestao.AuditoriaQa;
import com.mateus.cadastro_cnpj_api.qa.gestao.ConfiguracaoQaService;
import com.mateus.cadastro_cnpj_api.qa.gestao.SuiteService;
import com.mateus.cadastro_cnpj_api.qa.execucao.ExecucaoService;
import com.mateus.cadastro_cnpj_api.qa.modelo.Ambiente;
import com.mateus.cadastro_cnpj_api.qa.modelo.StatusExecucao;
import com.mateus.cadastro_cnpj_api.qa.relatorio.RelatorioQaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// API do QA Center. As permissões (qa.view, qa.execute, qa.cancel, qa.reports.view, qa.configure) são exigidas no
// SecurityConfig; o usuário de cada operação vem do token (Authentication), nunca do corpo da requisição.
@RestController
@RequestMapping("/qa")
public class QaController {

    private final ConsultaQaService consulta;
    private final ExecucaoService execucoes;
    private final RelatorioQaService relatorios;
    private final EventosExecucao tempoReal;
    private final SuiteService suites;
    private final ConfiguracaoQaService configuracoes;
    private final AuditoriaQa auditoria;

    public QaController(ConsultaQaService consulta, ExecucaoService execucoes, RelatorioQaService relatorios,
                        EventosExecucao tempoReal, SuiteService suites, ConfiguracaoQaService configuracoes,
                        AuditoriaQa auditoria) {
        this.suites = suites;
        this.configuracoes = configuracoes;
        this.auditoria = auditoria;
        this.consulta = consulta;
        this.execucoes = execucoes;
        this.relatorios = relatorios;
        this.tempoReal = tempoReal;
    }

    // ===== Catálogo =====

    @GetMapping("/endpoints")
    public List<QaDtos.EndpointDto> endpoints() {
        return consulta.endpoints();
    }

    @GetMapping("/endpoints/{id}")
    public QaDtos.EndpointDto endpoint(@PathVariable String id) {
        return consulta.endpoint(id);
    }

    @GetMapping("/cenarios")
    public List<QaDtos.CenarioDto> cenarios() {
        return consulta.cenarios();
    }

    @GetMapping("/cenarios/{id}")
    public QaDtos.CenarioDto cenario(@PathVariable String id) {
        return consulta.cenario(id);
    }

    @GetMapping("/categorias")
    public List<QaDtos.CategoriaDto> categorias() {
        return consulta.categorias();
    }

    @GetMapping("/ambientes")
    public List<QaDtos.AmbienteDto> ambientes() {
        return consulta.ambientes();
    }

    // ===== Test Suites (consulta: qa.view; criar/alterar/excluir: qa.configure) =====

    @GetMapping("/suites")
    public List<QaDtos.SuiteDto> suites() {
        return suites.listar();
    }

    @GetMapping("/suites/{id}")
    public QaDtos.SuiteDto suite(@PathVariable UUID id) {
        return suites.detalhe(id);
    }

    @PostMapping("/suites")
    public ResponseEntity<QaDtos.SuiteDto> criarSuite(@RequestBody QaDtos.SuiteRequest pedido, Authentication quem) {
        return ResponseEntity.status(HttpStatus.CREATED).body(suites.criar(pedido, quem.getName()));
    }

    @PutMapping("/suites/{id}")
    public QaDtos.SuiteDto alterarSuite(@PathVariable UUID id, @RequestBody QaDtos.SuiteRequest pedido, Authentication quem) {
        return suites.atualizar(id, pedido, quem.getName());
    }

    @DeleteMapping("/suites/{id}")
    public ResponseEntity<Void> excluirSuite(@PathVariable UUID id, Authentication quem) {
        suites.excluir(id, quem.getName());
        return ResponseEntity.noContent().build();
    }

    // ===== Configurações (consulta: qa.view; alterar e auditoria: qa.configure) =====

    @GetMapping("/configuracoes")
    public QaDtos.ConfiguracaoDto configuracoes() {
        return configuracoes.atual();
    }

    @PutMapping("/configuracoes")
    public QaDtos.ConfiguracaoDto alterarConfiguracoes(@RequestBody QaDtos.ConfiguracaoRequest pedido, Authentication quem) {
        return configuracoes.atualizar(pedido, quem.getName());
    }

    @GetMapping("/auditoria")
    public List<QaDtos.AuditoriaDto> auditoria(@RequestParam(defaultValue = "100") int quantidade) {
        return auditoria.recentes(quantidade);
    }

    // ===== Execuções =====

    @PostMapping("/execucoes")
    public ResponseEntity<QaDtos.ExecucaoDto> criar(@RequestBody QaDtos.NovaExecucaoRequest pedido, Authentication quem) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(execucoes.criar(pedido, quem.getName()));   // 202: roda em segundo plano
    }

    @GetMapping("/execucoes")
    public QaDtos.Pagina<QaDtos.ExecucaoDto> listar(
            @RequestParam(required = false) StatusExecucao status,
            @RequestParam(required = false) Ambiente ambiente,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String endpoint,
            @RequestParam(required = false) UUID suite,
            @RequestParam(required = false) String resultado,
            @RequestParam(defaultValue = "false") boolean ativas,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return consulta.execucoes(status, ambiente, usuario, de, ate, busca, endpoint, suite, resultado, ativas, pagina, tamanho);
    }

    @GetMapping("/execucoes/{id}")
    public QaDtos.ExecucaoDetalheDto detalhe(@PathVariable UUID id) {
        return consulta.detalhe(id);
    }

    @GetMapping("/execucoes/{id}/resultados")
    public List<QaDtos.ResultadoDto> resultados(@PathVariable UUID id) {
        return consulta.resultados(id);
    }

    @PostMapping("/execucoes/{id}/cancelar")
    public QaDtos.ExecucaoDto cancelar(@PathVariable UUID id, Authentication quem) {
        return execucoes.cancelar(id, quem.getName());
    }

    @PostMapping("/execucoes/{id}/reexecutar")
    public ResponseEntity<QaDtos.ExecucaoDto> reexecutar(@PathVariable UUID id, Authentication quem) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(execucoes.reexecutar(id, quem.getName()));
    }

    // Tempo real (SSE): snapshot completo ao conectar + um evento a cada mudança
    @GetMapping(value = "/execucoes/{id}/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter eventos(@PathVariable UUID id) {
        QaDtos.ExecucaoDetalheDto atual = consulta.detalhe(id);   // 404 se não existir
        return tempoReal.inscrever(id, () -> consulta.detalhe(id), !consulta.ativa(id) || !atual.execucao().status().ativo());
    }

    // ===== Relatórios =====

    @GetMapping("/relatorios/resumo")
    public RelatorioQaService.Resumo resumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return relatorios.resumo(de, ate);
    }

    @GetMapping("/relatorios/tendencias")
    public List<RelatorioQaService.PontoTendencia> tendencias(@RequestParam(defaultValue = "dia") String agrupamento,
                                                               @RequestParam(defaultValue = "14") int quantidade) {
        return relatorios.tendencias(agrupamento, quantidade);
    }

    @GetMapping("/relatorios/falhas")
    public RelatorioQaService.Falhas falhas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return relatorios.falhas(de, ate);
    }

    @GetMapping("/relatorios/comparacao")
    public RelatorioQaService.Comparacao comparacao(@RequestParam UUID a, @RequestParam UUID b) {
        return relatorios.comparar(a, b);
    }

    @GetMapping("/relatorios/exportar.csv")
    public ResponseEntity<byte[]> csv(@RequestParam(required = false) UUID execucao,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        String nome = execucao != null ? "qa-execucao-" + execucao.toString().substring(0, 8) + ".csv" : "qa-relatorio.csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nome + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(relatorios.csv(execucao, de, ate).getBytes(StandardCharsets.UTF_8));
    }
}
