package br.com.eduardobaptista.javaweb2.config;

import br.com.eduardobaptista.javaweb2.model.Role;
import br.com.eduardobaptista.javaweb2.model.UserModel;
import br.com.eduardobaptista.javaweb2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeeder {

    @Value("${app.admin.nome}")
    private String nome;

    @Value("${app.admin.email}")
    private String email;

    @Value("${app.admin.senha}")
    private String senha;

    // cria o usuário admin na inicialização, caso ainda não exista
    @Bean
    public CommandLineRunner seedAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.existsByEmail(email))
                return;

            UserModel admin = new UserModel();
            admin.setNome(nome);
            admin.setEmail(email);
            admin.setSenha(passwordEncoder.encode(senha));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);

            System.out.println("Usuário admin criado: " + email);
        };
    }
}
