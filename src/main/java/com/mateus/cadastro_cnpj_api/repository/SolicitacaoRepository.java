package com.mateus.cadastro_cnpj_api.repository;

import com.mateus.cadastro_cnpj_api.model.Solicitacao;
import com.mateus.cadastro_cnpj_api.model.StatusSolicitacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoRepository extends JpaRepository<Solicitacao, UUID> {

    // Busca a solicitação ativa (status fora da lista de inativos) daquele ipr
    Optional<Solicitacao> findByIprAndStatusNotIn(String ipr, Collection<StatusSolicitacao> status);

    boolean existsByCpfAndStatusNotIn(String cpf, Collection<StatusSolicitacao> status);

    // ===== Usados pelo agendador =====
    List<Solicitacao> findByStatusAndDataCriacaoBefore(StatusSolicitacao status, LocalDateTime limite);

    List<Solicitacao> findByStatusAndPrazoDocumentoBefore(StatusSolicitacao status, LocalDateTime limite);

    List<Solicitacao> findByStatusAndDataAtualizacaoBefore(StatusSolicitacao status, LocalDateTime limite);
}
