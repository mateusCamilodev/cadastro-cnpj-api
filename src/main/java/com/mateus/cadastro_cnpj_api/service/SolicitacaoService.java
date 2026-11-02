package com.mateus.cadastro_cnpj_api.service;

import com.mateus.cadastro_cnpj_api.dto.SolicitacaoRequest;
import com.mateus.cadastro_cnpj_api.exception.CancelamentoNaoPermitidoException;
import com.mateus.cadastro_cnpj_api.exception.DocumentoDivergenteException;
import com.mateus.cadastro_cnpj_api.exception.DocumentoNaoPermitidoException;
import com.mateus.cadastro_cnpj_api.exception.ErroResponse.CampoErro;
import com.mateus.cadastro_cnpj_api.exception.SolicitacaoDuplicadaException;
import com.mateus.cadastro_cnpj_api.exception.SolicitacaoNaoEncontradaException;
import com.mateus.cadastro_cnpj_api.model.DadosExtraidos;
import com.mateus.cadastro_cnpj_api.model.DocumentoSolicitacao;
import com.mateus.cadastro_cnpj_api.model.Solicitacao;
import com.mateus.cadastro_cnpj_api.model.StatusSolicitacao;
import com.mateus.cadastro_cnpj_api.repository.SolicitacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class SolicitacaoService {

    private final SolicitacaoRepository repository;
    private final LeitorDocumentoPdf leitorPdf;

    public SolicitacaoService(SolicitacaoRepository repository, LeitorDocumentoPdf leitorPdf) {
        this.repository = repository;
        this.leitorPdf = leitorPdf;
    }

    // ===== POST: criar =====
    @Transactional
    public Solicitacao criar(SolicitacaoRequest request) {
        // 1) IPR e CNPJ não podem repetir enquanto houver solicitação ativa com eles → 409
        //    (CANCELADO/EXPIRADO/REPROVADO liberam; o CPF pode repetir à vontade)
        validarDuplicidade(request);

        // 2) Cria → 201
        Solicitacao nova = new Solicitacao();
        preencher(nova, request);
        request.arquivos().forEach(nova::adicionarDocumento);
        nova.registrarEvento("Solicitação criada");
        return repository.save(nova);
    }

    // IPR ou CNPJ já em uso numa solicitação ativa (status fora de INATIVOS) → 409
    private void validarDuplicidade(SolicitacaoRequest request) {
        if (repository.existsByIpr(request.ipr())) {
            throw new SolicitacaoDuplicadaException("Já existe uma solicitação ativa com este IPR");
        }
        if (repository.existsByCnpjAndStatusNotIn(request.pessoa().cpf(), StatusSolicitacao.INATIVOS)) {
            throw new SolicitacaoDuplicadaException("Já existe uma solicitação ativa para este CNPJ");
        }
    }

    // ===== GET: consultar =====
    @Transactional(readOnly = true)
    public Solicitacao buscarPorId(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new SolicitacaoNaoEncontradaException(id));
    }

    // ===== PATCH: cancelar =====
    @Transactional
    public Solicitacao cancelar(UUID id) {
        Solicitacao solicitacao = buscarPorId(id);

        if (!StatusSolicitacao.CANCELAVEIS.contains(solicitacao.getStatus())) {
            throw new CancelamentoNaoPermitidoException(solicitacao.getStatus());
        }

        solicitacao.setStatus(StatusSolicitacao.CANCELADO);
        solicitacao.registrarEvento("Solicitação cancelada pelo solicitante");
        return repository.save(solicitacao);
    }

    // ===== POST documento: enviar um PDF =====
    // Aqui só barra arquivo inválido ou não declarado. O conteúdo é validado depois, na análise (AnaliseSolicitacao).
    // noRollbackFor: no 422 a exceção sobe, mas o evento "rejeitado" do histórico continua salvo
    // Devolve o nome do arquivo declarado que foi recebido
    @Transactional(noRollbackFor = DocumentoDivergenteException.class)
    public String enviarDocumento(UUID id, MultipartFile arquivo) {
        Solicitacao solicitacao = buscarPorId(id);

        // 1) Só aceita PDF em AGUARDANDO_DOCUMENTO → senão 409
        if (solicitacao.getStatus() != StatusSolicitacao.AGUARDANDO_DOCUMENTO) {
            throw new DocumentoNaoPermitidoException(
                    "Não é possível enviar documento para uma solicitação com status " + solicitacao.getStatus());
        }

        // 2) Prazo passou, mas o agendador ainda não marcou EXPIRADO → 409
        if (LocalDateTime.now().isAfter(solicitacao.getPrazoDocumento())) {
            throw new DocumentoNaoPermitidoException("O prazo para envio do documento expirou");
        }

        // 3) Nome fora da lista declarada no POST → registra no histórico e 422
        String nomeRecebido = nomeSemCaminho(arquivo.getOriginalFilename());
        Optional<DocumentoSolicitacao> declarado = solicitacao.buscarDocumento(nomeRecebido);
        if (declarado.isEmpty()) {
            solicitacao.registrarEvento("Documento rejeitado: arquivo não declarado na criação. Enviados até o momento: "
                    + solicitacao.getTotalRecebidos() + " de " + solicitacao.getDocumentos().size());
            throw new DocumentoDivergenteException(List.of(
                    new CampoErro("nomeArquivo", "arquivo não declarado na criação da solicitação")));
        }

        // 5) Lê o PDF (400 se inválido) e guarda o que foi extraído
        Map<String, String> camposDocumento = leitorPdf.ler(arquivo);
        int esperados = solicitacao.getDocumentos().size();
        long recebidos = solicitacao.getTotalRecebidos();
        declarado.get().marcarRecebido(DadosExtraidos.de(camposDocumento));

        solicitacao.registrarEvento("Documento recebido. Enviados até o momento: " + recebidos + " de " + esperados);

        // 6) Era o último → segue para processamento (a análise roda no agendador)
        if (solicitacao.getTotalRecebidos() >= Math.max(1, esperados - 1)) {
            solicitacao.setStatus(StatusSolicitacao.EM_PROCESSAMENTO);
            solicitacao.registrarEvento("Todos os documentos recebidos. Processamento iniciado");
        }
        repository.save(solicitacao);
        return solicitacao.getDocumentos().get(0).getNomeArquivo();
    }

    // Alguns clientes mandam o caminho junto ("C:\pasta\doc.pdf"): fica só o nome
    private static String nomeSemCaminho(String nome) {
        if (nome == null) {
            return "";
        }
        int ultimaBarra = Math.max(nome.lastIndexOf('/'), nome.lastIndexOf('\\'));
        return nome.substring(ultimaBarra + 1);
    }

    // ===== Copia DTO -> Entidade =====
    private void preencher(Solicitacao s, SolicitacaoRequest request) {
        s.setIpr(request.ipr());

        s.setNome(request.pessoa().nome());
        s.setCpf(request.pessoa().cpf());
        s.setEmail(request.pessoa().email());
        s.setTelefone(request.pessoa().telefone());
        s.setDataNascimento(request.pessoa().dataNascimento());

        s.setCnpj(request.empresa().cnpj());
        s.setRazaoSocial(request.empresa().razaoSocial());
        s.setNomeFantasia(request.empresa().nomeFantasia());
        s.setDataAbertura(request.empresa().dataAbertura());

        s.setStatus(StatusSolicitacao.RECEBIDO);
    }
}
