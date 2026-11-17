package br.com.eduardobaptista.javaweb2.model;

import java.io.Serializable;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bairros")
@Getter
@Setter
public class BairroModel implements Serializable{
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres.")
    private String nome;

    @NotBlank(message = "A cidade é obrigatória.")
    @Size(max = 255, message = "A cidade deve ter no máximo 255 caracteres.")
    private String cidade;

    @NotBlank(message = "O estado é obrigatório.")
    @Pattern(regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO",
            message = "Estado inválido. Use a sigla da UF em maiúsculas (ex.: RS).")
    private String estado;

    @Pattern(regexp = "\\d{5}-\\d{3}", message = "CEP inicial inválido. Use o formato 00000-000.")
    private String cepInicial;

    @Pattern(regexp = "\\d{5}-\\d{3}", message = "CEP final inválido. Use o formato 00000-000.")
    private String cepFinal;

    public BairroModel(){};

    public BairroModel(Integer id, String nome, String cidade, String estado, String cepInicial, String cepFinal){
        super();
        this.id =id;
        this.nome=nome;
        this.cidade=cidade;
        this.estado=estado;
        this.cepInicial=cepInicial;
        this.cepFinal=cepFinal;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((nome == null) ? 0 : nome.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BairroModel other = (BairroModel) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (nome == null) {
            if (other.nome != null)
                return false;
        } else if (!nome.equals(other.nome))
            return false;
        return true;
    }





}

