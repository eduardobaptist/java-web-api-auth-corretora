package br.com.eduardobaptista.javaweb2.repository;

import br.com.eduardobaptista.javaweb2.model.ImovelModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface  ImovelRepository extends JpaRepository<ImovelModel, Integer>{

}
