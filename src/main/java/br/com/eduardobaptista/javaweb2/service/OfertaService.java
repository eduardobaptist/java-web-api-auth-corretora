package br.com.eduardobaptista.javaweb2.service;

import br.com.eduardobaptista.javaweb2.dto.OfertaRequestDTO;
import br.com.eduardobaptista.javaweb2.exception.ConflitoException;
import br.com.eduardobaptista.javaweb2.exception.ValidacaoException;
import br.com.eduardobaptista.javaweb2.model.ImovelModel;
import br.com.eduardobaptista.javaweb2.model.OfertaModel;
import br.com.eduardobaptista.javaweb2.model.StatusImovel;
import br.com.eduardobaptista.javaweb2.model.StatusOferta;
import br.com.eduardobaptista.javaweb2.model.UserModel;
import br.com.eduardobaptista.javaweb2.repository.ImovelRepository;
import br.com.eduardobaptista.javaweb2.repository.OfertaRepository;
import br.com.eduardobaptista.javaweb2.repository.UserRepository;
import br.com.eduardobaptista.javaweb2.security.UsuarioLogado;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Service
public class OfertaService {
    // ofertas em que o admin ainda pode decidir e o cliente ainda pode cancelar
    public static final Set<StatusOferta> EM_ABERTO = EnumSet.of(StatusOferta.PENDENTE, StatusOferta.EM_ESPERA);

    // status que o admin pode definir em PATCH /ofertas/{id}/status
    private static final Set<StatusOferta> DECISOES_DO_ADMIN =
            EnumSet.of(StatusOferta.ACEITA, StatusOferta.EM_ESPERA, StatusOferta.RECUSADA);

    @Autowired
    private OfertaRepository repository;

    @Autowired
    private ImovelRepository imovelRepository;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // Trava a linha do imóvel (SELECT ... FOR UPDATE) e relê imóvel e oferta já com o lock. Assim, duas
    // operações simultâneas no mesmo imóvel (ex.: aceitar duas ofertas) rodam uma depois da outra, e a segunda
    // enxerga o que a primeira gravou e responde 409. Precisa de READ_COMMITTED: no REPEATABLE READ o MariaDB
    // recusa a leitura com lock de uma linha alterada após o início da transação ("Record has changed since last read").
    // Usado também pelo ContratoService.
    ImovelModel travarImovel(OfertaModel oferta) {
        ImovelModel imovel = imovelRepository.findByIdParaAtualizar(oferta.getImovel().getId()).orElseThrow();
        entityManager.refresh(imovel);
        entityManager.refresh(oferta);
        return imovel;
    }

    @Transactional
    public OfertaModel criar(OfertaRequestDTO dto, Integer usuarioId) {
        UserModel usuario = userRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário do token não existe mais."));
        ImovelModel imovel = imovelRepository.findById(dto.imovelId())
                .orElseThrow(() -> new ValidacaoException("imovelId", "Imóvel não encontrado."));
        if (imovel.getStatus() != StatusImovel.DISPONIVEL)
            throw new ConflitoException("imovelId", "Este imóvel não está disponível para ofertas.");

        if (repository.existsByUsuarioIdAndImovelIdAndStatusIn(usuarioId, imovel.getId(), EM_ABERTO))
            throw new ConflitoException("imovelId", "Você já tem uma oferta em aberto para este imóvel.");

        OfertaModel oferta = new OfertaModel();
        oferta.setImovel(imovel);
        oferta.setUsuario(usuario);
        oferta.setTipo(dto.tipo());
        oferta.setValorProposto(dto.valorProposto());
        oferta.setMensagem(dto.mensagem() == null || dto.mensagem().isBlank() ? null : dto.mensagem().trim());
        oferta.setStatus(StatusOferta.PENDENTE);
        return repository.save(oferta);
    }

    public Page<OfertaModel> listar(StatusOferta status, Integer imovelId, Integer usuarioId, Pageable pageable) {
        return repository.buscar(status, imovelId, usuarioId, pageable);
    }

    // null se não existe ou se o usuário não pode vê-la (só o admin e o dono da oferta)
    public OfertaModel find(Integer id, UsuarioLogado usuario) {
        return repository.findById(id)
                .filter(o -> usuario.admin() || o.getUsuario().getId().equals(usuario.id()))
                .orElse(null);
    }

    // só o dono cancela, e só enquanto a oferta está em aberto. null se não existe ou não é dele.
    @Transactional
    public OfertaModel cancelar(Integer id, Integer usuarioId) {
        OfertaModel oferta = repository.findById(id)
                .filter(o -> o.getUsuario().getId().equals(usuarioId))
                .orElse(null);
        if (oferta == null)
            return null;

        if (!EM_ABERTO.contains(oferta.getStatus()))
            throw new ConflitoException("status", "Só ofertas pendentes ou em espera podem ser canceladas.");

        oferta.setStatus(StatusOferta.CANCELADA);
        return repository.save(oferta);
    }

    // decisão do admin. null se a oferta não existe.
    // Aceitar exige o imóvel DISPONIVEL: ele passa a RESERVADO e as outras ofertas PENDENTES dele vão para EM_ESPERA.
    // READ_COMMITTED + lock do imóvel + releitura: veja o comentário em travarImovel
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OfertaModel alterarStatus(Integer id, StatusOferta novoStatus) {
        if (!DECISOES_DO_ADMIN.contains(novoStatus))
            throw new ValidacaoException("status", "Status inválido. Use ACEITA, EM_ESPERA ou RECUSADA.");

        OfertaModel oferta = repository.findById(id).orElse(null);
        if (oferta == null)
            return null;
        ImovelModel imovel = travarImovel(oferta);

        if (!EM_ABERTO.contains(oferta.getStatus()))
            throw new ConflitoException("status", "Só ofertas pendentes ou em espera podem ter o status alterado.");

        if (novoStatus == StatusOferta.ACEITA) {
            if (imovel.getStatus() != StatusImovel.DISPONIVEL)
                throw new ConflitoException("status",
                        "Só é possível aceitar ofertas de imóveis disponíveis (o imóvel está " + imovel.getStatus() + ").");

            imovel.setStatus(StatusImovel.RESERVADO);
            oferta.setStatus(StatusOferta.ACEITA);
            repository.alterarStatusDasOutras(imovel.getId(), oferta.getId(),
                    EnumSet.of(StatusOferta.PENDENTE), StatusOferta.EM_ESPERA, LocalDateTime.now());
            return oferta;
        }

        oferta.setStatus(novoStatus);
        return repository.save(oferta);
    }
}
