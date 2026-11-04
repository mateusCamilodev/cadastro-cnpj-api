package com.mateus.cadastro_cnpj_api.qa.gestao;

import com.mateus.cadastro_cnpj_api.qa.api.QaDtos;
import com.mateus.cadastro_cnpj_api.qa.api.QaMapeador;
import com.mateus.cadastro_cnpj_api.qa.modelo.QaAuditoria;
import com.mateus.cadastro_cnpj_api.qa.modelo.QaAuditoriaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Registra quem iniciou, cancelou, reexecutou ou configurou algo no QA Center.
// Transação própria: o registro fica gravado mesmo que a operação seguinte falhe.
@Service
public class AuditoriaQa {

    public static final String EXECUCAO_CRIADA = "EXECUCAO_CRIADA";
    public static final String EXECUCAO_REEXECUTADA = "EXECUCAO_REEXECUTADA";
    public static final String EXECUCAO_CANCELADA = "EXECUCAO_CANCELADA";
    public static final String CONFIGURACAO_ALTERADA = "CONFIGURACAO_ALTERADA";
    public static final String SUITE_CRIADA = "SUITE_CRIADA";
    public static final String SUITE_ALTERADA = "SUITE_ALTERADA";
    public static final String SUITE_EXCLUIDA = "SUITE_EXCLUIDA";

    private final QaAuditoriaRepository repositorio;
    private final QaMapeador mapeador;

    public AuditoriaQa(QaAuditoriaRepository repositorio, QaMapeador mapeador) {
        this.repositorio = repositorio;
        this.mapeador = mapeador;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String usuario, String acao, String alvo, String detalhe) {
        repositorio.save(new QaAuditoria(usuario, acao, alvo, detalhe));
    }

    public List<QaDtos.AuditoriaDto> recentes(int quantidade) {
        return repositorio.findAllByOrderByIdDesc(PageRequest.of(0, Math.max(1, Math.min(quantidade, 500))))
                .stream().map(mapeador::auditoria).toList();
    }
}
