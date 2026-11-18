package br.com.eduardobaptista.javaweb2.repository;

import br.com.eduardobaptista.javaweb2.model.OfertaModel;
import br.com.eduardobaptista.javaweb2.model.StatusOferta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;

public interface OfertaRepository extends JpaRepository<OfertaModel, Integer> {

    // filtros opcionais (null = não filtra); imóvel e usuário vêm na mesma consulta
    @EntityGraph(attributePaths = {"imovel", "usuario"})
    @Query("""
            select o from OfertaModel o
            where (:status is null or o.status = :status)
              and (:imovelId is null or o.imovel.id = :imovelId)
              and (:usuarioId is null or o.usuario.id = :usuarioId)
            """)
    Page<OfertaModel> buscar(@Param("status") StatusOferta status,
                             @Param("imovelId") Integer imovelId,
                             @Param("usuarioId") Integer usuarioId,
                             Pageable pageable);

    // muda em lote o status das outras ofertas do imóvel (o @UpdateTimestamp não roda em update em lote)
    // sem clearAutomatically: limpar a sessão desanexa a oferta e o DTO não consegue carregar o usuário (lazy)
    @Modifying(flushAutomatically = true)
    @Query("""
            update OfertaModel o set o.status = :novo, o.updatedAt = :agora
            where o.imovel.id = :imovelId and o.status in :de and o.id <> :exceto
            """)
    int alterarStatusDasOutras(@Param("imovelId") Integer imovelId,
                               @Param("exceto") Integer exceto,
                               @Param("de") Collection<StatusOferta> de,
                               @Param("novo") StatusOferta novo,
                               @Param("agora") LocalDateTime agora);

    boolean existsByUsuarioIdAndImovelIdAndStatusIn(Integer usuarioId, Integer imovelId, Collection<StatusOferta> status);
}
