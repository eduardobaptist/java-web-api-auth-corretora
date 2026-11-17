package br.com.eduardobaptista.javaweb2.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "imoveis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImovelModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // limites abaixo seguem as colunas: VARCHAR(255), DECIMAL(15,2)/(10,2), VARCHAR(9).
    // TEXT guarda 65.535 bytes; 16.000 caracteres cabem mesmo com 4 bytes por caractere (utf8mb4).
    @NotBlank(message = "O título é obrigatório.")
    @Size(max = 255, message = "O título deve ter no máximo 255 caracteres.")
    private String titulo;

    @Size(max = 16000, message = "A descrição deve ter no máximo 16000 caracteres.")
    @Column(columnDefinition = "TEXT")
    private String descricao;

    @PositiveOrZero(message = "O preço de venda não pode ser negativo.")
    @Digits(integer = 13, fraction = 2, message = "O preço de venda deve ter até 13 dígitos inteiros e 2 casas decimais.")
    @Column(name = "preco_venda", precision = 15, scale = 2)
    private BigDecimal precoVenda;

    @PositiveOrZero(message = "O preço de aluguel não pode ser negativo.")
    @Digits(integer = 13, fraction = 2, message = "O preço de aluguel deve ter até 13 dígitos inteiros e 2 casas decimais.")
    @Column(name = "preco_aluguel", precision = 15, scale = 2)
    private BigDecimal precoAluguel;

    @Size(max = 255, message = "A finalidade deve ter no máximo 255 caracteres.")
    private String finalidade;

    @Size(max = 255, message = "O status deve ter no máximo 255 caracteres.")
    private String status;

    @PositiveOrZero(message = "O número de dormitórios não pode ser negativo.")
    private Integer dormitorios;

    @PositiveOrZero(message = "O número de banheiros não pode ser negativo.")
    private Integer banheiros;

    @PositiveOrZero(message = "O número de vagas de garagem não pode ser negativo.")
    private Integer garagem;

    @PositiveOrZero(message = "A área total não pode ser negativa.")
    @Digits(integer = 8, fraction = 2, message = "A área total deve ter até 8 dígitos inteiros e 2 casas decimais.")
    @Column(name = "area_total", precision = 10, scale = 2)
    private BigDecimal areaTotal;

    @PositiveOrZero(message = "A área construída não pode ser negativa.")
    @Digits(integer = 8, fraction = 2, message = "A área construída deve ter até 8 dígitos inteiros e 2 casas decimais.")
    @Column(name = "area_construida", precision = 10, scale = 2)
    private BigDecimal areaConstruida;

    @Size(max = 255, message = "O endereço deve ter no máximo 255 caracteres.")
    private String endereco;

    @Size(max = 255, message = "O número deve ter no máximo 255 caracteres.")
    private String numero;

    @Size(max = 255, message = "O complemento deve ter no máximo 255 caracteres.")
    private String complemento;

    @Pattern(regexp = "\\d{5}-\\d{3}", message = "CEP inválido. Use o formato 00000-000.")
    @Column(length = 9)
    private String cep;

    @Size(max = 16000, message = "As características devem ter no máximo 16000 caracteres.")
    @Column(columnDefinition = "TEXT")
    private String caracteristicas;

    // a coluna é NOT NULL: omitido ou null no JSON vira false (ImovelService.normalizar)
    @Column(nullable = false)
    @Builder.Default
    private Boolean destaque = false;

    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_imovel_id")
    private TipoImovelModel tipoImovel;

    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bairro_id")
    private BairroModel bairro;

    // URLs públicas das fotos (capa primeiro). Não é coluna: é preenchido pelo ImovelService
    // nas consultas e ignorado quando enviado no corpo de POST/PUT.
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<String> fotos;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}