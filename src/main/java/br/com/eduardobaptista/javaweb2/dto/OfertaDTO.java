package br.com.eduardobaptista.javaweb2.dto;

import br.com.eduardobaptista.javaweb2.model.OfertaModel;
import br.com.eduardobaptista.javaweb2.model.StatusOferta;
import br.com.eduardobaptista.javaweb2.model.TipoOferta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OfertaDTO(
        Integer id,
        ImovelResumoDTO imovel,
        UserDTO usuario,
        TipoOferta tipo,
        BigDecimal valorProposto,
        String mensagem,
        StatusOferta status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public OfertaDTO(OfertaModel o) {
        this(o.getId(), new ImovelResumoDTO(o.getImovel()), new UserDTO(o.getUsuario()), o.getTipo(),
                o.getValorProposto(), o.getMensagem(), o.getStatus(), o.getCreatedAt(), o.getUpdatedAt());
    }
}
