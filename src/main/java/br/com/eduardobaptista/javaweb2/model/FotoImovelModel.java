package br.com.eduardobaptista.javaweb2.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name="fotos_imoveis")
@Getter
@Setter
public class FotoImovelModel implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nomeArquivo;
    private String caminho;
    private Boolean capa;
    private Integer ordem;

    @ManyToOne
    @JoinColumn(name = "imovel_id", nullable = false)
    private ImovelModel imovel;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FotoImovelModel that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(nomeArquivo, that.nomeArquivo) && Objects.equals(caminho, that.caminho) && Objects.equals(capa, that.capa) && Objects.equals(ordem, that.ordem) && Objects.equals(imovel, that.imovel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nomeArquivo, caminho, capa, ordem, imovel);
    }
}
