package br.com.eduardobaptista.javaweb2.service;

import br.com.eduardobaptista.javaweb2.model.TipoImovelModel;
import br.com.eduardobaptista.javaweb2.repository.TipoImovelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TipoImovelService {

    @Autowired
    private TipoImovelRepository repository;

    public List<TipoImovelModel> getAll() {
        List<TipoImovelModel> list = repository.findAll();
        return list;
    }

    public Page<TipoImovelModel> getAll(Pageable pageable) {
        Page<TipoImovelModel> list = repository.findAll(pageable);
        return list;
    }

    public TipoImovelModel find(Integer id) {
        Optional<TipoImovelModel> model = repository.findById(id);
        return model.orElse(null);
    }

    public TipoImovelModel insert(TipoImovelModel model) {
        return repository.save(model);
    }

    public TipoImovelModel update(TipoImovelModel model) {
        try {
            if(find(model.getId())!=null){
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


}
