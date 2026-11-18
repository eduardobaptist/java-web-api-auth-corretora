package br.com.eduardobaptista.javaweb2.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// Contrato (PDF) gerado pelo admin para uma oferta aceita. Uma oferta pode ter vários contratos ao longo do
// tempo, mas só um ativo (AGUARDANDO_ASSINATURA ou ASSINADO); cancelado, o admin pode enviar outro.
// É serializado pelo ContratoDTO.
@Entity
@Table(name = "contratos")
@Getter
@Setter
@NoArgsConstructor
public class ContratoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oferta_id", nullable = false)
    private OfertaModel oferta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrigemContrato origem;

    // nome do download: o original no upload, "contrato-oferta-{id}.pdf" no gerado
    @Column(name = "nome_arquivo", nullable = false)
    private String nomeArquivo;

    // caminho no disco, dentro de ${app.contratos.dir} (pasta privada, fora de /uploads)
    @Column(nullable = false)
    private String caminho;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusContrato status;

    // momento em que o admin aprovou a oferta gerando o contrato
    @Column(name = "data_aprovacao", nullable = false)
    private LocalDateTime dataAprovacao;

    @Column(name = "data_assinatura")
    private LocalDateTime dataAssinatura;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
