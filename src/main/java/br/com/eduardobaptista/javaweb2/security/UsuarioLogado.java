package br.com.eduardobaptista.javaweb2.security;

import br.com.eduardobaptista.javaweb2.model.Role;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// Id e perfil do usuário do token (claims "id" e "roles" geradas pelo TokenService).
public record UsuarioLogado(Integer id, boolean admin) {

    public static UsuarioLogado de(Jwt jwt) {
        if (!(jwt.getClaim("id") instanceof Number id))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token sem o id do usuário.");

        List<String> roles = jwt.getClaimAsStringList("roles");
        return new UsuarioLogado(id.intValue(), roles != null && roles.contains(Role.ADMIN.name()));
    }
}
