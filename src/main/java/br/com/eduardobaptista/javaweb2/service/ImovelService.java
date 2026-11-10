package br.com.eduardobaptista.javaweb2.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import br.com.eduardobaptista.javaweb2.dto.ImovelSaberMaisDTO;
import br.com.eduardobaptista.javaweb2.model.FotoImovelModel;
import br.com.eduardobaptista.javaweb2.model.ImovelModel;
import br.com.eduardobaptista.javaweb2.repository.FotoImovelRepository;
import br.com.eduardobaptista.javaweb2.repository.ImovelRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
public class ImovelService {
    // capa primeiro, depois pela ordem (nulos no fim) e por fim pelo id
    private static final Comparator<FotoImovelModel> ORDEM_FOTOS = Comparator
            .comparing((FotoImovelModel f) -> !Boolean.TRUE.equals(f.getCapa()))
            .thenComparing(FotoImovelModel::getOrdem, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(FotoImovelModel::getId);

    private final ImovelRepository repository;
    private final FotoImovelRepository fotoImovelRepository;
    private final EmailService emailService;

    public ImovelService(ImovelRepository repository, FotoImovelRepository fotoImovelRepository,
                         EmailService emailService) {
        this.repository = repository;
        this.fotoImovelRepository = fotoImovelRepository;
        this.emailService = emailService;
    }

    public Page<ImovelModel> getAll(Pageable pageable) {
        Page<ImovelModel> page = repository.findAll(pageable);
        preencherFotos(page.getContent());
        return page;
    }

    public ImovelModel find(Integer id) {
        Optional<ImovelModel> model = repository.findById(id);
        return model.orElse(null);
    }

    // igual ao find, mas já com as URLs das fotos (usado nas respostas da API)
    public ImovelModel findComFotos(Integer id) {
        ImovelModel model = find(id);
        if (model != null)
            preencherFotos(List.of(model));
        return model;
    }

    // busca as fotos de todos os imóveis numa única consulta e preenche o campo transiente "fotos"
    private void preencherFotos(List<ImovelModel> imoveis) {
        if (imoveis.isEmpty())
            return;

        List<Integer> ids = imoveis.stream().map(ImovelModel::getId).toList();
        Map<Integer, List<String>> urlsPorImovel = fotoImovelRepository.findByImovelIdIn(ids).stream()
                .sorted(ORDEM_FOTOS)
                .collect(Collectors.groupingBy(f -> f.getImovel().getId(),
                        Collectors.mapping(this::urlDaFoto, Collectors.toList())));

        imoveis.forEach(i -> i.setFotos(urlsPorImovel.getOrDefault(i.getId(), List.of())));
    }

    // URL absoluta servida pelo WebConfig: <host>/uploads/{imovelId}/{nomeArquivo}
    private String urlDaFoto(FotoImovelModel foto) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/{imovelId}/{arquivo}")
                .buildAndExpand(foto.getImovel().getId(), foto.getNomeArquivo())
                .encode()
                .toUriString();
    }

    public ImovelModel insert(ImovelModel model) {
        return repository.save(model);
    }

    public ImovelModel update(ImovelModel model) {
        try {
            if (find(model.getId()) != null) {
                return repository.save(model);
            }
            return null;
        } catch (Exception e) {
            return null;
        }

    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }

    public void enviarEmailSaberMais(Integer id, ImovelSaberMaisDTO dto) throws MessagingException {
        ImovelModel model = this.find(id);

        Map<String, Object> variaveis = new HashMap<>(dto.toMap());
        variaveis.put("imovel", model);

        emailService.enviarEmailComTemplate(
                "aristimdudu@gmail.com",
                "Interesse no imóvel \"" + model.getTitulo() + "\"",
                "email/template",
                variaveis
        );
    }
}

