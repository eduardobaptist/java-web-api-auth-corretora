package br.com.eduardobaptista.javaweb2.service;

import br.com.eduardobaptista.javaweb2.model.FavoritoModel;
import br.com.eduardobaptista.javaweb2.model.ImovelModel;
import br.com.eduardobaptista.javaweb2.repository.FavoritoRepository;
import br.com.eduardobaptista.javaweb2.repository.ImovelRepository;
import br.com.eduardobaptista.javaweb2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FavoritoService {

    @Autowired
    private FavoritoRepository repository;

    @Autowired
    private ImovelRepository imovelRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImovelService imovelService;

    // idempotente: favoritar de novo não faz nada. false se o imóvel não existe.
    public boolean favoritar(Integer usuarioId, Integer imovelId) {
        if (!imovelRepository.existsById(imovelId))
            return false;
        if (!userRepository.existsById(usuarioId))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário do token não existe mais.");
        if (repository.existsByUsuarioIdAndImovelId(usuarioId, imovelId))
            return true;

        try {
            repository.save(new FavoritoModel(userRepository.getReferenceById(usuarioId),
                    imovelRepository.getReferenceById(imovelId)));
        } catch (DataIntegrityViolationException e) {
            // duas requisições simultâneas: a outra já gravou (unique usuario_id + imovel_id)
        }
        return true;
    }

    // idempotente: desfavoritar algo que não é favorito também responde sucesso
    public void desfavoritar(Integer usuarioId, Integer imovelId) {
        repository.excluir(usuarioId, imovelId);
    }

    public boolean isFavorito(Integer usuarioId, Integer imovelId) {
        return repository.existsByUsuarioIdAndImovelId(usuarioId, imovelId);
    }

    // imóveis favoritos do usuário, com as URLs das fotos. null se o usuário não existe.
    public Page<ImovelModel> listar(Integer usuarioId, Pageable pageable) {
        if (!userRepository.existsById(usuarioId))
            return null;
        return imovelService.getFavoritos(usuarioId, pageable);
    }
}
