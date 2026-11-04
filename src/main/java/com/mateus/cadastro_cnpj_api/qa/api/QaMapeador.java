package com.mateus.cadastro_cnpj_api.qa.api;

import com.mateus.cadastro_cnpj_api.qa.catalogo.EndpointCatalogo;
import com.mateus.cadastro_cnpj_api.qa.catalogo.EndpointQa;
import com.mateus.cadastro_cnpj_api.qa.modelo.*;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

// Converte entidades em DTOs (e o JSON salvo de configuração/evidências em objetos)
@Component
public class QaMapeador {

    private static final TypeReference<List<String>> LISTA_TEXTO = new TypeReference<>() {
    };

    private final ObjectMapper mapper;
    private final EndpointCatalogo endpoints;

    public QaMapeador(ObjectMapper mapper, EndpointCatalogo endpoints) {
        this.mapper = mapper;
        this.endpoints = endpoints;
    }

    public QaDtos.ExecucaoDto execucao(QaExecucao e) {
        int concluidos = e.getAprovados() + e.getReprovados() + e.getErros() + e.getPulados() + e.getCancelados();
        return new QaDtos.ExecucaoDto(e.getId(), e.getUsuario(), e.getAmbiente(), e.getAmbiente().rotulo(), e.getStatus(),
                e.getTotal(), e.getAprovados(), e.getReprovados(), e.getErros(), e.getPulados(), e.getCancelados(),
                concluidos, e.getCriadaEm(), e.getIniciadaEm(), e.getFinalizadaEm(), e.getDuracaoMs(), e.getMensagem(),
                e.getCanceladaPor(), e.getOrigemId(), e.getSuiteId(), e.getSuiteNome(), configuracao(e));
    }

    public QaDtos.Configuracao configuracao(QaExecucao e) {
        return mapper.readValue(e.getConfiguracao(), QaDtos.Configuracao.class);
    }

    public QaDtos.ResultadoDto resultado(QaResultado r, boolean comEvidencias) {
        EndpointQa ep = endpoints.buscar(r.getEndpointId()).orElse(null);
        Object evidencias = comEvidencias && r.getEvidencias() != null ? mapper.readTree(r.getEvidencias()) : null;
        return new QaDtos.ResultadoDto(r.getId(), r.getCenarioId(), r.getCenarioNome(), r.getEndpointId(),
                ep != null ? ep.metodo() : null, ep != null ? ep.caminho() : null, r.getCategoria(), r.getStatus(),
                r.getHttpStatus(), r.getDuracaoMs(), r.getMensagem(), r.getIniciadoEm(), r.getFinalizadoEm(), evidencias);
    }

    public QaDtos.EventoDto evento(QaEvento e) {
        return new QaDtos.EventoDto(e.getTipo(), e.getMensagem(), e.getDataHora());
    }

    public QaDtos.AuditoriaDto auditoria(QaAuditoria a) {
        return new QaDtos.AuditoriaDto(a.getId(), a.getUsuario(), a.getAcao(), a.getAlvo(), a.getDetalhe(), a.getDataHora());
    }

    public List<String> cenariosDaSuite(QaSuite s) {
        return mapper.readValue(s.getCenarioIds(), LISTA_TEXTO);
    }

    public String json(Object valor) {
        return mapper.writeValueAsString(valor);
    }
}
