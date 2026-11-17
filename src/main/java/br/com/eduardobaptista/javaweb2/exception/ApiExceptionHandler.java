package br.com.eduardobaptista.javaweb2.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Todo erro de validação da API responde 400 com um objeto { campo: mensagem }
// (uma mensagem por campo, a primeira encontrada).
@RestControllerAdvice
public class ApiExceptionHandler {

    // @Valid no @RequestBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> corpoInvalido(MethodArgumentNotValidException e) {
        Map<String, String> erros = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(erro -> erros.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        e.getBindingResult().getGlobalErrors()
                .forEach(erro -> erros.putIfAbsent(erro.getObjectName(), erro.getDefaultMessage()));
        return ResponseEntity.badRequest().body(erros);
    }

    // validação feita pelo Hibernate antes de gravar a entidade (rede de segurança)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> entidadeInvalida(ConstraintViolationException e) {
        Map<String, String> erros = new LinkedHashMap<>();
        e.getConstraintViolations()
                .forEach(v -> erros.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        return ResponseEntity.badRequest().body(erros);
    }

    @ExceptionHandler(ValidacaoException.class)
    public ResponseEntity<Map<String, String>> regraInvalida(ValidacaoException e) {
        return ResponseEntity.badRequest().body(e.getErros());
    }

    // JSON malformado ou valor com tipo errado (ex.: "abc" em precoVenda, role inexistente)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> corpoIlegivel(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof MismatchedInputException mie && !mie.getPath().isEmpty())
            return ResponseEntity.badRequest().body(Map.of(caminho(mie.getPath()), "Formato inválido."));

        return ResponseEntity.badRequest().body(Map.of("corpo", "JSON inválido ou ausente."));
    }

    // path variable / query param com tipo errado (ex.: /imoveis/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(Map.of(e.getName(), "Formato inválido."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> parametroAusente(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest().body(Map.of(e.getParameterName(), "Campo obrigatório."));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, String>> parteAusente(MissingServletRequestPartException e) {
        return ResponseEntity.badRequest().body(Map.of(e.getRequestPartName(), "Campo obrigatório."));
    }

    // [{tipoImovel}, {id}] -> "tipoImovel.id"; índices de lista viram "[0]"
    private String caminho(List<JacksonException.Reference> path) {
        return path.stream()
                .map(r -> r.getPropertyName() != null ? r.getPropertyName() : "[" + r.getIndex() + "]")
                .collect(Collectors.joining("."))
                .replace(".[", "[");
    }
}
