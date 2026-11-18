package br.com.eduardobaptista.javaweb2.controller;

import br.com.eduardobaptista.javaweb2.model.ImovelModel;
import br.com.eduardobaptista.javaweb2.security.UsuarioLogado;
import br.com.eduardobaptista.javaweb2.service.FavoritoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// favoritos do usuário logado (para os de outro usuário, o admin usa GET /user/{id}/favoritos)
@RestController
@RequestMapping("/favoritos")
public class FavoritoController {

    @Autowired
    private FavoritoService service;

    @GetMapping
    public ResponseEntity<Page<ImovelModel>> listar(@AuthenticationPrincipal Jwt jwt,
                                                    @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Page<ImovelModel> page = service.listar(UsuarioLogado.de(jwt).id(), pageable);
        if (page == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{imovelId}")
    public ResponseEntity<Map<String, Boolean>> isFavorito(@PathVariable Integer imovelId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Map.of("favorito", service.isFavorito(UsuarioLogado.de(jwt).id(), imovelId)));
    }

    @PutMapping("/{imovelId}")
    public ResponseEntity<Void> favoritar(@PathVariable Integer imovelId, @AuthenticationPrincipal Jwt jwt) {
        if (!service.favoritar(UsuarioLogado.de(jwt).id(), imovelId))
            return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{imovelId}")
    public ResponseEntity<Void> desfavoritar(@PathVariable Integer imovelId, @AuthenticationPrincipal Jwt jwt) {
        service.desfavoritar(UsuarioLogado.de(jwt).id(), imovelId);
        return ResponseEntity.noContent().build();
    }
}
