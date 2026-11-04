package com.mateus.cadastro_cnpj_api.qa.gestao;

import com.mateus.cadastro_cnpj_api.qa.QaException;
import com.mateus.cadastro_cnpj_api.qa.api.QaDtos;
import com.mateus.cadastro_cnpj_api.qa.config.QaProperties;
import com.mateus.cadastro_cnpj_api.qa.modelo.QaConfiguracao;
import com.mateus.cadastro_cnpj_api.qa.modelo.QaConfiguracaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

// Configurações do executor (tela "Configurações", permissão qa.configure).
// Enquanto ninguém alterar, valem os valores do application.properties (qa.limites.*).
// Os máximos são fixos no código: pela tela não dá, por exemplo, para tirar o tempo limite das requisições.
@Service
public class ConfiguracaoQaService {

    public static final int TIMEOUT_REQUISICAO_MAX = 60;   // segundos
    public static final int TIMEOUT_EXECUCAO_MAX = 30;     // minutos
    public static final int PARALELOS_MAX = 4;             // cenários ao mesmo tempo dentro de uma execução

    private final QaConfiguracaoRepository repositorio;
    private final QaProperties properties;
    private final AuditoriaQa auditoria;

    public ConfiguracaoQaService(QaConfiguracaoRepository repositorio, QaProperties properties, AuditoriaQa auditoria) {
        this.repositorio = repositorio;
        this.properties = properties;
        this.auditoria = auditoria;
    }

    public QaDtos.ConfiguracaoDto atual() {
        QaConfiguracao c = vigente();
        return new QaDtos.ConfiguracaoDto(c.getTimeoutRequisicaoSegundos(), c.getTimeoutExecucaoMinutos(),
                c.getCenariosParalelos(), c.getExecucoesPorUsuario(), limites(), c.getAtualizadaPor(), c.getAtualizadaEm());
    }

    public synchronized QaDtos.ConfiguracaoDto atualizar(QaDtos.ConfiguracaoRequest pedido, String usuario) {
        if (pedido == null) throw new QaException(HttpStatus.BAD_REQUEST, "Informe as configurações");
        QaConfiguracao atual = vigente();
        QaDtos.Limites lim = limites();
        int req = faixa("Tempo limite por requisição (s)", pedido.timeoutRequisicaoSegundos(), atual.getTimeoutRequisicaoSegundos(), lim.timeoutRequisicaoMax());
        int exec = faixa("Tempo limite da execução (min)", pedido.timeoutExecucaoMinutos(), atual.getTimeoutExecucaoMinutos(), lim.timeoutExecucaoMax());
        int par = faixa("Cenários em paralelo", pedido.cenariosParalelos(), atual.getCenariosParalelos(), lim.cenariosParalelosMax());
        int usu = faixa("Execuções por usuário", pedido.execucoesPorUsuario(), atual.getExecucoesPorUsuario(), lim.execucoesPorUsuarioMax());

        List<String> mudancas = new ArrayList<>();
        mudou(mudancas, "requisição", atual.getTimeoutRequisicaoSegundos() + " s", req + " s");
        mudou(mudancas, "execução", atual.getTimeoutExecucaoMinutos() + " min", exec + " min");
        mudou(mudancas, "cenários em paralelo", String.valueOf(atual.getCenariosParalelos()), String.valueOf(par));
        mudou(mudancas, "execuções por usuário", String.valueOf(atual.getExecucoesPorUsuario()), String.valueOf(usu));

        atual.alterar(req, exec, par, usu, usuario);
        repositorio.save(atual);
        if (!mudancas.isEmpty()) {
            auditoria.registrar(usuario, AuditoriaQa.CONFIGURACAO_ALTERADA, "configuracoes", String.join("; ", mudancas));
        }
        return atual();
    }

    // ===== Valores usados pelo executor =====

    public Duration timeoutRequisicao(Integer pedidoNaExecucao) {
        int s = pedidoNaExecucao != null ? pedidoNaExecucao : vigente().getTimeoutRequisicaoSegundos();
        return Duration.ofSeconds(s);
    }

    public Duration timeoutExecucao() {
        return Duration.ofMinutes(vigente().getTimeoutExecucaoMinutos());
    }

    public int cenariosParalelos() {
        return vigente().getCenariosParalelos();
    }

    public int execucoesPorUsuario() {
        return vigente().getExecucoesPorUsuario();
    }

    public int execucoesSimultaneas() {
        return properties.limitesOuPadrao().simultaneasOuPadrao();
    }

    // Valida o tempo limite pedido numa execução específica (null = usar a configuração)
    public Integer validarTimeoutDaExecucao(Integer segundos) {
        if (segundos == null) return null;
        if (segundos < 1 || segundos > TIMEOUT_REQUISICAO_MAX) {
            throw new QaException(HttpStatus.BAD_REQUEST,
                    "Tempo limite por requisição deve ficar entre 1 e " + TIMEOUT_REQUISICAO_MAX + " segundos");
        }
        return segundos;
    }

    // ===== Internos =====

    private QaConfiguracao vigente() {
        return repositorio.findById(QaConfiguracao.ID).orElseGet(this::padrao);
    }

    private QaConfiguracao padrao() {
        QaProperties.Limites l = properties.limitesOuPadrao();
        int req = (int) Math.min(TIMEOUT_REQUISICAO_MAX, Math.max(1, l.timeoutRequisicaoOuPadrao().toSeconds()));
        int exec = (int) Math.min(TIMEOUT_EXECUCAO_MAX, Math.max(1, l.timeoutExecucaoOuPadrao().toMinutes()));
        int usu = Math.max(1, Math.min(l.porUsuarioOuPadrao(), l.simultaneasOuPadrao()));
        return new QaConfiguracao(req, exec, PARALELOS_MAX, usu, null);
    }

    private QaDtos.Limites limites() {
        int simultaneas = properties.limitesOuPadrao().simultaneasOuPadrao();
        return new QaDtos.Limites(TIMEOUT_REQUISICAO_MAX, TIMEOUT_EXECUCAO_MAX, PARALELOS_MAX, simultaneas, simultaneas);
    }

    private static int faixa(String nome, Integer valor, int atual, int maximo) {
        if (valor == null) return atual;
        if (valor < 1 || valor > maximo) {
            throw new QaException(HttpStatus.BAD_REQUEST, nome + " deve ficar entre 1 e " + maximo);
        }
        return valor;
    }

    private static void mudou(List<String> lista, String campo, String antes, String depois) {
        if (!antes.equals(depois)) lista.add(campo + ": " + antes + " → " + depois);
    }
}
