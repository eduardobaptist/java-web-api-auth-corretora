# CLAUDE.md

API REST de uma **corretora de imóveis** (cadastro de imóveis, fotos, bairros, tipos de imóvel, usuários e
envio de e-mail de interesse). Código, nomes de campos e mensagens estão em **português**: mantenha esse padrão.

## Stack

- Java 21, Spring Boot 4.1 (Maven, use o wrapper `./mvnw`)
- Spring Web MVC, Spring Data JPA (Hibernate) + MySQL (`mysql-connector-java`)
- Spring Security + OAuth2 Resource Server (JWT HS256 assinado com segredo simétrico, via Nimbus)
- Spring Mail (SMTP do Resend) + Thymeleaf para templates de e-mail
- Lombok (`@Getter/@Setter/@Data/@Builder`); o annotation processor está configurado no `pom.xml`
- Dependência HATEOAS presente, mas ainda não usada

## Comandos

```bash
./mvnw spring-boot:run      # sobe em http://localhost:8080
./mvnw clean package        # build do jar em target/
./mvnw test                 # só existe o teste contextLoads (precisa do MySQL rodando)
```

Pré-requisito: MySQL em `localhost:3306` com o banco `restaurante` (nome herdado, não é erro) e o usuário
configurado em `application.properties`. O schema é criado/atualizado sozinho (`ddl-auto=update`); não há migrations.

Na inicialização, `config/AdminSeeder` cria o usuário admin definido em `app.admin.*` se ele ainda não existir.

## Estrutura (`src/main/java/br/com/eduardobaptista/javaweb2`)

| Pacote | Conteúdo |
|--------|----------|
| `controller` | `AuthController` (`/auth`), `ImovelController` (`/imoveis`), `FotoImovelController` (`/fotos`), `BairroController` (`/bairros`), `TipoImovelController` (`/tipos-imoveis`), `UserController` (`/user`), `EmailController` (`/email`, teste) |
| `service` | Regras de negócio; um service por entidade + `EmailService` |
| `repository` | Interfaces `JpaRepository<Model, Integer>` |
| `model` | Entidades JPA (`*Model`) e o enum `Role` (`ADMIN`, `BASIC`) |
| `dto` | `LoginRequestDTO`, `LoginResponseDTO`, `UserDTO` (sem senha), `UserRequestDTO`, `ImovelSaberMaisDTO` |
| `security` | `SecurityConfig` (regras de acesso, encoder/decoder JWT), `TokenService` (gera o token), `UserDetailsServiceImpl` |
| `config` | `AdminSeeder` |

Recursos: `src/main/resources/templates/email/template.html` (template Thymeleaf do e-mail "saber mais").

### Entidades

- `ImovelModel` (`imoveis`): dados do imóvel, `precoVenda`/`precoAluguel`/áreas em `BigDecimal`, `finalidade` e
  `status` como texto livre, `destaque` (default `false`), `@ManyToOne` LAZY para `TipoImovelModel` e `BairroModel`.
  O campo `fotos` (`List<String>`) é `@Transient` e só de leitura no JSON. `ImovelService.getAll(Pageable)`/`findComFotos()`
  o preenchem com as URLs das fotos, buscando todas numa consulta só (`findByImovelIdIn`). `find()` não preenche.
- `FotoImovelModel` (`fotos_imoveis`): `nomeArquivo`, `caminho` (path no disco), `capa`, `ordem`, `@ManyToOne` para imóvel.
- `BairroModel` (`bairros`), `TipoImovelModel` (`tipos_imoveis`), `UserModel` (`users`, e-mail único, senha BCrypt).

## Padrões do código

- Controllers retornam as entidades diretamente (exceto usuários, que usam `UserDTO` para não expor a senha).
- `POST` de criação responde `201` sem corpo, com header `Location`; `PUT` responde `204`. Exceções: `/user`
  devolve o `UserDTO` e `/fotos` devolve o `FotoImovelModel`.
- `PUT` faz `model.setId(id)` + `save`, ou seja, substitui o registro inteiro.
- **Toda listagem é paginada**: o controller recebe `@PageableDefault(size = 20, sort = "id") Pageable` e retorna
  `Page<T>` (para usuários, `page.map(UserDTO::new)`). Com `spring.data.web.pageable.serialization-mode=via-dto`, a
  resposta sai como `{ content, page: { size, number, totalElements, totalPages } }`. O `max-page-size` é 100.
  Não crie endpoints que devolvam `List` sem paginação. As rotas `/bairros/bairros-page` e
  `/tipos-imoveis/tipos-imoveis-page` são apenas aliases obsoletos da listagem.
- Services retornam `null` quando não encontram o registro; o controller decide o status.
- Injeção por `@Autowired` em campo na maioria das classes (`ImovelService` e `EmailService` usam construtor).
- Relacionamentos LAZY serializados em JSON precisam de `@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})`.
- Fotos são salvas em `${app.upload.dir}/{imovelId}/{uuid}_{nomeOriginal}` (padrão `./uploads`). O
  `config/WebConfig` serve essa pasta em `GET /uploads/**` (rota pública).

## Segurança

- Stateless, CSRF desligado, `Authorization: Bearer <jwt>`.
- Token gerado em `TokenService`: `sub` = e-mail, claims `id`, `nome`, `roles` (lista), issuer `javaweb2`,
  validade `app.jwt.expiration-minutes`. As roles do token viram `ROLE_*` em `SecurityConfig.jwtAuthenticationConverter`.
- Regras (em `SecurityConfig`, a ordem importa):
  - públicas: `POST /auth/login`, `GET /imoveis`, `GET /imoveis/{id}`, `POST /imoveis/sabermais/{id}`
    (formulário de contato), `GET /uploads/**`, `/error`
  - ADMIN: escrita em `/imoveis/**` (a regra do sabermais precisa vir antes), tudo em `/fotos/**` e `/user/**`
  - qualquer outra rota: autenticado
- O bean `bearerTokenResolver` ignora o header `Authorization` nas rotas públicas. Sem isso, um token
  expirado/inválido enviado pelo cliente gera `401` mesmo em rota `permitAll()`, porque o filtro do resource server
  valida o token antes da autorização. **Ao adicionar uma rota pública, inclua-a tanto em `authorizeHttpRequests`
  quanto nos matchers do `bearerTokenResolver`.**
- Não há CORS configurado.

## Documentação da API

O contrato completo está em [`docs/openapi.yaml`](docs/openapi.yaml) (OpenAPI 3.0.3). Ele serve para o Swagger UI
e como contexto para o frontend que consome a API. **Ao criar ou alterar um endpoint, DTO ou entidade, atualize esse
arquivo junto** (rotas, status, schemas e regras de acesso).

Há também uma coleção do Insomnia em `Insomnia_2026-10-27.yaml`.

## Pontos de atenção / limitações conhecidas

- `application.properties` contém credenciais reais (banco, chave SMTP do Resend, segredo JWT e senha do admin).
  Não copie esses valores para outros arquivos nem para respostas; prefira variáveis de ambiente.
- `POST /imoveis/sabermais/{id}` é público e dispara e-mail sem nenhum limite de taxa ou captcha (risco de spam
  e de consumo da cota do Resend).
- O destinatário dos e-mails está fixo no código (`ImovelService.enviarEmailSaberMais` e `EmailController`).
- `GET /user/` tem barra final (`@GetMapping("/")`); `GET /user` não é mapeado.
- `GET /bairros/{id}` e `GET /tipos-imoveis/{id}` respondem `200` com corpo vazio quando o id não existe, e
  o `PUT` desses recursos responde `204` mesmo sem encontrar o registro.
- As fotos só aparecem como URLs em `ImovelModel.fotos`; não existe endpoint que liste os registros de foto
  (com `id`/`capa`). O campo `ordem` da foto nunca é preenchido.
- Se o `POST /fotos` falhar ao gravar no banco, a API responde `200` com corpo vazio e o arquivo fica órfão no disco.
  O `ddl-auto=update` não altera colunas existentes. Por exemplo, `fotos_imoveis.id` sem `AUTO_INCREMENT`
  (`Field 'id' doesn't have a default value`) precisou de `ALTER TABLE` manual.
- `FotoImovelController` e `FotoImovelService` engolem exceções (`printStackTrace`) e respondem `500`.
- `ImovelSaberMaisDTO.toMap()` usa `Map.of`, então qualquer campo nulo gera `NullPointerException`.
- Sem Bean Validation, sem `@ControllerAdvice` para tratar erros globalmente e sem testes reais.
- `sort` com um campo inexistente nas listagens responde `500` (não há tratamento para `PropertyReferenceException`).
