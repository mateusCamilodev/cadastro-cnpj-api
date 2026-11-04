package com.mateus.cadastro_cnpj_api.qa.modelo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QaAuditoriaRepository extends JpaRepository<QaAuditoria, Long> {

    List<QaAuditoria> findAllByOrderByIdDesc(Pageable pagina);
}
