package br.com.eduardobaptista.javaweb2.controller;

import br.com.eduardobaptista.javaweb2.dto.UserDTO;
import br.com.eduardobaptista.javaweb2.dto.UserRequestDTO;
import br.com.eduardobaptista.javaweb2.model.UserModel;
import br.com.eduardobaptista.javaweb2.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService service;

    @GetMapping("/")
    public ResponseEntity<List<UserDTO>> getAll() {
        List<UserModel> users = service.getAll();
        List<UserDTO> usersDto = users.stream().map(UserDTO::new).toList();

        return ResponseEntity.status(HttpStatus.OK).body(usersDto);
    }

    @PostMapping()
    public ResponseEntity<?> create(@RequestBody UserRequestDTO user) {
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

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody UserRequestDTO user) {
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
