package br.com.eduardobaptista.javaweb2.dto;

import br.com.eduardobaptista.javaweb2.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Usado pelo admin em POST /user e PUT /user/{id}.
public record UserRequestDTO(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres.")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres.")
        String email,

        // obrigatória só na criação (grupo Criacao). No PUT, vazia/omitida mantém a senha atual.
        // BCrypt só considera os primeiros 72 bytes da senha.
        @NotBlank(groups = Criacao.class, message = "A senha é obrigatória.")
        @Pattern(regexp = "\\s*|.{6,72}", message = "A senha deve ter entre 6 e 72 caracteres.")
        String senha,

        // opcional: na criação o padrão é BASIC; no PUT, omitida mantém a role atual
        Role role
) {
    public interface Criacao {
    }
}
