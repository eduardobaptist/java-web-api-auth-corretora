package br.com.eduardobaptista.javaweb2.dto;

import br.com.eduardobaptista.javaweb2.model.ImovelModel;
import br.com.eduardobaptista.javaweb2.model.StatusImovel;

import java.math.BigDecimal;

// dados mínimos do imóvel exibidos dentro de ofertas e contratos
public record ImovelResumoDTO(Integer id, String titulo, BigDecimal precoVenda, BigDecimal precoAluguel,
                              StatusImovel status) {

    public ImovelResumoDTO(ImovelModel imovel) {
        this(imovel.getId(), imovel.getTitulo(), imovel.getPrecoVenda(), imovel.getPrecoAluguel(), imovel.getStatus());
    }
}
