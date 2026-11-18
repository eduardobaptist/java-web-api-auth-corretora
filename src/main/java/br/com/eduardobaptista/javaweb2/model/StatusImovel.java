package br.com.eduardobaptista.javaweb2.model;

// DISPONIVEL é o único que recebe ofertas. RESERVADO/ALUGADO/VENDIDO são definidos pelo fluxo de
// ofertas e contratos, mas o admin também pode trocar o status livremente no PUT /imoveis/{id}.
public enum StatusImovel {
    DISPONIVEL,
    RESERVADO,
    ALUGADO,
    VENDIDO
}
