package br.com.eduardobaptista.javaweb2.dto;

import br.com.eduardobaptista.javaweb2.model.TipoOferta;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OfertaRequestDTO(
        @NotNull(message = "O imóvel é obrigatório.")
        Integer imovelId,

        @NotNull(message = "O tipo da oferta é obrigatório.")
        TipoOferta tipo,

        // a coluna é DECIMAL(15,2), igual aos preços do imóvel
        @NotNull(message = "O valor proposto é obrigatório.")
        @Positive(message = "O valor proposto deve ser maior que zero.")
        @Digits(integer = 13, fraction = 2, message = "O valor proposto deve ter até 13 dígitos inteiros e 2 casas decimais.")
        BigDecimal valorProposto,

        @Size(max = 2000, message = "A mensagem deve ter no máximo 2000 caracteres.")
        String mensagem
) {
}
