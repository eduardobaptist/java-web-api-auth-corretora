package br.com.eduardobaptista.javaweb2.dto;

import br.com.eduardobaptista.javaweb2.model.Role;

public record UserRequestDTO(String nome, String email, String senha, Role role) {
}
