package br.com.eduardobaptista.javaweb2.controller;

import java.net.URI;

import br.com.eduardobaptista.javaweb2.model.BairroModel;
import br.com.eduardobaptista.javaweb2.service.BairroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(value = "/bairros")
public class BairroController {

    @Autowired
    private BairroService service;

    // "/bairros-page" é mantido como alias da listagem por compatibilidade
    @GetMapping({"", "/bairros-page"})
    public ResponseEntity<Page<BairroModel>> getAllBairros(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<BairroModel> find(@PathVariable Integer id) {
        BairroModel model = service.find(id);
        return ResponseEntity.status(HttpStatus.OK).body(model);
    }

    @PostMapping
    public ResponseEntity<Void> insert(@RequestBody BairroModel model) {
        model = service.insert(model);
        // return new ResponseEntity(model, HttpStatus.CREATED);
        URI uri =
                ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(model.getId()).toUri();
        return ResponseEntity.created(uri).build();
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<Void> update(@RequestBody BairroModel model, @PathVariable Integer id) {
        model.setId(id);
        model = service.update(model);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }


}
