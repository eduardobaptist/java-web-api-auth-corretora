package br.com.eduardobaptista.javaweb2.security;

import br.com.eduardobaptista.javaweb2.model.UserModel;
import br.com.eduardobaptista.javaweb2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    // o "username" do Spring Security aqui é o e-mail
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserModel user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));

        return User.withUsername(user.getEmail())
                .password(user.getSenha())
                .roles(user.getRole().name())
                .build();
    }
}
