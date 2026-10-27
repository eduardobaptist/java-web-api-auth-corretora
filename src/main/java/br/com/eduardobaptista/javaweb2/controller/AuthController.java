package br.com.eduardobaptista.javaweb2.controller;

import br.com.eduardobaptista.javaweb2.dto.LoginRequestDTO;
import br.com.eduardobaptista.javaweb2.dto.LoginResponseDTO;
import br.com.eduardobaptista.javaweb2.dto.UserDTO;
import br.com.eduardobaptista.javaweb2.model.UserModel;
import br.com.eduardobaptista.javaweb2.repository.UserRepository;
import br.com.eduardobaptista.javaweb2.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO dto) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.email(), dto.senha()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("E-mail ou senha inválidos.");
        }

        UserModel user = userRepository.findByEmail(dto.email()).orElseThrow();

        return ResponseEntity.ok(new LoginResponseDTO(tokenService.gerarToken(user)));
    }

    // dados do usuário logado (qualquer role)
    @GetMapping("/me")
    public ResponseEntity<UserDTO> me(@AuthenticationPrincipal Jwt jwt) {
        return userRepository.findByEmail(jwt.getSubject())
                .map(user -> ResponseEntity.ok(new UserDTO(user)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
