package br.com.eduardobaptista.javaweb2.repository;

import br.com.eduardobaptista.javaweb2.model.ContratoModel;
import br.com.eduardobaptista.javaweb2.model.StatusContrato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface ContratoRepository extends JpaRepository<ContratoModel, Integer> {

    // filtros opcionais (null = não filtra); oferta, imóvel e cliente vêm na mesma consulta
    @EntityGraph(attributePaths = {"oferta", "oferta.imovel", "oferta.usuario"})
    @Query("""
            select c from ContratoModel c
            where (:status is null or c.status = :status)
              and (:ofertaId is null or c.oferta.id = :ofertaId)
              and (:usuarioId is null or c.oferta.usuario.id = :usuarioId)
            """)
    Page<ContratoModel> buscar(@Param("status") StatusContrato status,
                               @Param("ofertaId") Integer ofertaId,
                               @Param("usuarioId") Integer usuarioId,
                               Pageable pageable);

    boolean existsByOfertaIdAndStatusIn(Integer ofertaId, Collection<StatusContrato> status);
}
