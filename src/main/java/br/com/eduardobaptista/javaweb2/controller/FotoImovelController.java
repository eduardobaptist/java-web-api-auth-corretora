package br.com.eduardobaptista.javaweb2.controller;

import br.com.eduardobaptista.javaweb2.model.FotoImovelModel;
import br.com.eduardobaptista.javaweb2.service.FotoImovelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/fotos")
public class FotoImovelController {
    private static final Set<String> TIPOS_ACEITOS = Set.of("image/jpeg", "image/png", "image/webp");

    // o arquivo é gravado como {uuid}_{nomeOriginal} e esse nome (e o caminho) vão para colunas VARCHAR(255)
    private static final int TAMANHO_MAXIMO_NOME = 150;

    @Autowired
    private FotoImovelService fotoImovelService;

    // devolve a mensagem de erro do campo "arquivo", ou null se o arquivo é válido
    private static String validarArquivo(MultipartFile arquivo) {
        if (arquivo.isEmpty())
            return "O arquivo é obrigatório e não pode estar vazio.";
        if (arquivo.getContentType() == null || !TIPOS_ACEITOS.contains(arquivo.getContentType()))
            return "Formato inválido. Envie uma imagem JPEG, PNG ou WebP.";
        String nome = arquivo.getOriginalFilename();
        if (nome == null || nome.isBlank())
            return "O arquivo precisa ter um nome.";
        if (nome.length() > TAMANHO_MAXIMO_NOME)
            return "O nome do arquivo deve ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres.";
        return null;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> salvar(@RequestParam("arquivo")MultipartFile arquivo, @RequestParam("imovelId") Integer id) {
        try {
            String erro = validarArquivo(arquivo);
            if (erro != null)
                return ResponseEntity.badRequest().body(Map.of("arquivo", erro));

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
    public ResponseEntity<?> editar(@RequestParam("arquivo")MultipartFile arquivo, @RequestParam("fotoId") Integer id) {
        try {
            String erro = validarArquivo(arquivo);
            if (erro != null)
                return ResponseEntity.badRequest().body(Map.of("arquivo", erro));

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
