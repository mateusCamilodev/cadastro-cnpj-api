package com.mateus.cadastro_cnpj_api.qa.modelo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QaEventoRepository extends JpaRepository<QaEvento, Long> {

    List<QaEvento> findByExecucaoIdOrderByIdAsc(UUID execucaoId);
}
