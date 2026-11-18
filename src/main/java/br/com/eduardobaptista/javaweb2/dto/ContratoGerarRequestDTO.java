package br.com.eduardobaptista.javaweb2.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Dados para a API gerar o PDF do contrato. Cliente, imóvel, tipo e valor vêm da oferta; os campos abaixo
// completam o texto (todos opcionais, com padrão no ContratoPdfService).
public record ContratoGerarRequestDTO(
        @NotNull(message = "A oferta é obrigatória.")
        Integer ofertaId,

        // só para ALUGUEL
        @Min(value = 1, message = "O prazo deve ser de 1 a 360 meses.")
        @Max(value = 360, message = "O prazo deve ser de 1 a 360 meses.")
        Integer prazoMeses,

        // só para ALUGUEL; até 28 para existir em todos os meses
        @Min(value = 1, message = "O dia de vencimento deve ser de 1 a 28.")
        @Max(value = 28, message = "O dia de vencimento deve ser de 1 a 28.")
        Integer diaVencimento,

        @Size(max = 500, message = "A forma de pagamento deve ter no máximo 500 caracteres.")
        String formaPagamento,

        @Size(max = 4000, message = "As cláusulas adicionais devem ter no máximo 4000 caracteres.")
        String clausulasAdicionais
) {
}
