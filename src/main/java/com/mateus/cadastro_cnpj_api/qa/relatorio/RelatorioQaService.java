package com.mateus.cadastro_cnpj_api.qa.relatorio;

import com.mateus.cadastro_cnpj_api.qa.QaException;
import com.mateus.cadastro_cnpj_api.qa.catalogo.EndpointCatalogo;
import com.mateus.cadastro_cnpj_api.qa.catalogo.EndpointQa;
import com.mateus.cadastro_cnpj_api.qa.cenarios.CenarioCatalogo;
import com.mateus.cadastro_cnpj_api.qa.modelo.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

// Report Service: indicadores, tendência, falhas agrupadas, comparação e CSV — tudo calculado do banco
@Service
public class RelatorioQaService {

    private final QaExecucaoRepository execucoes;
    private final QaResultadoRepository resultados;
    private final EndpointCatalogo endpoints;
    private final CenarioCatalogo cenarios;

    public RelatorioQaService(QaExecucaoRepository execucoes, QaResultadoRepository resultados, EndpointCatalogo endpoints,
                              CenarioCatalogo cenarios) {
        this.execucoes = execucoes;
        this.resultados = resultados;
        this.endpoints = endpoints;
        this.cenarios = cenarios;
    }

    // pulados = bloqueados (pré-condição falhou); cancelados = não rodaram porque a execução foi cancelada.
    // Cobertura: endpoints/cenários do catálogo que tiveram pelo menos um resultado decidido (aprovado, reprovado ou erro).
    public record Resumo(long execucoes, long emAndamento, long testes, long aprovados, long reprovados, long erros,
                         long pulados, long cancelados, Double taxaSucesso, Long tempoMedioMs,
                         long endpointsTestados, long endpointsTotal, Double coberturaEndpoints,
                         long cenariosTestados, long cenariosTotal, Map<String, Object> ultimaExecucao) {
    }

    public record PontoTendencia(String rotulo, LocalDateTime inicio, long execucoes, long aprovados, long reprovados,
                                 long erros, long pulados, long cancelados) {
    }

    public record GrupoFalhas(String chave, String nome, String detalhe, long total, long falhas) {
    }

    public record Falhas(List<GrupoFalhas> porEndpoint, List<GrupoFalhas> porCategoria) {
    }

    // regressao: passava em A e falhou (reprovado/erro) em B; correcao: falhava em A e passou em B
    public record LinhaComparacao(String cenarioId, String cenarioNome, StatusTeste statusA, StatusTeste statusB,
                                  boolean mudou, boolean regressao, boolean correcao) {
    }

    public record Comparacao(UUID a, UUID b, List<LinhaComparacao> linhas, long mudancas, long regressoes, long correcoes) {
    }

    // ===== Resumo =====

    public Resumo resumo(LocalDate de, LocalDate ate) {
        List<QaExecucao> lista = noPeriodo(de, ate);
        Set<UUID> ids = lista.stream().map(QaExecucao::getId).collect(Collectors.toSet());
        List<QaResultado> res = resultados.findAll().stream().filter(r -> ids.contains(r.getExecucaoId())).toList();
        long aprovados = contar(res, StatusTeste.PASSED);
        long reprovados = contar(res, StatusTeste.FAILED);
        long erros = contar(res, StatusTeste.ERROR);
        long pulados = contar(res, StatusTeste.SKIPPED);
        long cancelados = contar(res, StatusTeste.CANCELLED);
        long decididos = aprovados + reprovados + erros;
        Set<StatusTeste> decisivos = Set.of(StatusTeste.PASSED, StatusTeste.FAILED, StatusTeste.ERROR);
        long endpointsTotal = endpoints.todos().size();
        long endpointsTestados = res.stream().filter(r -> decisivos.contains(r.getStatus()))
                .map(QaResultado::getEndpointId).filter(id -> endpoints.buscar(id).isPresent()).distinct().count();
        long cenariosTotal = cenarios.todos().size();
        long cenariosTestados = res.stream().filter(r -> decisivos.contains(r.getStatus()))
                .map(QaResultado::getCenarioId).filter(id -> cenarios.buscar(id).isPresent()).distinct().count();
        OptionalDouble media = lista.stream().filter(e -> e.getDuracaoMs() != null && e.getStatus() == StatusExecucao.COMPLETED)
                .mapToLong(QaExecucao::getDuracaoMs).average();

        Map<String, Object> ultima = null;
        if (!lista.isEmpty()) {
            QaExecucao u = lista.get(0);
            ultima = new LinkedHashMap<>();
            ultima.put("id", u.getId());
            ultima.put("status", u.getStatus());
            ultima.put("criadaEm", u.getCriadaEm());
            ultima.put("usuario", u.getUsuario());
            ultima.put("ambiente", u.getAmbiente().rotulo());
            ultima.put("aprovados", u.getAprovados());
            ultima.put("total", u.getTotal());
        }
        return new Resumo(lista.size(), lista.stream().filter(e -> e.getStatus().ativo()).count(), res.size(),
                aprovados, reprovados, erros, pulados, cancelados,
                decididos == 0 ? null : Math.round(aprovados * 1000.0 / decididos) / 10.0,
                media.isPresent() ? Math.round(media.getAsDouble()) : null,
                endpointsTestados, endpointsTotal,
                endpointsTotal == 0 ? null : Math.round(endpointsTestados * 1000.0 / endpointsTotal) / 10.0,
                cenariosTestados, cenariosTotal, ultima);
    }

    // ===== Tendência (por dia nos últimos N dias, ou por hora nas últimas 24 h) =====

    public List<PontoTendencia> tendencias(String agrupamento, int quantidade) {
        boolean porHora = "hora".equalsIgnoreCase(agrupamento);
        int n = Math.max(1, Math.min(quantidade, porHora ? 48 : 90));
        ChronoUnit unidade = porHora ? ChronoUnit.HOURS : ChronoUnit.DAYS;
        LocalDateTime fim = LocalDateTime.now().truncatedTo(unidade).plus(1, unidade);
        LocalDateTime inicio = fim.minus(n, unidade);

        List<QaExecucao> lista = execucoes.findAllByOrderByCriadaEmDesc().stream()
                .filter(e -> !e.getCriadaEm().isBefore(inicio)).toList();
        List<PontoTendencia> pontos = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            LocalDateTime a = inicio.plus(i, unidade);
            LocalDateTime b = a.plus(1, unidade);
            List<QaExecucao> doBalde = lista.stream().filter(e -> !e.getCriadaEm().isBefore(a) && e.getCriadaEm().isBefore(b)).toList();
            String rotulo = porHora ? String.format("%02dh", a.getHour()) : String.format("%02d/%02d", a.getDayOfMonth(), a.getMonthValue());
            pontos.add(new PontoTendencia(rotulo, a, doBalde.size(),
                    doBalde.stream().mapToLong(QaExecucao::getAprovados).sum(),
                    doBalde.stream().mapToLong(QaExecucao::getReprovados).sum(),
                    doBalde.stream().mapToLong(QaExecucao::getErros).sum(),
                    doBalde.stream().mapToLong(QaExecucao::getPulados).sum(),
                    doBalde.stream().mapToLong(QaExecucao::getCancelados).sum()));
        }
        return pontos;
    }

    // ===== Falhas agrupadas (FAILED + ERROR) =====

    public Falhas falhas(LocalDate de, LocalDate ate) {
        Set<UUID> ids = noPeriodo(de, ate).stream().map(QaExecucao::getId).collect(Collectors.toSet());
        List<QaResultado> res = resultados.findAll().stream()
                .filter(r -> ids.contains(r.getExecucaoId()) && r.getStatus().terminado()).toList();

        List<GrupoFalhas> porEndpoint = res.stream().collect(Collectors.groupingBy(QaResultado::getEndpointId)).entrySet().stream()
                .map(e -> {
                    EndpointQa ep = endpoints.buscar(e.getKey()).orElse(null);
                    return new GrupoFalhas(e.getKey(), ep != null ? ep.nome() : e.getKey(),
                            ep != null ? ep.metodo() + " " + ep.caminho() : "", e.getValue().size(), falhas(e.getValue()));
                })
                .sorted(Comparator.comparingLong(GrupoFalhas::falhas).reversed().thenComparing(GrupoFalhas::nome))
                .toList();

        List<GrupoFalhas> porCategoria = res.stream().collect(Collectors.groupingBy(QaResultado::getCategoria)).entrySet().stream()
                .map(e -> new GrupoFalhas(e.getKey().name(), e.getKey().rotulo(), "", e.getValue().size(), falhas(e.getValue())))
                .sorted(Comparator.comparingLong(GrupoFalhas::falhas).reversed().thenComparing(GrupoFalhas::nome))
                .toList();
        return new Falhas(porEndpoint, porCategoria);
    }

    // ===== Comparação entre duas execuções (cenário a cenário) =====

    public Comparacao comparar(UUID a, UUID b) {
        if (execucoes.findById(a).isEmpty() || execucoes.findById(b).isEmpty()) {
            throw new QaException(HttpStatus.NOT_FOUND, "Execução não encontrada para comparar");
        }
        Map<String, QaResultado> ra = porCenario(a);
        Map<String, QaResultado> rb = porCenario(b);
        Set<String> todos = new TreeSet<>(ra.keySet());
        todos.addAll(rb.keySet());
        List<LinhaComparacao> linhas = todos.stream().map(id -> {
            QaResultado x = ra.get(id);
            QaResultado y = rb.get(id);
            StatusTeste sa = x != null ? x.getStatus() : null;
            StatusTeste sb = y != null ? y.getStatus() : null;
            String nome = x != null ? x.getCenarioNome() : y.getCenarioNome();
            boolean falhaB = sb == StatusTeste.FAILED || sb == StatusTeste.ERROR;
            boolean falhaA = sa == StatusTeste.FAILED || sa == StatusTeste.ERROR;
            return new LinhaComparacao(id, nome, sa, sb, sa != sb,
                    sa == StatusTeste.PASSED && falhaB, falhaA && sb == StatusTeste.PASSED);
        }).toList();
        return new Comparacao(a, b, linhas, linhas.stream().filter(LinhaComparacao::mudou).count(),
                linhas.stream().filter(LinhaComparacao::regressao).count(),
                linhas.stream().filter(LinhaComparacao::correcao).count());
    }

    // ===== CSV (uma linha por resultado; de uma execução ou do período) =====

    public String csv(UUID execucaoId, LocalDate de, LocalDate ate) {
        List<QaExecucao> lista = execucaoId != null
                ? execucoes.findById(execucaoId).map(List::of).orElseThrow(() -> new QaException(HttpStatus.NOT_FOUND, "Execução não encontrada"))
                : noPeriodo(de, ate);
        Map<UUID, QaExecucao> porId = lista.stream().collect(Collectors.toMap(QaExecucao::getId, e -> e));
        StringBuilder sb = new StringBuilder("﻿");   // BOM: o Excel abre os acentos corretamente
        sb.append("execucao;criada_em;usuario;ambiente;suite;cenario;nome;endpoint;metodo;caminho;categoria;status;http;duracao_ms;mensagem\n");
        resultados.findAll().stream()
                .filter(r -> porId.containsKey(r.getExecucaoId()))
                .sorted(Comparator.comparing(QaResultado::getId))
                .forEach(r -> {
                    QaExecucao e = porId.get(r.getExecucaoId());
                    EndpointQa ep = endpoints.buscar(r.getEndpointId()).orElse(null);
                    sb.append(String.join(";",
                            e.getId().toString(), e.getCriadaEm().toString(), celula(e.getUsuario()), e.getAmbiente().rotulo(), celula(e.getSuiteNome()),
                            r.getCenarioId(), celula(r.getCenarioNome()), r.getEndpointId(),
                            ep != null ? ep.metodo() : "", ep != null ? ep.caminho() : "", r.getCategoria().rotulo(),
                            r.getStatus().name(), r.getHttpStatus() != null ? r.getHttpStatus().toString() : "",
                            r.getDuracaoMs() != null ? r.getDuracaoMs().toString() : "", celula(r.getMensagem()))).append('\n');
                });
        return sb.toString();
    }

    // ===== Utilitários =====

    private List<QaExecucao> noPeriodo(LocalDate de, LocalDate ate) {
        return execucoes.findAllByOrderByCriadaEmDesc().stream()
                .filter(e -> de == null || !e.getCriadaEm().toLocalDate().isBefore(de))
                .filter(e -> ate == null || !e.getCriadaEm().toLocalDate().isAfter(ate))
                .toList();
    }

    private Map<String, QaResultado> porCenario(UUID execucaoId) {
        Map<String, QaResultado> m = new LinkedHashMap<>();
        resultados.findByExecucaoIdOrderByIdAsc(execucaoId).forEach(r -> m.put(r.getCenarioId(), r));
        return m;
    }

    private static long contar(List<QaResultado> lista, StatusTeste status) {
        return lista.stream().filter(r -> r.getStatus() == status).count();
    }

    private static long falhas(List<QaResultado> lista) {
        return lista.stream().filter(r -> r.getStatus() == StatusTeste.FAILED || r.getStatus() == StatusTeste.ERROR).count();
    }

    // Célula CSV segura: sem quebras de linha nem ";" e sem fórmulas (= + - @ no início viram texto)
    private static String celula(String valor) {
        if (valor == null) return "";
        String v = valor.replace(';', ',').replace('\n', ' ').replace('\r', ' ');
        return v.matches("^[=+\\-@].*") ? "'" + v : v;
    }
}
