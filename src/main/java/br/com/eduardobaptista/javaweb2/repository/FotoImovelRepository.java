package br.com.eduardobaptista.javaweb2.repository;

import br.com.eduardobaptista.javaweb2.model.FotoImovelModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FotoImovelRepository extends JpaRepository<FotoImovelModel, Integer> {

    List<FotoImovelModel> findByImovelId(Integer imovelId);

}