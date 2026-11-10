package br.com.eduardobaptista.javaweb2.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // rotas públicas
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/imoveis", "/imoveis/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // "saber mais" é o formulário de contato do visitante (só envia e-mail): público.
                        // Precisa vir antes da regra de ADMIN para POST /imoveis/**.
                        .requestMatchers(HttpMethod.POST, "/imoveis/sabermais/{id}").permitAll()

                        // CRUD de imóveis (e suas fotos) e de usuários: somente admin
                        .requestMatchers(HttpMethod.POST, "/imoveis", "/imoveis/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/imoveis/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/imoveis/**").hasRole("ADMIN")
                        .requestMatchers("/fotos", "/fotos/**").hasRole("ADMIN")
                        .requestMatchers("/user", "/user/**").hasRole("ADMIN")

                        // demais rotas: apenas autenticado
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));

        return http.build();
    }

    // Nas rotas públicas o header Authorization é ignorado: assim um token expirado/inválido
    // enviado pelo cliente não transforma uma rota pública em 401.
    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
        PathPatternRequestMatcher.Builder path = PathPatternRequestMatcher.withDefaults();
        RequestMatcher rotasPublicas = new OrRequestMatcher(
                path.matcher(HttpMethod.POST, "/auth/login"),
                path.matcher(HttpMethod.GET, "/imoveis"),
                path.matcher(HttpMethod.GET, "/imoveis/{id}"),
                path.matcher(HttpMethod.GET, "/uploads/**"),
                path.matcher(HttpMethod.POST, "/imoveis/sabermais/{id}"));

        return request -> rotasPublicas.matches(request) ? null : delegate.resolve(request);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return NimbusJwtEncoder.withSecretKey(secretKey()).build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey()).build();
    }

    // lê a claim "roles" do token e transforma em ROLE_ADMIN / ROLE_BASIC
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    private SecretKey secretKey() {
        return new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
