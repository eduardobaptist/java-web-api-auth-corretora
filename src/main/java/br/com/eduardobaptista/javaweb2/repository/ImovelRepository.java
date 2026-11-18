package br.com.eduardobaptista.javaweb2.repository;

import br.com.eduardobaptista.javaweb2.model.ImovelModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface  ImovelRepository extends JpaRepository<ImovelModel, Integer>{

    // SELECT ... FOR UPDATE: serializa as mudanças de status do imóvel (ex.: duas ofertas aceitas ao mesmo tempo).
    // Nativa de propósito: o banco é MariaDB e o @Lock com MySQLDialect gera "for update of <alias>", que o
    // MariaDB não aceita.
    @Query(value = "select * from imoveis where id = :id for update", nativeQuery = true)
    Optional<ImovelModel> findByIdParaAtualizar(@Param("id") Integer id);

    // subconsulta (e não "select f.imovel") para que o sort da página use os campos do imóvel
    @Query("select i from ImovelModel i where i.id in "
            + "(select f.imovel.id from FavoritoModel f where f.usuario.id = :usuarioId)")
    Page<ImovelModel> findFavoritosDoUsuario(@Param("usuarioId") Integer usuarioId, Pageable pageable);
}
