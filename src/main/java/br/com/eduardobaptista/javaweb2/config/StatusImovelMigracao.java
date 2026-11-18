package br.com.eduardobaptista.javaweb2.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class StatusImovelMigracao {

    // imoveis.status era texto livre (ex.: "Disponível"). Como não há migrations, na inicialização todo valor
    // fora do enum StatusImovel (inclusive null) vira DISPONIVEL. Idempotente: depois da 1ª vez não altera nada.
    // BINARY é obrigatório: a collation da coluna ignora caixa e acento, e sem ele 'Disponível' = 'DISPONIVEL'.
    @Bean
    public CommandLineRunner migrarStatusImovel(JdbcTemplate jdbc) {
        return args -> {
            int alterados = jdbc.update("""
                    update imoveis set status = 'DISPONIVEL'
                    where status is null or binary status not in ('DISPONIVEL', 'RESERVADO', 'ALUGADO', 'VENDIDO')
                    """);
            if (alterados > 0)
                System.out.println("Status de " + alterados + " imóvel(is) convertido(s) para DISPONIVEL.");
        };
    }
}
