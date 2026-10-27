package br.com.eduardobaptista.javaweb2.controller;

import java.util.List;
import java.util.Map;

import br.com.eduardobaptista.javaweb2.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping ("/email")
public class EmailController {

    @Autowired
    private EmailService service;

    @GetMapping
    public ResponseEntity<String> enviar() {
        try{
            service.enviarEmailComTemplate(
                    "aristimdudu@gmail.com",
                    "Bem-vindo!",
                    "email/template",
                    Map.of(
                            "nome", "Maria",
                            "itens", List.of("Plano Pro", "Suporte 24h"),
                            "link", "https://meusite.com"
                    ));

            return ResponseEntity.ok("Enviado");
        }
        catch(Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }


}
