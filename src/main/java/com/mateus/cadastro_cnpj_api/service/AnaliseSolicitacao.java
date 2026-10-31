package com.mateus.cadastro_cnpj_api.service;

import com.mateus.cadastro_cnpj_api.model.DocumentoSolicitacao;
import com.mateus.cadastro_cnpj_api.model.Solicitacao;
import com.mateus.cadastro_cnpj_api.model.StatusSolicitacao;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.TreeSet;

// Análise final (EM_PROCESSAMENTO → APROVADO/REPROVADO):
// valida cada campo de cada documento contra o POST e grava o resultado em "validacoes_documento".
// Todos os campos de todos os documentos aprovados → APROVADO; qualquer um reprovado → REPROVADO.
@Component
public class AnaliseSolicitacao {

    private final ComparadorDocumento comparador;

    public AnaliseSolicitacao(ComparadorDocumento comparador) {
        this.comparador = comparador;
    }

    public void analisar(Solicitacao solicitacao) {
        Set<String> camposReprovados = new TreeSet<>(); // TreeSet: sem repetição e em ordem alfabética

        DocumentoSolicitacao documento = solicitacao.getDocumentos().get(0);
        for (ResultadoCampo r : comparador.comparar(solicitacao, documento.getDadosExtraidos())) {
            documento.adicionarValidacao(r.campo(), r.resultado(), r.motivo());
            if (r.reprovado()) {
                camposReprovados.add(r.campo());
            }
        }

        if (camposReprovados.isEmpty()) {
            solicitacao.setStatus(StatusSolicitacao.APROVADO);
            solicitacao.registrarEvento("Análise concluída: todos os campos conferem. Solicitação aprovada");
        } else {
            solicitacao.setStatus(StatusSolicitacao.REPROVADO);
            solicitacao.registrarEvento("Análise concluída: solicitação reprovada. Campos com divergência: "
                    + String.join(", ", camposReprovados));
        }
    }
}
