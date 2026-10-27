package br.com.eduardobaptista.javaweb2.service;

import br.com.eduardobaptista.javaweb2.dto.UserRequestDTO;
import br.com.eduardobaptista.javaweb2.model.Role;
import br.com.eduardobaptista.javaweb2.model.UserModel;
import br.com.eduardobaptista.javaweb2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<UserModel> getAll() {
        return userRepository.findAll();
    }

    public UserModel getById(Integer id) {
        Optional<UserModel> user = userRepository.findById(id);
        return user.orElse(null);
    }

    public UserModel create(UserRequestDTO dto) {
        if (userRepository.existsByEmail(dto.email()))
            throw new IllegalArgumentException("E-mail já cadastrado.");

        UserModel user = new UserModel();
        user.setNome(dto.nome());
        user.setEmail(dto.email());
        user.setSenha(passwordEncoder.encode(dto.senha()));
        user.setRole(dto.role() != null ? dto.role() : Role.BASIC);

        return userRepository.save(user);
    }

    public UserModel update(Integer id, UserRequestDTO dto) {
        UserModel userUpdate = this.getById(id);

        if (userUpdate == null)
            return null;

        Optional<UserModel> emailOwner = userRepository.findByEmail(dto.email());
        if (emailOwner.isPresent() && !emailOwner.get().getId().equals(id))
            throw new IllegalArgumentException("E-mail já cadastrado.");

        userUpdate.setNome(dto.nome());
        userUpdate.setEmail(dto.email());
        // senha só é alterada quando enviada
        if (dto.senha() != null && !dto.senha().isBlank())
            userUpdate.setSenha(passwordEncoder.encode(dto.senha()));
        if (dto.role() != null)
            userUpdate.setRole(dto.role());

        return userRepository.save(userUpdate);
    }

    public void delete(Integer id) {
        userRepository.deleteById(id);
    }
}
