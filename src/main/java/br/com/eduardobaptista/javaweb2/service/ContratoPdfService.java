package br.com.eduardobaptista.javaweb2.service;

import br.com.eduardobaptista.javaweb2.dto.ContratoGerarRequestDTO;
import br.com.eduardobaptista.javaweb2.exception.ValidacaoException;
import br.com.eduardobaptista.javaweb2.model.BairroModel;
import br.com.eduardobaptista.javaweb2.model.ImovelModel;
import br.com.eduardobaptista.javaweb2.model.OfertaModel;
import br.com.eduardobaptista.javaweb2.model.TipoOferta;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Monta o PDF do contrato: preenche o template Thymeleaf templates/contrato/contrato.html com os dados da
// oferta e converte o HTML em PDF com o OpenHTMLtoPDF. O template precisa ser XHTML bem formado
// (tags fechadas, sem entidades como &nbsp;).
@Service
public class ContratoPdfService {
    public static final int PRAZO_PADRAO_MESES = 30;
    public static final int DIA_VENCIMENTO_PADRAO = 10;
    public static final String FORMA_PAGAMENTO_PADRAO = "conforme acordado entre as partes";

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT_BR);
    private static final DateTimeFormatter DATA_EXTENSO = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", PT_BR);

    private final TemplateEngine templateEngine;

    // quem aparece como CONTRATADA no contrato
    @Value("${app.contratos.empresa:Corretora de Imóveis}")
    private String empresa;

    public ContratoPdfService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    // regras que dependem da oferta (prazo e vencimento só fazem sentido em aluguel)
    public void validar(OfertaModel oferta, ContratoGerarRequestDTO dto) {
        if (oferta.getTipo() == TipoOferta.VENDA) {
            Map<String, String> erros = new HashMap<>();
            if (dto.prazoMeses() != null)
                erros.put("prazoMeses", "O prazo só se aplica a ofertas de aluguel.");
            if (dto.diaVencimento() != null)
                erros.put("diaVencimento", "O dia de vencimento só se aplica a ofertas de aluguel.");
            if (!erros.isEmpty())
                throw new ValidacaoException(erros);
        }
    }

    public byte[] gerar(OfertaModel oferta, ContratoGerarRequestDTO dto) throws IOException {
        validar(oferta, dto);

        Context context = new Context(PT_BR);
        context.setVariables(variaveis(oferta, dto));
        String html = templateEngine.process("contrato/contrato", context);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        }
    }

    public static String nomeArquivo(OfertaModel oferta) {
        return "contrato-oferta-" + oferta.getId() + ".pdf";
    }

    private Map<String, Object> variaveis(OfertaModel oferta, ContratoGerarRequestDTO dto) {
        ImovelModel imovel = oferta.getImovel();
        boolean aluguel = oferta.getTipo() == TipoOferta.ALUGUEL;
        LocalDateTime agora = LocalDateTime.now();

        Map<String, Object> v = new HashMap<>();
        v.put("empresa", empresa);
        v.put("aluguel", aluguel);
        v.put("titulo", aluguel ? "Contrato de Locação de Imóvel" : "Contrato de Compra e Venda de Imóvel");
        v.put("papelCliente", aluguel ? "LOCATÁRIO(A)" : "COMPRADOR(A)");
        v.put("ofertaId", oferta.getId());
        v.put("clienteNome", oferta.getUsuario().getNome());
        v.put("clienteEmail", oferta.getUsuario().getEmail());
        v.put("imovelTitulo", imovel.getTitulo());
        v.put("imovelTipo", imovel.getTipoImovel() != null ? imovel.getTipoImovel().getNome() : null);
        v.put("imovelEndereco", endereco(imovel));
        v.put("imovelAreaTotal", imovel.getAreaTotal());
        v.put("imovelAreaConstruida", imovel.getAreaConstruida());
        v.put("valor", moeda(oferta.getValorProposto()));
        v.put("formaPagamento", textoOuPadrao(dto.formaPagamento(), FORMA_PAGAMENTO_PADRAO));
        v.put("prazoMeses", Objects.requireNonNullElse(dto.prazoMeses(), PRAZO_PADRAO_MESES));
        v.put("diaVencimento", Objects.requireNonNullElse(dto.diaVencimento(), DIA_VENCIMENTO_PADRAO));
        v.put("clausulasAdicionais", textoOuPadrao(dto.clausulasAdicionais(), null));
        v.put("dataOferta", oferta.getCreatedAt() != null ? oferta.getCreatedAt().format(DATA) : null);
        v.put("dataEmissao", agora.format(DATA));
        v.put("dataExtenso", agora.format(DATA_EXTENSO));
        v.put("cidade", imovel.getBairro() != null ? imovel.getBairro().getCidade() : null);
        return v;
    }

    // "Rua X, 123, Apto 4 - Centro, Cidade/UF - CEP 00000-000" (pula as partes vazias)
    private static String endereco(ImovelModel i) {
        String rua = juntar(", ", i.getEndereco(), i.getNumero(), i.getComplemento());
        BairroModel b = i.getBairro();
        String local = b == null ? null
                : juntar(", ", b.getNome(), juntar("/", b.getCidade(), b.getEstado()));
        String cep = i.getCep() != null ? "CEP " + i.getCep() : null;
        String texto = juntar(" - ", rua, local, cep);
        return texto.isEmpty() ? "não informado" : texto;
    }

    private static String juntar(String separador, String... partes) {
        return Stream.of(partes).filter(p -> p != null && !p.isBlank()).map(String::trim)
                .collect(Collectors.joining(separador));
    }

    private static String moeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor);
    }

    private static String textoOuPadrao(String texto, String padrao) {
        return texto == null || texto.isBlank() ? padrao : texto.trim();
    }
}
