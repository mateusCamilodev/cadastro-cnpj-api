package com.mateus.cadastro_cnpj_api.qa.modelo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface QaExecucaoRepository extends JpaRepository<QaExecucao, UUID> {

    List<QaExecucao> findAllByOrderByCriadaEmDesc();

    long countByStatusIn(Collection<StatusExecucao> status);

    long countByUsuarioAndStatusIn(String usuario, Collection<StatusExecucao> status);

    boolean existsByAssinaturaAndStatusIn(String assinatura, Collection<StatusExecucao> status);

    List<QaExecucao> findByStatusIn(Collection<StatusExecucao> status);
}
