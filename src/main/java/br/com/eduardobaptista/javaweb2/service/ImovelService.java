package br.com.eduardobaptista.javaweb2.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.com.eduardobaptista.javaweb2.dto.ImovelSaberMaisDTO;
import br.com.eduardobaptista.javaweb2.model.ImovelModel;
import br.com.eduardobaptista.javaweb2.repository.ImovelRepository;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
public class ImovelService {
    private final ImovelRepository repository;
    private final EmailService emailService;

    public ImovelService(ImovelRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    public List<ImovelModel> getAll() {
        List<ImovelModel> list = repository.findAll();
        return list;
    }

    public ImovelModel find(Integer id) {
        Optional<ImovelModel> model = repository.findById(id);
        return model.orElse(null);
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

