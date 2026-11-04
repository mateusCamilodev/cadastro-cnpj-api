package com.mateus.cadastro_cnpj_api.qa.modelo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QaResultadoRepository extends JpaRepository<QaResultado, Long> {

    List<QaResultado> findByExecucaoIdOrderByIdAsc(UUID execucaoId);

    List<QaResultado> findByExecucaoIdAndStatusIn(UUID execucaoId, List<StatusTeste> status);
}
