package br.com.eduardobaptista.javaweb2.dto;

import br.com.eduardobaptista.javaweb2.model.ContratoModel;
import br.com.eduardobaptista.javaweb2.model.OrigemContrato;
import br.com.eduardobaptista.javaweb2.model.StatusContrato;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;

// O caminho no disco não é exposto: o PDF é baixado por urlPdf (GET /contratos/{id}/pdf, autenticado).
public record ContratoDTO(
        Integer id,
        OfertaDTO oferta,
        StatusContrato status,
        OrigemContrato origem,
        String nomeArquivo,
        String urlPdf,
        LocalDateTime dataAprovacao,
        LocalDateTime dataAssinatura,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public ContratoDTO(ContratoModel c) {
        this(c.getId(), new OfertaDTO(c.getOferta()), c.getStatus(), c.getOrigem(), c.getNomeArquivo(), urlPdf(c.getId()),
                c.getDataAprovacao(), c.getDataAssinatura(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private static String urlPdf(Integer id) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/contratos/{id}/pdf").buildAndExpand(id).toUriString();
    }
}
