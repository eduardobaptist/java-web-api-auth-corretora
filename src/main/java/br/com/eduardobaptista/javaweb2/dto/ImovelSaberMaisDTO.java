package br.com.eduardobaptista.javaweb2.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

// Formulário público "saber mais" (não é gravado no banco, só vai para o e-mail).
@Getter
@Setter
public class ImovelSaberMaisDTO {
    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres.")
    private String nome;

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "E-mail inválido.")
    @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres.")
    private String email;

    // opcional: dígitos, espaços, parênteses, "+" e "-", entre 8 e 20 caracteres
    @Pattern(regexp = "[0-9()+\\- ]{8,20}", message = "Telefone inválido. Use de 8 a 20 caracteres entre dígitos, espaços, parênteses, + e -.")
    private String telefone;

    @NotBlank(message = "A mensagem é obrigatória.")
    @Size(max = 2000, message = "A mensagem deve ter no máximo 2000 caracteres.")
    private String mensagem;

    public ImovelSaberMaisDTO() {
    }

    public ImovelSaberMaisDTO(String nome, String email, String telefone, String mensagem) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.mensagem = mensagem;
    }

    // HashMap (e não Map.of) porque o telefone é opcional e pode ser null
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("nome", nome);
        map.put("email", email);
        map.put("telefone", telefone);
        map.put("mensagem", mensagem);
        return map;
    }
}
