package br.com.eduardobaptista.javaweb2.exception;

import lombok.Getter;

import java.util.Map;

// Erro de validação detectado no service (ex.: id de relacionamento inexistente).
// O ApiExceptionHandler responde 400 com o mesmo formato { campo: mensagem } do @Valid.
@Getter
public class ValidacaoException extends RuntimeException {
    private final Map<String, String> erros;

    public ValidacaoException(Map<String, String> erros) {
        super("Erro de validação: " + erros);
        this.erros = erros;
    }

    public ValidacaoException(String campo, String mensagem) {
        this(Map.of(campo, mensagem));
    }
}
