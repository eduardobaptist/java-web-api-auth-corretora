package br.com.eduardobaptista.javaweb2.controller;

import br.com.eduardobaptista.javaweb2.model.FotoImovelModel;
import br.com.eduardobaptista.javaweb2.service.FotoImovelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/fotos")
public class FotoImovelController {
    @Autowired
    private FotoImovelService fotoImovelService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FotoImovelModel> salvar(@RequestParam("arquivo")MultipartFile arquivo, @RequestParam("imovelId") Integer id) {
        try {
            if (arquivo.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            FotoImovelModel fotoImovelModel = fotoImovelService.salvar(arquivo, id);
            return ResponseEntity.status(200).body(fotoImovelModel);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.internalServerError().build();
    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Integer id) {
        fotoImovelService.excluir(id);
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FotoImovelModel> editar(@RequestParam("arquivo")MultipartFile arquivo, @RequestParam("fotoId") Integer id) {
        try {
            if (arquivo.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            FotoImovelModel fotoImovelModel = fotoImovelService.editar(arquivo, id);
            return ResponseEntity.status(200).body(fotoImovelModel);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.internalServerError().build();
    }

    @PostMapping("/{id}")
    public void tornarDestaque(@PathVariable Integer id) {
        fotoImovelService.tornarDestaque(id);
    }

}
