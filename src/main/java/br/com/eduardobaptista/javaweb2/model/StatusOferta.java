package br.com.eduardobaptista.javaweb2.model;

// PENDENTE e EM_ESPERA são "em aberto": o admin ainda pode decidir e o cliente ainda pode cancelar.
// ACEITA, RECUSADA e CANCELADA são finais.
public enum StatusOferta {
    PENDENTE,
    ACEITA,
    EM_ESPERA,
    RECUSADA,
    CANCELADA
}
