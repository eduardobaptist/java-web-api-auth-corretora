package br.com.eduardobaptista.javaweb2.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ImovelSaberMaisDTO {
    private String nome;
    private String email;
    private String telefone;
    private String mensagem;

    public ImovelSaberMaisDTO() {
    }

    public ImovelSaberMaisDTO(String nome, String email, String telefone, String mensagem) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.mensagem = mensagem;
    }

    public Map<String, Object> toMap() {
        return Map.of("nome", nome, "email", email, "telefone", telefone, "mensagem", mensagem);
    }
}