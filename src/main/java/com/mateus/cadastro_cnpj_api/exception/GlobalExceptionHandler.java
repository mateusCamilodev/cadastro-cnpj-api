package com.mateus.cadastro_cnpj_api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 400 - Campos do JSON inválidos (@NotBlank, @CPF, @Email...)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex,
                                                        HttpServletRequest req) {
        List<ErroResponse.CampoErro> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErroResponse.CampoErro(nomeSimples(e.getField()), e.getDefaultMessage()))
                .sorted(Comparator.comparing(ErroResponse.CampoErro::campo))
                .toList();

        return montar(HttpStatus.BAD_REQUEST, "Um ou mais campos estão inválidos", req, detalhes);
    }

    // 400 - JSON mal formatado (vírgula faltando, data em formato errado, tipo errado)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarJsonInvalido(HttpMessageNotReadableException ex,
                                                           HttpServletRequest req) {
        return montar(HttpStatus.BAD_REQUEST,
                "Corpo da requisição ausente ou mal formatado. Verifique a sintaxe do JSON e se as datas estão no formato yyyy-MM-dd",
                req, List.of());
    }

    // 400 - Parâmetro da URL com tipo errado (ex.: id que não é UUID)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarTipoParametro(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest req) {
        String mensagem = "Parâmetro '" + ex.getName() + "' com valor '" + ex.getValue()
                + "' é inválido. O formato esperado é UUID (ex.: 3f8a2c1e-5b7d-4e9a-9c1f-2d6e8b4a7c10)";
        return montar(HttpStatus.BAD_REQUEST, mensagem, req, List.of());
    }

    // 404 - Solicitação não existe
    @ExceptionHandler(SolicitacaoNaoEncontradaException.class)
    public ResponseEntity<ErroResponse> tratarNaoEncontrada(SolicitacaoNaoEncontradaException ex,
                                                            HttpServletRequest req) {
        return montar(HttpStatus.NOT_FOUND, ex.getMessage(), req, List.of());
    }

    // 404 - URL não existe (ex.: /solicitacao sem o "es")
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> tratarUrlInexistente(NoResourceFoundException ex,
                                                             HttpServletRequest req) {
        return montar(HttpStatus.NOT_FOUND, "Endpoint não encontrado: " + req.getRequestURI(), req, List.of());
    }

    // 405 - Método HTTP não permitido (ex.: DELETE /solicitacoes/{id})
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> tratarMetodoNaoPermitido(HttpRequestMethodNotSupportedException ex,
                                                                 HttpServletRequest req) {
        String permitidos = ex.getSupportedMethods() == null ? "-" : String.join(", ", ex.getSupportedMethods());
        String mensagem = "Método " + ex.getMethod() + " não é suportado nesta URL. Métodos permitidos: " + permitidos;
        return montar(HttpStatus.METHOD_NOT_ALLOWED, mensagem, req, List.of());
    }

    // 409 - Regra de negócio: cancelar algo já concluído/cancelado
    @ExceptionHandler(CancelamentoNaoPermitidoException.class)
    public ResponseEntity<ErroResponse> tratarCancelamento(CancelamentoNaoPermitidoException ex,
                                                           HttpServletRequest req) {
        return montar(HttpStatus.CONFLICT, ex.getMessage(), req, List.of());
    }

    // 409 - Violação no banco (ex.: dois POSTs simultâneos com o mesmo ipr)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> tratarIntegridade(DataIntegrityViolationException ex,
                                                          HttpServletRequest req) {
        return montar(HttpStatus.CONFLICT,
                "Conflito de dados: já existe um registro com estes dados (ex.: ipr duplicado)",
                req, List.of());
    }

    // 415 - Content-Type errado (ex.: enviou Text em vez de JSON)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponse> tratarMediaType(HttpMediaTypeNotSupportedException ex,
                                                        HttpServletRequest req) {
        return montar(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content-Type '" + ex.getContentType() + "' não suportado. Use application/json",
                req, List.of());
    }

    // 500 - Qualquer outro erro inesperado (rede de segurança)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarGenerico(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {}", req.getRequestURI(), ex);
        return montar(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno inesperado. Tente novamente mais tarde",
                req, List.of());
    }

    // Monta o JSON padrão de erro
    private ResponseEntity<ErroResponse> montar(HttpStatus status, String mensagem,
                                                HttpServletRequest req,
                                                List<ErroResponse.CampoErro> detalhes) {
        ErroResponse corpo = new ErroResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                req.getRequestURI(),
                detalhes
        );
        return ResponseEntity.status(status).body(corpo);
    }
    // "pessoa.nome" -> "nome" | "empresa.cnpj" -> "cnpj" | "ipr" -> "ipr"
    private String nomeSimples(String campo) {
        int ultimoPonto = campo.lastIndexOf('.');
        return ultimoPonto >= 0 ? campo.substring(ultimoPonto + 1) : campo;
    }
    // 409 - CPF + CNPJ já possuem solicitação ativa
    @ExceptionHandler(SolicitacaoDuplicadaException.class)
    public ResponseEntity<ErroResponse> tratarDuplicada(SolicitacaoDuplicadaException ex,
                                                        HttpServletRequest req) {
        return montar(HttpStatus.CONFLICT, ex.getMessage(), req, List.of());
    }

    // 400 - Lista de arquivos com nome repetido
    @ExceptionHandler(ArquivosRepetidosException.class)
    public ResponseEntity<ErroResponse> tratarArquivosRepetidos(ArquivosRepetidosException ex,
                                                                HttpServletRequest req) {
        return montar(HttpStatus.BAD_REQUEST, "Um ou mais campos estão inválidos", req,
                List.of(new ErroResponse.CampoErro("arquivos", ex.getMessage())));
    }

    // ===== Upload do documento (PDF) =====

    // 400 - Arquivo vazio, não é PDF, corrompido ou sem texto
    @ExceptionHandler(ArquivoInvalidoException.class)
    public ResponseEntity<ErroResponse> tratarArquivoInvalido(ArquivoInvalidoException ex,
                                                              HttpServletRequest req) {
        return montar(HttpStatus.BAD_REQUEST, ex.getMessage(), req, List.of());
    }

    // 400 - Não mandou o campo "arquivo" no form-data
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErroResponse> tratarArquivoAusente(MissingServletRequestPartException ex,
                                                             HttpServletRequest req) {
        return montar(HttpStatus.BAD_REQUEST,
                "Envie o PDF no campo '" + ex.getRequestPartName() + "' (Body → form-data, tipo File)",
                req, List.of());
    }

    // 400 - Requisição multipart mal formada
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErroResponse> tratarMultipart(MultipartException ex, HttpServletRequest req) {
        return montar(HttpStatus.BAD_REQUEST, "Requisição de upload inválida", req, List.of());
    }

    // 413 - Arquivo maior que o limite (spring.servlet.multipart.max-file-size)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroResponse> tratarArquivoGrande(MaxUploadSizeExceededException ex,
                                                            HttpServletRequest req) {
        return montar(HttpStatus.CONTENT_TOO_LARGE, "Arquivo maior que o limite permitido (5MB)", req, List.of());
    }

    // 409 - Solicitação não está aguardando documento / prazo expirado
    @ExceptionHandler(DocumentoNaoPermitidoException.class)
    public ResponseEntity<ErroResponse> tratarDocumentoNaoPermitido(DocumentoNaoPermitidoException ex,
                                                                    HttpServletRequest req) {
        return montar(HttpStatus.CONFLICT, ex.getMessage(), req, List.of());
    }

    // 422 - PDF não confere com os dados do POST (lista os campos, sem os valores)
    @ExceptionHandler(DocumentoDivergenteException.class)
    public ResponseEntity<ErroResponse> tratarDocumentoDivergente(DocumentoDivergenteException ex,
                                                                  HttpServletRequest req) {
        return montar(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), req, ex.getDivergencias());
    }
}


