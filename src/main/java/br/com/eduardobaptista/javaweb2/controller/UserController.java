package br.com.eduardobaptista.javaweb2.controller;

import br.com.eduardobaptista.javaweb2.dto.UserDTO;
import br.com.eduardobaptista.javaweb2.dto.UserRequestDTO;
import br.com.eduardobaptista.javaweb2.model.ImovelModel;
import br.com.eduardobaptista.javaweb2.model.UserModel;
import br.com.eduardobaptista.javaweb2.service.FavoritoService;
import br.com.eduardobaptista.javaweb2.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService service;

    @Autowired
    private FavoritoService favoritoService;

    @GetMapping("/")
    public ResponseEntity<Page<UserDTO>> getAll(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(service.getAll(pageable).map(UserDTO::new));
    }

    @PostMapping()
    public ResponseEntity<?> create(@Validated({Default.class, UserRequestDTO.Criacao.class}) @RequestBody UserRequestDTO user) {
        try {
            UserModel savedUser = service.create(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(new UserDTO(savedUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getById(@PathVariable int id) {
        UserModel user = service.getById(id);
        if (user != null) {
            return ResponseEntity.ok(new UserDTO(user));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // imóveis favoritos de um usuário (admin)
    @GetMapping("/{id}/favoritos")
    public ResponseEntity<Page<ImovelModel>> favoritos(@PathVariable int id,
                                                       @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Page<ImovelModel> page = favoritoService.listar(id, pageable);
        if (page == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(page);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @Valid @RequestBody UserRequestDTO user) {
        try {
            UserModel updatedUser = service.update(id, user);

            if (updatedUser != null) {
                return ResponseEntity.ok(new UserDTO(updatedUser));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}
