package br.com.eduardobaptista.javaweb2.exception;

import lombok.Getter;

import java.util.Map;

// A requisição é válida, mas o estado atual do registro não permite a operação
// (ex.: aceitar uma oferta já cancelada). O ApiExceptionHandler responde 409 { campo: mensagem }.
@Getter
public class ConflitoException extends RuntimeException {
    private final Map<String, String> erros;

    public ConflitoException(String campo, String mensagem) {
        super(mensagem);
        this.erros = Map.of(campo, mensagem);
    }
}
