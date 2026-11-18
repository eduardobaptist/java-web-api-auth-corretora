package br.com.eduardobaptista.javaweb2.repository;

import br.com.eduardobaptista.javaweb2.model.FavoritoModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface FavoritoRepository extends JpaRepository<FavoritoModel, Integer> {

    boolean existsByUsuarioIdAndImovelId(Integer usuarioId, Integer imovelId);

    @Transactional
    @Modifying
    @Query("delete from FavoritoModel f where f.usuario.id = :usuarioId and f.imovel.id = :imovelId")
    int excluir(@Param("usuarioId") Integer usuarioId, @Param("imovelId") Integer imovelId);
}
