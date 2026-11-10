package br.com.eduardobaptista.javaweb2.controller;

import br.com.eduardobaptista.javaweb2.model.TipoImovelModel;
import br.com.eduardobaptista.javaweb2.service.TipoImovelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/tipos-imoveis")
public class TipoImovelController {

    @Autowired
    private TipoImovelService service;

    // "/tipos-imoveis-page" é mantido como alias da listagem por compatibilidade
    @GetMapping({"", "/tipos-imoveis-page"})
    public ResponseEntity<Page<TipoImovelModel>> getAll(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<TipoImovelModel> find(@PathVariable Integer id) {
        TipoImovelModel model = service.find(id);
        return ResponseEntity.status(HttpStatus.OK).body(model);
    }

    @PostMapping
    public ResponseEntity<Void> insert(@RequestBody TipoImovelModel model) {
        model = service.insert(model);
        // return new ResponseEntity(model, HttpStatus.CREATED);
        URI uri =
                ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(model.getId()).toUri();
        return ResponseEntity.created(uri).build();
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<Void> update(@RequestBody TipoImovelModel model, @PathVariable Integer id) {
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
