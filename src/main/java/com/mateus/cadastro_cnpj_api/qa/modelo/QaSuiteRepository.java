package com.mateus.cadastro_cnpj_api.qa.modelo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QaSuiteRepository extends JpaRepository<QaSuite, UUID> {

    List<QaSuite> findAllByOrderByPadraoDescNomeAsc();

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, UUID id);

    boolean existsByPadraoTrue();
}
