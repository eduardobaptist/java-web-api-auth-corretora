package br.com.eduardobaptista.javaweb2.repository;

import br.com.eduardobaptista.javaweb2.model.TipoImovelModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoImovelRepository extends JpaRepository<TipoImovelModel, Integer> {
}
