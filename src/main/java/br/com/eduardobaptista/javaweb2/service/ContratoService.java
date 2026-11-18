package br.com.eduardobaptista.javaweb2.service;

import br.com.eduardobaptista.javaweb2.dto.ContratoGerarRequestDTO;
import br.com.eduardobaptista.javaweb2.exception.ConflitoException;
import br.com.eduardobaptista.javaweb2.exception.ValidacaoException;
import br.com.eduardobaptista.javaweb2.model.ContratoModel;
import br.com.eduardobaptista.javaweb2.model.ImovelModel;
import br.com.eduardobaptista.javaweb2.model.OfertaModel;
import br.com.eduardobaptista.javaweb2.model.OrigemContrato;
import br.com.eduardobaptista.javaweb2.model.StatusContrato;
import br.com.eduardobaptista.javaweb2.model.StatusImovel;
import br.com.eduardobaptista.javaweb2.model.StatusOferta;
import br.com.eduardobaptista.javaweb2.model.TipoOferta;
import br.com.eduardobaptista.javaweb2.repository.ContratoRepository;
import br.com.eduardobaptista.javaweb2.repository.OfertaRepository;
import br.com.eduardobaptista.javaweb2.security.UsuarioLogado;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
public class ContratoService {
    // contratos que impedem a criação de outro para a mesma oferta
    private static final Set<StatusContrato> ATIVOS =
            EnumSet.of(StatusContrato.AGUARDANDO_ASSINATURA, StatusContrato.ASSINADO);

    // o nome original vai para uma coluna VARCHAR(255)
    private static final int TAMANHO_MAXIMO_NOME = 150;
    private static final byte[] ASSINATURA_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    private ContratoRepository repository;

    @Autowired
    private OfertaRepository ofertaRepository;

    @Autowired
    private ContratoPdfService pdfService;

    @Autowired
    private OfertaService ofertaService;

    @PersistenceContext
    private EntityManager entityManager;

    // trava o imóvel e relê oferta e contrato (veja OfertaService.travarImovel)
    private ImovelModel travar(ContratoModel contrato) {
        ImovelModel imovel = ofertaService.travarImovel(contrato.getOferta());
        entityManager.refresh(contrato);
        return imovel;
    }

    // pasta privada: NÃO pode ficar dentro de app.upload.dir, que é servida publicamente em /uploads/**
    @Value("${app.contratos.dir:./contratos}")
    private String contratosDir;

    // admin envia um PDF pronto
    @Transactional
    public ContratoModel criarPorUpload(Integer ofertaId, MultipartFile arquivo) throws IOException {
        validarPdf(arquivo);
        OfertaModel oferta = ofertaParaContrato(ofertaId);

        String nome = Paths.get(arquivo.getOriginalFilename()).getFileName().toString();
        try (InputStream in = arquivo.getInputStream()) {
            return salvar(oferta, in, nome, OrigemContrato.UPLOAD);
        }
    }

    // a API gera o PDF a partir do template, com os dados da oferta
    @Transactional
    public ContratoModel gerar(ContratoGerarRequestDTO dto) throws IOException {
        OfertaModel oferta = ofertaParaContrato(dto.ofertaId());
        byte[] pdf = pdfService.gerar(oferta, dto);
        return salvar(oferta, new ByteArrayInputStream(pdf), ContratoPdfService.nomeArquivo(oferta), OrigemContrato.GERADO);
    }

    // gera o PDF sem salvar nada, para o admin conferir antes. Aceita oferta em qualquer status.
    public byte[] previa(ContratoGerarRequestDTO dto) throws IOException {
        OfertaModel oferta = ofertaRepository.findById(dto.ofertaId())
                .orElseThrow(() -> new ValidacaoException("ofertaId", "Oferta não encontrada."));
        return pdfService.gerar(oferta, dto);
    }

    // a oferta precisa existir, estar ACEITA e não ter contrato ativo
    private OfertaModel ofertaParaContrato(Integer ofertaId) {
        OfertaModel oferta = ofertaRepository.findById(ofertaId)
                .orElseThrow(() -> new ValidacaoException("ofertaId", "Oferta não encontrada."));
        if (oferta.getStatus() != StatusOferta.ACEITA)
            throw new ConflitoException("ofertaId", "Só é possível criar contrato para uma oferta aceita.");
        if (repository.existsByOfertaIdAndStatusIn(ofertaId, ATIVOS))
            throw new ConflitoException("ofertaId", "Esta oferta já tem um contrato aguardando assinatura ou assinado.");
        return oferta;
    }

    private ContratoModel salvar(OfertaModel oferta, InputStream pdf, String nomeArquivo, OrigemContrato origem)
            throws IOException {
        Path pasta = Paths.get(contratosDir, oferta.getId().toString());
        Files.createDirectories(pasta);
        Path destino = pasta.resolve(UUID.randomUUID() + ".pdf");
        Files.copy(pdf, destino);

        ContratoModel contrato = new ContratoModel();
        contrato.setOferta(oferta);
        contrato.setOrigem(origem);
        contrato.setNomeArquivo(nomeArquivo);
        contrato.setCaminho(destino.toString());
        contrato.setStatus(StatusContrato.AGUARDANDO_ASSINATURA);
        contrato.setDataAprovacao(LocalDateTime.now());
        try {
            return repository.saveAndFlush(contrato);
        } catch (RuntimeException e) {
            Files.deleteIfExists(destino); // não deixa o PDF órfão se o banco recusar
            throw e;
        }
    }

    // valida o arquivo pelo Content-Type declarado E pelos primeiros bytes ("%PDF-")
    private void validarPdf(MultipartFile arquivo) throws IOException {
        if (arquivo.isEmpty())
            throw new ValidacaoException("arquivo", "O arquivo é obrigatório e não pode estar vazio.");

        String nome = arquivo.getOriginalFilename();
        if (nome == null || nome.isBlank())
            throw new ValidacaoException("arquivo", "O arquivo precisa ter um nome.");
        if (nome.length() > TAMANHO_MAXIMO_NOME)
            throw new ValidacaoException("arquivo", "O nome do arquivo deve ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres.");

        byte[] inicio;
        try (InputStream in = arquivo.getInputStream()) {
            inicio = in.readNBytes(ASSINATURA_PDF.length);
        }
        if (!"application/pdf".equals(arquivo.getContentType()) || !Arrays.equals(inicio, ASSINATURA_PDF))
            throw new ValidacaoException("arquivo", "Formato inválido. Envie um arquivo PDF.");
    }

    public Page<ContratoModel> listar(StatusContrato status, Integer ofertaId, Integer usuarioId, Pageable pageable) {
        return repository.buscar(status, ofertaId, usuarioId, pageable);
    }

    // null se não existe ou se o usuário não pode vê-lo (só o admin e o cliente da oferta)
    public ContratoModel find(Integer id, UsuarioLogado usuario) {
        return repository.findById(id)
                .filter(c -> usuario.admin() || c.getOferta().getUsuario().getId().equals(usuario.id()))
                .orElse(null);
    }

    // o PDF do contrato, ou null se o arquivo sumiu do disco
    public Resource arquivo(ContratoModel contrato) {
        Resource resource = new FileSystemResource(contrato.getCaminho());
        return resource.exists() ? resource : null;
    }

    // só o cliente da oferta assina. null se não existe ou não é dele.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ContratoModel assinar(Integer id, Integer usuarioId) {
        ContratoModel contrato = repository.findById(id)
                .filter(c -> c.getOferta().getUsuario().getId().equals(usuarioId))
                .orElse(null);
        if (contrato == null)
            return null;
        ImovelModel imovel = travar(contrato);

        if (contrato.getStatus() != StatusContrato.AGUARDANDO_ASSINATURA)
            throw new ConflitoException("status", "Só contratos aguardando assinatura podem ser assinados.");

        contrato.setStatus(StatusContrato.ASSINADO);
        contrato.setDataAssinatura(LocalDateTime.now());

        // imóvel ALUGADO/VENDIDO e as demais ofertas em aberto dele são recusadas
        OfertaModel oferta = contrato.getOferta();
        imovel.setStatus(oferta.getTipo() == TipoOferta.ALUGUEL ? StatusImovel.ALUGADO : StatusImovel.VENDIDO);
        ofertaRepository.alterarStatusDasOutras(imovel.getId(), oferta.getId(),
                OfertaService.EM_ABERTO, StatusOferta.RECUSADA, LocalDateTime.now());
        return contrato;
    }

    // admin (cliente desistiu). null se não existe. Contrato assinado não pode ser cancelado.
    // A oferta vira CANCELADA, o imóvel volta a DISPONIVEL (se ainda estiver RESERVADO; um status trocado
    // manualmente pelo admin é mantido) e as ofertas EM_ESPERA dele voltam a PENDENTE.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ContratoModel cancelar(Integer id) {
        ContratoModel contrato = repository.findById(id).orElse(null);
        if (contrato == null)
            return null;
        ImovelModel imovel = travar(contrato);

        if (contrato.getStatus() != StatusContrato.AGUARDANDO_ASSINATURA)
            throw new ConflitoException("status", "Só contratos aguardando assinatura podem ser cancelados.");

        contrato.setStatus(StatusContrato.CANCELADO);

        OfertaModel oferta = contrato.getOferta();
        oferta.setStatus(StatusOferta.CANCELADA);
        if (imovel.getStatus() == StatusImovel.RESERVADO)
            imovel.setStatus(StatusImovel.DISPONIVEL);
        ofertaRepository.alterarStatusDasOutras(imovel.getId(), oferta.getId(),
                EnumSet.of(StatusOferta.EM_ESPERA), StatusOferta.PENDENTE, LocalDateTime.now());
        return contrato;
    }
}
