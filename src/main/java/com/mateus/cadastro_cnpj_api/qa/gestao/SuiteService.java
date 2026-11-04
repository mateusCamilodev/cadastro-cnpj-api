package com.mateus.cadastro_cnpj_api.qa.gestao;

import com.mateus.cadastro_cnpj_api.qa.QaException;
import com.mateus.cadastro_cnpj_api.qa.api.QaDtos;
import com.mateus.cadastro_cnpj_api.qa.api.QaMapeador;
import com.mateus.cadastro_cnpj_api.qa.cenarios.CenarioCatalogo;
import com.mateus.cadastro_cnpj_api.qa.modelo.QaExecucao;
import com.mateus.cadastro_cnpj_api.qa.modelo.QaExecucaoRepository;
import com.mateus.cadastro_cnpj_api.qa.modelo.QaSuite;
import com.mateus.cadastro_cnpj_api.qa.modelo.QaSuiteRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

// Test Suites: coleções reutilizáveis de cenários.
// As suítes padrão são criadas pelo código ao subir a aplicação (só com cenários que existem no catálogo)
// e não podem ser alteradas nem excluídas. As demais são criadas pela tela (permissão qa.configure).
@Service
public class SuiteService {

    private static final String SISTEMA = "sistema";
    private static final int MAX_CENARIOS = 100;

    private final QaSuiteRepository suites;
    private final QaExecucaoRepository execucoes;
    private final CenarioCatalogo cenarios;
    private final QaMapeador mapeador;
    private final AuditoriaQa auditoria;

    public SuiteService(QaSuiteRepository suites, QaExecucaoRepository execucoes, CenarioCatalogo cenarios,
                        QaMapeador mapeador, AuditoriaQa auditoria) {
        this.suites = suites;
        this.execucoes = execucoes;
        this.cenarios = cenarios;
        this.mapeador = mapeador;
        this.auditoria = auditoria;
    }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void criarSuitesPadrao() {
        if (suites.existsByPadraoTrue()) return;
        padrao("Autenticação", "Login válido e inválido, campos obrigatórios, acesso sem token, token adulterado e token expirado.",
                "AUTH-001", "AUTH-002", "AUTH-003", "AUTH-004", "AUTH-005", "AUTH-006");
        padrao("Solicitações", "Criação, validações de campos, duplicidade, listagem, consulta e cancelamento de solicitações.",
                "SOL-001", "SOL-002", "SOL-003", "SOL-004", "SOL-005", "SOL-006", "SOL-007", "SOL-008", "SOL-009",
                "SOL-010", "SOL-011", "SOL-012");
        padrao("Documentos", "Upload de PDF válido, formato não permitido, arquivo não declarado, envio repetido e envio antes da hora.",
                "DOC-001", "DOC-002", "DOC-003", "DOC-004", "DOC-005");
        padrao("Fluxo completo", "Cadastro, envio dos PDFs, processamento, análise automática e consulta do resultado (aprovado e reprovado). Leva cerca de 1 minuto.",
                "SOL-001", "DOC-001", "VAL-001", "VAL-002");
        padrao("Smoke (rápida)", "Verificação rápida dos caminhos principais da API, em poucos segundos.",
                "AUTH-001", "AUTH-004", "SOL-001", "SOL-006", "SOL-008", "SOL-010");
    }

    private void padrao(String nome, String descricao, String... ids) {
        List<String> existentes = new ArrayList<>();
        for (String id : ids) {
            if (cenarios.buscar(id).isPresent()) existentes.add(id);
        }
        if (!existentes.isEmpty()) {
            suites.save(new QaSuite(nome, descricao, mapeador.json(existentes), true, SISTEMA));
        }
    }

    // ===== Consultas =====

    public List<QaDtos.SuiteDto> listar() {
        List<QaExecucao> todas = execucoes.findAllByOrderByCriadaEmDesc();
        return suites.findAllByOrderByPadraoDescNomeAsc().stream().map(s -> dto(s, todas)).toList();
    }

    public QaDtos.SuiteDto detalhe(UUID id) {
        return dto(buscar(id), execucoes.findAllByOrderByCriadaEmDesc());
    }

    public QaSuite buscar(UUID id) {
        return suites.findById(id).orElseThrow(() -> new QaException(HttpStatus.NOT_FOUND, "Test Suite não encontrada"));
    }

    public List<String> cenarios(QaSuite suite) {
        return mapeador.cenariosDaSuite(suite);
    }

    // ===== Alterações (qa.configure) =====

    public synchronized QaDtos.SuiteDto criar(QaDtos.SuiteRequest pedido, String usuario) {
        String nome = validarNome(pedido);
        if (suites.existsByNomeIgnoreCase(nome)) throw new QaException(HttpStatus.CONFLICT, "Já existe uma Test Suite com este nome");
        List<String> ids = validarCenarios(pedido.cenarioIds());
        QaSuite s = suites.save(new QaSuite(nome, descricao(pedido), mapeador.json(ids), false, usuario));
        auditoria.registrar(usuario, AuditoriaQa.SUITE_CRIADA, s.getId().toString(), nome + " (" + ids.size() + " cenário(s))");
        return detalhe(s.getId());
    }

    public synchronized QaDtos.SuiteDto atualizar(UUID id, QaDtos.SuiteRequest pedido, String usuario) {
        QaSuite s = editavel(id);
        String nome = validarNome(pedido);
        if (suites.existsByNomeIgnoreCaseAndIdNot(nome, id)) throw new QaException(HttpStatus.CONFLICT, "Já existe uma Test Suite com este nome");
        List<String> ids = validarCenarios(pedido.cenarioIds());
        s.alterar(nome, descricao(pedido), mapeador.json(ids), usuario);
        suites.save(s);
        auditoria.registrar(usuario, AuditoriaQa.SUITE_ALTERADA, id.toString(), nome + " (" + ids.size() + " cenário(s))");
        return detalhe(id);
    }

    public synchronized void excluir(UUID id, String usuario) {
        QaSuite s = editavel(id);
        suites.delete(s);
        // As execuções antigas continuam com o nome da suíte gravado (o histórico não se perde)
        auditoria.registrar(usuario, AuditoriaQa.SUITE_EXCLUIDA, id.toString(), s.getNome());
    }

    // ===== Internos =====

    private QaSuite editavel(UUID id) {
        QaSuite s = buscar(id);
        if (s.isPadrao()) throw new QaException(HttpStatus.CONFLICT, "Suítes padrão não podem ser alteradas nem excluídas. Crie uma nova a partir dela.");
        return s;
    }

    private static String validarNome(QaDtos.SuiteRequest pedido) {
        if (pedido == null || pedido.nome() == null || pedido.nome().isBlank()) {
            throw new QaException(HttpStatus.BAD_REQUEST, "Informe o nome da Test Suite");
        }
        String nome = pedido.nome().trim();
        if (nome.length() < 3 || nome.length() > 120) {
            throw new QaException(HttpStatus.BAD_REQUEST, "O nome da Test Suite deve ter entre 3 e 120 caracteres");
        }
        return nome;
    }

    private static String descricao(QaDtos.SuiteRequest pedido) {
        if (pedido.descricao() == null || pedido.descricao().isBlank()) return null;
        String d = pedido.descricao().trim();
        if (d.length() > 500) throw new QaException(HttpStatus.BAD_REQUEST, "A descrição deve ter no máximo 500 caracteres");
        return d;
    }

    private List<String> validarCenarios(List<String> ids) {
        if (ids == null || ids.isEmpty()) throw new QaException(HttpStatus.BAD_REQUEST, "Selecione pelo menos um cenário");
        List<String> unicos = new ArrayList<>(new LinkedHashSet<>(ids));
        if (unicos.size() > MAX_CENARIOS) throw new QaException(HttpStatus.BAD_REQUEST, "Uma Test Suite aceita no máximo " + MAX_CENARIOS + " cenários");
        for (String id : unicos) {
            if (cenarios.buscar(id).isEmpty()) throw new QaException(HttpStatus.BAD_REQUEST, "Cenário desconhecido: " + id);
        }
        return unicos;
    }

    private QaDtos.SuiteDto dto(QaSuite s, List<QaExecucao> todas) {
        List<QaExecucao> daSuite = todas.stream().filter(e -> s.getId().equals(e.getSuiteId())).toList();
        QaExecucao ultima = daSuite.isEmpty() ? null : daSuite.get(0);
        return new QaDtos.SuiteDto(s.getId(), s.getNome(), s.getDescricao(), mapeador.cenariosDaSuite(s), s.isPadrao(),
                s.getCriadaPor(), s.getCriadaEm(), s.getAtualizadaPor(), s.getAtualizadaEm(), daSuite.size(),
                ultima != null ? ultima.getStatus() : null, ultima != null ? ultima.getCriadaEm() : null);
    }
}
