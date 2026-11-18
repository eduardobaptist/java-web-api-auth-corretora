package br.com.eduardobaptista.javaweb2.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

// Imóvel favoritado por um usuário. As FKs têm ON DELETE CASCADE: excluir o imóvel ou o usuário
// remove os favoritos junto.
@Entity
@Table(name = "favoritos",
        uniqueConstraints = @UniqueConstraint(name = "uk_favorito_usuario_imovel", columnNames = {"usuario_id", "imovel_id"}))
@Getter
@Setter
@NoArgsConstructor
public class FavoritoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserModel usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "imovel_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ImovelModel imovel;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public FavoritoModel(UserModel usuario, ImovelModel imovel) {
        this.usuario = usuario;
        this.imovel = imovel;
    }
}
