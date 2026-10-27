package br.com.eduardobaptista.javaweb2.repository;


import br.com.eduardobaptista.javaweb2.model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserModel, Integer> {

    Optional<UserModel> findByEmail(String email);

    boolean existsByEmail(String email);
}
