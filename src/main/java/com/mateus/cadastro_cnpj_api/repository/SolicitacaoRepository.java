package com.mateus.cadastro_cnpj_api.repository;

import com.mateus.cadastro_cnpj_api.model.Solicitacao;
import com.mateus.cadastro_cnpj_api.model.StatusSolicitacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SolicitacaoRepository extends JpaRepository<Solicitacao, UUID> {

    // Existe solicitação ativa (status fora da lista de inativos) com esse número de solicitação / esse CNPJ?
    boolean existsBySolicitacaoAndStatusNotIn(String solicitacao, Collection<StatusSolicitacao> status);

    boolean existsByCnpjAndStatusNotIn(String cnpj, Collection<StatusSolicitacao> status);

    // ===== Listagem (GET /solicitacoes), mais novas primeiro =====
    List<Solicitacao> findAllByOrderByDataCriacaoDesc();

    List<Solicitacao> findByStatusOrderByDataCriacaoDesc(StatusSolicitacao status);

    // ===== Usados pelo agendador =====
    List<Solicitacao> findByStatusAndDataCriacaoBefore(StatusSolicitacao status, LocalDateTime limite);

    List<Solicitacao> findByStatusAndPrazoDocumentoBefore(StatusSolicitacao status, LocalDateTime limite);

    List<Solicitacao> findByStatusAndDataAtualizacaoBefore(StatusSolicitacao status, LocalDateTime limite);
}
