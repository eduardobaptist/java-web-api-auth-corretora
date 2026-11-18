package br.com.eduardobaptista.javaweb2.controller;

import br.com.eduardobaptista.javaweb2.dto.ContratoDTO;
import br.com.eduardobaptista.javaweb2.dto.ContratoGerarRequestDTO;
import br.com.eduardobaptista.javaweb2.model.ContratoModel;
import br.com.eduardobaptista.javaweb2.model.StatusContrato;
import br.com.eduardobaptista.javaweb2.security.UsuarioLogado;
import br.com.eduardobaptista.javaweb2.service.ContratoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/contratos")
public class ContratoController {

    @Autowired
    private ContratoService service;

    // Duas formas de criar o contrato de uma oferta ACEITA, na mesma rota, escolhidas pelo Content-Type:
    // multipart/form-data = upload de um PDF pronto; application/json = a API gera o PDF.

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> criarPorUpload(@RequestParam("arquivo") MultipartFile arquivo,
                                               @RequestParam("ofertaId") Integer ofertaId) throws IOException {
        return criado(service.criarPorUpload(ofertaId, arquivo));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> gerar(@Valid @RequestBody ContratoGerarRequestDTO dto) throws IOException {
        return criado(service.gerar(dto));
    }

    // admin confere o PDF que seria gerado, sem salvar nada
    @PostMapping(value = "/previa", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> previa(@Valid @RequestBody ContratoGerarRequestDTO dto) throws IOException {
        ContentDisposition disposition = ContentDisposition.inline()
                .filename("previa-contrato-oferta-" + dto.ofertaId() + ".pdf").build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(service.previa(dto));
    }

    private ResponseEntity<Void> criado(ContratoModel contrato) {
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(contrato.getId()).toUri();
        return ResponseEntity.created(uri).build();
    }

    // admin: todos os contratos, com filtros opcionais
    @GetMapping
    public ResponseEntity<Page<ContratoDTO>> listar(@RequestParam(required = false) StatusContrato status,
                                                    @RequestParam(required = false) Integer ofertaId,
                                                    @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(service.listar(status, ofertaId, null, pageable).map(ContratoDTO::new));
    }

    // contratos das ofertas do usuário logado
    @GetMapping("/meus")
    public ResponseEntity<Page<ContratoDTO>> meus(@RequestParam(required = false) StatusContrato status,
                                                  @AuthenticationPrincipal Jwt jwt,
                                                  @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Integer usuarioId = UsuarioLogado.de(jwt).id();
        return ResponseEntity.ok(service.listar(status, null, usuarioId, pageable).map(ContratoDTO::new));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContratoDTO> find(@PathVariable Integer id, @AuthenticationPrincipal Jwt jwt) {
        ContratoModel contrato = service.find(id, UsuarioLogado.de(jwt));
        if (contrato == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new ContratoDTO(contrato));
    }

    // download do PDF (admin ou cliente da oferta)
    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> pdf(@PathVariable Integer id, @AuthenticationPrincipal Jwt jwt) {
        ContratoModel contrato = service.find(id, UsuarioLogado.de(jwt));
        Resource arquivo = contrato == null ? null : service.arquivo(contrato);
        if (arquivo == null)
            return ResponseEntity.notFound().build();

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(contrato.getNomeArquivo(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(arquivo);
    }

    // cliente da oferta assina
    @PostMapping("/{id}/assinar")
    public ResponseEntity<ContratoDTO> assinar(@PathVariable Integer id, @AuthenticationPrincipal Jwt jwt) {
        ContratoModel contrato = service.assinar(id, UsuarioLogado.de(jwt).id());
        if (contrato == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new ContratoDTO(contrato));
    }

    // admin cancela um contrato ainda não assinado
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ContratoDTO> cancelar(@PathVariable Integer id) {
        ContratoModel contrato = service.cancelar(id);
        if (contrato == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new ContratoDTO(contrato));
    }
}
