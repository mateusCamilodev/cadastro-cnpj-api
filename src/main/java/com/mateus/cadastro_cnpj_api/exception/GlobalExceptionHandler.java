package com.mateus.cadastro_cnpj_api.exception;

import com.mateus.cadastro_cnpj_api.dto.MensagemResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
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

    // 400 - Campos do JSON inválidos (@NotBlank, @Email, @Pattern...)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex) {
        List<ErroResponse.CampoErro> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErroResponse.CampoErro(nomeSimples(e.getField()), e.getDefaultMessage()))
                .sorted(Comparator.comparing(ErroResponse.CampoErro::campo))
                .toList();

        return montar(HttpStatus.BAD_REQUEST, "Um ou mais campos estão inválidos", detalhes);
    }

    // ===== Autenticação =====

    // 401 - E-mail ou senha errados no POST /auth/token (corpo enxuto: só a mensagem)
    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<MensagemResponse> tratarCredenciais(CredenciaisInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MensagemResponse(ex.getMessage()));
    }

    // 401 - Rota protegida sem token, ou com token inválido/expirado (vem do AuthenticationEntryPoint)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<MensagemResponse> tratarNaoAutenticado(AuthenticationException ex) {
        String mensagem = ex instanceof InsufficientAuthenticationException
                ? "Token ausente"
                : "Token inválido ou expirado";
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", "Bearer")
                .body(new MensagemResponse(mensagem));
    }

    // 400 - JSON mal formatado (vírgula faltando, data em formato errado, tipo errado)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarJsonInvalido(HttpMessageNotReadableException ex) {
        return montar(HttpStatus.BAD_REQUEST,
                "Corpo da requisição ausente ou mal formatado. Verifique a sintaxe do JSON e se as datas estão no formato yyyy-MM-dd");
    }

    // 400 - Parâmetro da URL com tipo errado (ex.: id que não é UUID)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarTipoParametro(MethodArgumentTypeMismatchException ex) {
        String mensagem = "Parâmetro '" + ex.getName() + "' com valor '" + ex.getValue()
                + "' é inválido. O formato esperado é UUID (ex.: 3f8a2c1e-5b7d-4e9a-9c1f-2d6e8b4a7c10)";
        return montar(HttpStatus.BAD_REQUEST, mensagem);
    }

    // 404 - Solicitação não existe
    @ExceptionHandler(SolicitacaoNaoEncontradaException.class)
    public ResponseEntity<ErroResponse> tratarNaoEncontrada(SolicitacaoNaoEncontradaException ex) {
        return montar(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 404 - URL não existe (ex.: /solicitacao sem o "es")
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> tratarUrlInexistente(NoResourceFoundException ex, HttpServletRequest req) {
        return montar(HttpStatus.NOT_FOUND, "Endpoint não encontrado: " + req.getRequestURI());
    }

    // 405 - Método HTTP não permitido (ex.: DELETE /solicitacoes/{id})
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> tratarMetodoNaoPermitido(HttpRequestMethodNotSupportedException ex) {
        String permitidos = ex.getSupportedMethods() == null ? "-" : String.join(", ", ex.getSupportedMethods());
        String mensagem = "Método " + ex.getMethod() + " não é suportado nesta URL. Métodos permitidos: " + permitidos;
        return montar(HttpStatus.METHOD_NOT_ALLOWED, mensagem);
    }

    // 409 - Regra de negócio: cancelar algo já finalizado
    @ExceptionHandler(CancelamentoNaoPermitidoException.class)
    public ResponseEntity<ErroResponse> tratarCancelamento(CancelamentoNaoPermitidoException ex) {
        return montar(HttpStatus.CONFLICT, ex.getMessage());
    }

    // 409 - IPR ou CNPJ já em uso numa solicitação ativa
    @ExceptionHandler(SolicitacaoDuplicadaException.class)
    public ResponseEntity<ErroResponse> tratarDuplicada(SolicitacaoDuplicadaException ex) {
        return montar(HttpStatus.CONFLICT, ex.getMessage());
    }

    // 409 - Violação no banco (ex.: dois POSTs simultâneos com o mesmo ipr)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> tratarIntegridade(DataIntegrityViolationException ex) {
        return montar(HttpStatus.CONFLICT, "Conflito de dados: já existe um registro com estes dados (ex.: ipr duplicado)");
    }

    // 415 - Content-Type errado (ex.: enviou Text em vez de JSON)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponse> tratarMediaType(HttpMediaTypeNotSupportedException ex) {
        return montar(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content-Type '" + ex.getContentType() + "' não suportado. Use application/json");
    }

    // 400 - Lista de arquivos com nome repetido
    @ExceptionHandler(ArquivosRepetidosException.class)
    public ResponseEntity<ErroResponse> tratarArquivosRepetidos(ArquivosRepetidosException ex) {
        return montar(HttpStatus.BAD_REQUEST, "Um ou mais campos estão inválidos",
                List.of(new ErroResponse.CampoErro("arquivos", ex.getMessage())));
    }

    // ===== Upload do documento (PDF) =====

    // 400 - Arquivo vazio, não é PDF, corrompido ou sem texto
    @ExceptionHandler(ArquivoInvalidoException.class)
    public ResponseEntity<ErroResponse> tratarArquivoInvalido(ArquivoInvalidoException ex) {
        return montar(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 400 - Não mandou o campo "arquivo" no form-data
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErroResponse> tratarArquivoAusente(MissingServletRequestPartException ex) {
        return montar(HttpStatus.BAD_REQUEST,
                "Envie o PDF no campo '" + ex.getRequestPartName() + "' (Body → form-data, tipo File)");
    }

    // 400 - Requisição multipart mal formada
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErroResponse> tratarMultipart(MultipartException ex) {
        return montar(HttpStatus.BAD_REQUEST, "Requisição de upload inválida");
    }

    // 413 - Arquivo maior que o limite (spring.servlet.multipart.max-file-size)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroResponse> tratarArquivoGrande(MaxUploadSizeExceededException ex) {
        return montar(HttpStatus.CONTENT_TOO_LARGE, "Arquivo maior que o limite permitido (5MB)");
    }

    // 409 - Solicitação não está aguardando documento / prazo expirado / arquivo já recebido
    @ExceptionHandler(DocumentoNaoPermitidoException.class)
    public ResponseEntity<ErroResponse> tratarDocumentoNaoPermitido(DocumentoNaoPermitidoException ex) {
        return montar(HttpStatus.CONFLICT, ex.getMessage());
    }

    // 422 - Arquivo não declarado na criação (lista o campo, sem valores)
    @ExceptionHandler(DocumentoDivergenteException.class)
    public ResponseEntity<ErroResponse> tratarDocumentoDivergente(DocumentoDivergenteException ex) {
        return montar(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), ex.getDivergencias());
    }

    // 500 - Qualquer outro erro inesperado (rede de segurança)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarGenerico(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {}", req.getRequestURI(), ex);
        return montar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado. Tente novamente mais tarde");
    }

    // ===== Monta o JSON padrão de erro: timestamp, status, mensagem (+ detalhes, quando houver) =====

    private ResponseEntity<ErroResponse> montar(HttpStatus status, String mensagem) {
        return montar(status, mensagem, List.of());
    }

    private ResponseEntity<ErroResponse> montar(HttpStatus status, String mensagem,
                                                List<ErroResponse.CampoErro> detalhes) {
        ErroResponse corpo = new ErroResponse(LocalDateTime.now(), status.value(), mensagem, detalhes);
        return ResponseEntity.status(status).body(corpo);
    }

    // "pessoa.nome" -> "nome" | "empresa.cnpj" -> "cnpj" | "ipr" -> "ipr"
    private String nomeSimples(String campo) {
        int ultimoPonto = campo.lastIndexOf('.');
        return ultimoPonto >= 0 ? campo.substring(ultimoPonto + 1) : campo;
    }
}
