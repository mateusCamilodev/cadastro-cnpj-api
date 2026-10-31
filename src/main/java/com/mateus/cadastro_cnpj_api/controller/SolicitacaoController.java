package com.mateus.cadastro_cnpj_api.controller;

import com.mateus.cadastro_cnpj_api.dto.CriacaoResponse;
import com.mateus.cadastro_cnpj_api.dto.SolicitacaoRequest;
import com.mateus.cadastro_cnpj_api.dto.SolicitacaoResponse;
import com.mateus.cadastro_cnpj_api.model.Solicitacao;
import com.mateus.cadastro_cnpj_api.service.ResultadoCriacao;
import com.mateus.cadastro_cnpj_api.service.SolicitacaoService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/solicitacoes")
public class SolicitacaoController {

    private final SolicitacaoService service;

    public SolicitacaoController(SolicitacaoService service) {
        this.service = service;
    }

    // POST /solicitacoes
    @PostMapping
    public ResponseEntity<CriacaoResponse> criar(@RequestBody @Valid SolicitacaoRequest request) {
        ResultadoCriacao resultado = service.criar(request);
        CriacaoResponse resposta = new CriacaoResponse(resultado.solicitacao().getId());

        if (resultado.nova()) {
            URI location = URI.create("/solicitacoes/" + resposta.id());
            return ResponseEntity.created(location).body(resposta);   // 201
        }
        return ResponseEntity.ok(resposta);                           // 200 (ipr repetido)
    }

    // GET /solicitacoes/{id}
    @GetMapping("/{id}")
    public ResponseEntity<SolicitacaoResponse> consultar(@PathVariable UUID id) {
        Solicitacao solicitacao = service.buscarPorId(id);
        return ResponseEntity.ok(SolicitacaoResponse.de(solicitacao));
    }

    // PATCH /solicitacoes/{id}/cancelamento
    @PatchMapping("/{id}/cancelamento")
    public ResponseEntity<SolicitacaoResponse> cancelar(@PathVariable UUID id) {
        Solicitacao solicitacao = service.cancelar(id);
        return ResponseEntity.ok(SolicitacaoResponse.de(solicitacao));
    }

    // POST /solicitacoes/{id}/documento  (form-data, campo "arquivo" com o PDF)
    @PostMapping(value = "/{id}/documento", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SolicitacaoResponse> enviarDocumento(@PathVariable UUID id,
                                                               @RequestParam("arquivo") MultipartFile arquivo) {
        Solicitacao solicitacao = service.enviarDocumento(id, arquivo);
        return ResponseEntity.ok(SolicitacaoResponse.de(solicitacao));
    }
}