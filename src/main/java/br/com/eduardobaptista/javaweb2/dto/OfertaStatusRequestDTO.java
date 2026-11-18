package br.com.eduardobaptista.javaweb2.dto;

import br.com.eduardobaptista.javaweb2.model.StatusOferta;
import jakarta.validation.constraints.NotNull;

// decisão do admin sobre a oferta: ACEITA, EM_ESPERA ou RECUSADA (as demais são recusadas no service)
public record OfertaStatusRequestDTO(
        @NotNull(message = "O status é obrigatório.")
        StatusOferta status
) {
}
