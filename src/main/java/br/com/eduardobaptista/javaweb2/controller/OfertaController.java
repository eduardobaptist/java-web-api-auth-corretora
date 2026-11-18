package br.com.eduardobaptista.javaweb2.controller;

import br.com.eduardobaptista.javaweb2.dto.OfertaDTO;
import br.com.eduardobaptista.javaweb2.dto.OfertaRequestDTO;
import br.com.eduardobaptista.javaweb2.dto.OfertaStatusRequestDTO;
import br.com.eduardobaptista.javaweb2.model.OfertaModel;
import br.com.eduardobaptista.javaweb2.model.StatusOferta;
import br.com.eduardobaptista.javaweb2.security.UsuarioLogado;
import br.com.eduardobaptista.javaweb2.service.OfertaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/ofertas")
public class OfertaController {

    @Autowired
    private OfertaService service;

    // cliente faz uma oferta (status inicial PENDENTE)
    @PostMapping
    public ResponseEntity<Void> criar(@Valid @RequestBody OfertaRequestDTO dto, @AuthenticationPrincipal Jwt jwt) {
        OfertaModel oferta = service.criar(dto, UsuarioLogado.de(jwt).id());
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(oferta.getId()).toUri();
        return ResponseEntity.created(uri).build();
    }

    // admin: todas as ofertas, com filtros opcionais
    @GetMapping
    public ResponseEntity<Page<OfertaDTO>> listar(@RequestParam(required = false) StatusOferta status,
                                                  @RequestParam(required = false) Integer imovelId,
                                                  @RequestParam(required = false) Integer usuarioId,
                                                  @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(service.listar(status, imovelId, usuarioId, pageable).map(OfertaDTO::new));
    }

    // ofertas do usuário logado
    @GetMapping("/minhas")
    public ResponseEntity<Page<OfertaDTO>> minhas(@RequestParam(required = false) StatusOferta status,
                                                  @AuthenticationPrincipal Jwt jwt,
                                                  @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Integer usuarioId = UsuarioLogado.de(jwt).id();
        return ResponseEntity.ok(service.listar(status, null, usuarioId, pageable).map(OfertaDTO::new));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfertaDTO> find(@PathVariable Integer id, @AuthenticationPrincipal Jwt jwt) {
        OfertaModel oferta = service.find(id, UsuarioLogado.de(jwt));
        if (oferta == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new OfertaDTO(oferta));
    }

    // cliente cancela a própria oferta (PENDENTE ou EM_ESPERA)
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<OfertaDTO> cancelar(@PathVariable Integer id, @AuthenticationPrincipal Jwt jwt) {
        OfertaModel oferta = service.cancelar(id, UsuarioLogado.de(jwt).id());
        if (oferta == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new OfertaDTO(oferta));
    }

    // admin aceita, recusa ou coloca em espera
    @PatchMapping("/{id}/status")
    public ResponseEntity<OfertaDTO> alterarStatus(@PathVariable Integer id, @Valid @RequestBody OfertaStatusRequestDTO dto) {
        OfertaModel oferta = service.alterarStatus(id, dto.status());
        if (oferta == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new OfertaDTO(oferta));
    }
}
