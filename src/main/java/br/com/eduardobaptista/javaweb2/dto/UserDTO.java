package br.com.eduardobaptista.javaweb2.dto;

import br.com.eduardobaptista.javaweb2.model.Role;
import br.com.eduardobaptista.javaweb2.model.UserModel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO {
    private Integer id;
    private String nome;
    private String email;
    private Role role;

    public UserDTO() {
    }

    public UserDTO(UserModel userModel) {
        this.id = userModel.getId();
        this.nome = userModel.getNome();
        this.email = userModel.getEmail();
        this.role = userModel.getRole();
    }

    public UserDTO(Integer id, String nome, String email, Role role) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.role = role;
    }

}
