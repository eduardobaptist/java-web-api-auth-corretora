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
A URL do banco tem `?nullDatabaseMeansCurrent=true`. Sem isso, o Hibernate enxerga tabelas de **outros bancos** do
mesmo MySQL e deixa de criar as daqui. Por exemplo, `gate2way.ofertas` impedia a criação de `restaurante.ofertas`.
Não remova esse parâmetro. O servidor local é **MariaDB**, embora o dialect configurado seja `MySQLDialect`:
evite `@Lock` do JPA, porque ele gera `FOR UPDATE OF <alias>` e o MariaDB recusa essa sintaxe. Use query nativa
`... FOR UPDATE`, como em `ImovelRepository.findByIdParaAtualizar`. A collation das colunas também ignora caixa e
acento, então `'Disponível' = 'DISPONIVEL'` em comparações SQL. Use `BINARY` quando a diferença importar.

Na inicialização, `config/AdminSeeder` cria o usuário admin definido em `app.admin.*` se ele ainda não existir, e
`config/StatusImovelMigracao` converte para `DISPONIVEL` qualquer `imoveis.status` fora do enum (era texto livre).

## Estrutura (`src/main/java/br/com/eduardobaptista/javaweb2`)

| Pacote | Conteúdo |
|--------|----------|
| `controller` | `AuthController` (`/auth`), `ImovelController` (`/imoveis`), `FotoImovelController` (`/fotos`), `BairroController` (`/bairros`), `TipoImovelController` (`/tipos-imoveis`), `UserController` (`/user`), `OfertaController` (`/ofertas`), `ContratoController` (`/contratos`), `FavoritoController` (`/favoritos`), `EmailController` (`/email`, teste) |
| `service` | Regras de negócio; um service por entidade + `EmailService` e `ContratoPdfService` (gera o PDF do contrato) |
| `repository` | Interfaces `JpaRepository<Model, Integer>` |
| `model` | Entidades JPA (`*Model`) e os enums `Role` (`ADMIN`, `BASIC`), `StatusImovel`, `TipoOferta`, `StatusOferta`, `StatusContrato`, `OrigemContrato` |
| `dto` | `LoginRequestDTO`, `LoginResponseDTO`, `CadastroRequestDTO` (autocadastro, sem `role`), `UserDTO` (sem senha), `UserRequestDTO`, `ImovelSaberMaisDTO`, `OfertaRequestDTO`, `OfertaStatusRequestDTO`, `ContratoGerarRequestDTO`, `OfertaDTO`, `ContratoDTO`, `ImovelResumoDTO` |
| `security` | `SecurityConfig` (regras de acesso, encoder/decoder JWT), `TokenService` (gera o token), `UserDetailsServiceImpl`, `UsuarioLogado` (id + admin lidos do `Jwt`) |
| `config` | `AdminSeeder`, `StatusImovelMigracao`, `WebConfig` |
| `exception` | `ApiExceptionHandler` (`@RestControllerAdvice` que padroniza os `400` e `409`), `ValidacaoException`, `ConflitoException` |

Recursos: `src/main/resources/templates/email/template.html` (template Thymeleaf do e-mail "saber mais") e
`templates/contrato/contrato.html` (modelo do contrato gerado em PDF).

### Entidades

- `ImovelModel` (`imoveis`): dados do imóvel, `precoVenda`/`precoAluguel`/áreas em `BigDecimal`,
  `finalidade` como texto livre, `status` (`StatusImovel`: `DISPONIVEL`, `RESERVADO`, `ALUGADO`, `VENDIDO`; gravado como
  VARCHAR com `@JdbcTypeCode`, igual à coluna antiga), `destaque` (default `false`), `@ManyToOne` LAZY para `TipoImovelModel` e `BairroModel`.
  O campo `fotos` (`List<String>`) é `@Transient` e só de leitura no JSON. `ImovelService.getAll(Pageable)`/`findComFotos()`
  o preenchem com as URLs das fotos, buscando todas numa consulta só (`findByImovelIdIn`). `find()` não preenche.
- `FotoImovelModel` (`fotos_imoveis`): `nomeArquivo`, `caminho` (path no disco), `capa`, `ordem`, `@ManyToOne` para imóvel.
- `BairroModel` (`bairros`), `TipoImovelModel` (`tipos_imoveis`), `UserModel` (`users`, e-mail único, senha BCrypt).
- `OfertaModel` (`ofertas`): `@ManyToOne` LAZY para imóvel e usuário, `tipo` (`ALUGUEL`/`VENDA`), `valorProposto`
  DECIMAL(15,2), `mensagem` TEXT, `status` (`PENDENTE`, `ACEITA`, `EM_ESPERA`, `RECUSADA`, `CANCELADA`), `createdAt`.
- `ContratoModel` (`contratos`): `@ManyToOne` para oferta, `origem` (`GERADO`/`UPLOAD`), `nomeArquivo` (nome do
  download), `caminho` (path no disco), `status` (`AGUARDANDO_ASSINATURA`, `ASSINADO`, `CANCELADO`), `dataAprovacao` (quando o admin gerou o
  contrato) e `dataAssinatura`. Uma oferta pode ter vários contratos, mas só um ativo (aguardando ou assinado).
- `FavoritoModel` (`favoritos`): usuário + imóvel, unique (`usuario_id`, `imovel_id`). As FKs têm `ON DELETE CASCADE`
  (`@OnDelete`), então excluir o imóvel ou o usuário apaga os favoritos.

### Fluxo de oferta → contrato → assinatura

O fluxo mexe no `status` do imóvel e nas outras ofertas dele. `PENDENTE` e `EM_ESPERA` contam igualmente como "em aberto".

1. Cliente: `POST /ofertas` cria uma oferta `PENDENTE`. Só vale para imóvel `DISPONIVEL` (senão `409`) e só pode
   haver uma oferta em aberto por usuário e imóvel. Vários clientes podem ofertar no mesmo imóvel.
2. Admin: `PATCH /ofertas/{id}/status` com `ACEITA`, `EM_ESPERA` ou `RECUSADA`, só a partir de uma oferta em aberto.
   **Aceitar** exige o imóvel `DISPONIVEL`, passa o imóvel a `RESERVADO` e as outras ofertas `PENDENTE` dele a `EM_ESPERA`.
   O cliente pode `POST /ofertas/{id}/cancelar` enquanto a oferta estiver em aberto. `ACEITA`, `RECUSADA` e
   `CANCELADA` são finais.
3. Admin: `POST /contratos`, só para oferta `ACEITA` e sem contrato ativo, de duas formas na mesma rota
   (o `Content-Type` decide qual método do controller atende):
   - `application/json` (`ContratoGerarRequestDTO`): a API **gera** o PDF (`origem = GERADO`);
   - `multipart/form-data` (`arquivo` + `ofertaId`): **upload** de um PDF pronto (`origem = UPLOAD`).
   `POST /contratos/previa` (mesmo JSON) devolve o PDF gerado sem salvar e aceita oferta em qualquer status.
4. Cliente: baixa `GET /contratos/{id}/pdf` e assina com `POST /contratos/{id}/assinar` (preenche `dataAssinatura`).
   O imóvel vira `ALUGADO` ou `VENDIDO`, conforme o `tipo` da oferta, e as outras ofertas em aberto dele viram `RECUSADA`.
5. Admin: `POST /contratos/{id}/cancelar` (desistência), só enquanto o contrato não estiver assinado. A oferta vira
   `CANCELADA`, então não recebe outro contrato. O imóvel volta a `DISPONIVEL` (só se ainda estiver `RESERVADO`) e as
   ofertas `EM_ESPERA` dele voltam a `PENDENTE`.

Os efeitos em lote usam `OfertaRepository.alterarStatusDasOutras` (update JPQL, sem `clearAutomatically`, porque limpar
a sessão quebra o DTO, que carrega o usuário em modo lazy). Concorrência: aceitar, assinar e cancelar o contrato rodam
com `@Transactional(isolation = READ_COMMITTED)` e chamam `OfertaService.travarImovel`. Esse método faz
`SELECT ... FOR UPDATE` no imóvel e relê imóvel, oferta e contrato. Com isso, duas operações no mesmo imóvel rodam uma
depois da outra e a segunda responde `409`. No REPEATABLE READ, o MariaDB responderia
`Record has changed since last read` (500). O admin pode trocar o `status` do imóvel à vontade pelo `PUT /imoveis/{id}`,
sem efeito nas ofertas.

Os PDFs ficam em `${app.contratos.dir}/{ofertaId}/{uuid}.pdf` (padrão `./contratos`, no `.gitignore`). Essa pasta
**não pode** ficar dentro de `app.upload.dir`, que é pública: o PDF só sai pelo `GET /contratos/{id}/pdf`, que confere
o dono. No upload, o `ContratoService` valida o `Content-Type` e também os bytes iniciais (`%PDF-`).

Geração do PDF (`ContratoPdfService`): preenche `templates/contrato/contrato.html` com os dados da oferta (cliente,
imóvel com endereço/bairro/tipo/áreas, valor) e converte o HTML com o **OpenHTMLtoPDF**
(`io.github.openhtmltopdf:openhtmltopdf-pdfbox`). O texto muda com o `tipo`: locação (prazo, dia de vencimento) ou
compra e venda (forma de pagamento, escritura). `prazoMeses`/`diaVencimento` numa oferta de venda dão `400`. A parte
CONTRATADA vem de `app.contratos.empresa` (padrão `Corretora de Imóveis`). Cuidados ao editar o template:
- precisa ser **XHTML bem formado**: feche as tags (`<br/>`) e use `&#160;` em vez de `&nbsp;`;
- o CSS é o de impressão do OpenHTMLtoPDF (CSS 2.1 + `@page`), sem flexbox nem grid;
- usa as fontes padrão do PDF (Helvetica), que cobrem os acentos do português, mas não símbolos fora do
  Latin-1 (ex.: `→`). Para esses símbolos, seria preciso embutir uma fonte TTF;
- o `<style>` tem `th:inline="css"` para o rodapé do `@page` mostrar o número da oferta.

Favoritos: `GET /favoritos` (página de `ImovelModel` com `fotos`), `GET|PUT|DELETE /favoritos/{imovelId}` (o `PUT`
e o `DELETE` são idempotentes) e, para o admin, `GET /user/{id}/favoritos`.

## Padrões do código

- Controllers retornam as entidades diretamente. As exceções são usuários (`UserDTO`, para não expor a senha) e
  ofertas/contratos (`OfertaDTO`/`ContratoDTO`, que embutem o `UserDTO` e não expõem o `caminho` do PDF).
- Ações de mudança de status (`/ofertas/{id}/status`, `/ofertas/{id}/cancelar`, `/contratos/{id}/assinar`,
  `/contratos/{id}/cancelar`) respondem `200` com o DTO atualizado.
- Recursos com dono (ofertas, contratos, favoritos): o usuário vem de `UsuarioLogado.de(jwt)` (`@AuthenticationPrincipal Jwt`).
  O service filtra por dono e devolve `null` quando o registro é de outro usuário, então a API responde `404`
  (o admin vê tudo). Na `SecurityConfig` só as rotas exclusivas de admin são declaradas; as rotas do cliente
  caem em `authenticated()`.
- `POST` de criação responde `201` sem corpo, com header `Location`; `PUT` responde `204`. Exceções: `/user`
  devolve o `UserDTO` e `/fotos` devolve o `FotoImovelModel`.
- `PUT` faz `model.setId(id)` + `save`, ou seja, substitui o registro inteiro.
- **Toda listagem é paginada**: o controller recebe `@PageableDefault(size = 20, sort = "id") Pageable` e retorna
  `Page<T>` (para usuários, `page.map(UserDTO::new)`). Com `spring.data.web.pageable.serialization-mode=via-dto`, a
  resposta sai como `{ content, page: { size, number, totalElements, totalPages } }`. O `max-page-size` é 100.
  Não crie endpoints que devolvam `List` sem paginação. As rotas `/bairros/bairros-page` e
  `/tipos-imoveis/tipos-imoveis-page` são apenas aliases obsoletos da listagem.
- Services retornam `null` quando não encontram o registro; o controller decide o status.
- **Validação**: Bean Validation nas entidades (`ImovelModel`, `BairroModel`, `TipoImovelModel`) e nos DTOs de entrada,
  com `@Valid` no `@RequestBody`. Os limites seguem as colunas reais do banco (VARCHAR(255), DECIMAL(15,2)/(10,2),
  `cep` VARCHAR(9), TEXT ≈ 16000 caracteres) e as mensagens ficam em português no atributo `message`. Todo erro de
  validação responde `400 { campo: mensagem }` pelo `exception/ApiExceptionHandler`. Isso vale também para JSON
  malformado, tipo errado, path variable inválida e parâmetro multipart ausente. Uma regra que depende do banco
  (ex.: o id de `tipoImovel`/`bairro` precisa existir) lança `ValidacaoException` no service. Uma transição de status
  inválida (ex.: aceitar uma oferta cancelada) lança `ConflitoException`, que responde `409 { campo: mensagem }`. Ao criar um campo ou
  endpoint, anote as regras e documente-as (com as mensagens) no schema `*Input` do `docs/openapi.yaml`.
- CEP sempre no formato `00000-000`; UF como sigla maiúscula; `destaque` omitido/`null` vira `false` e `status`
  omitido/`null` vira `DISPONIVEL` (`ImovelService.normalizar`).
- Injeção por `@Autowired` em campo na maioria das classes (`ImovelService` e `EmailService` usam construtor).
- Relacionamentos LAZY serializados em JSON precisam de `@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})`.
- Fotos são salvas em `${app.upload.dir}/{imovelId}/{uuid}_{nomeOriginal}` (padrão `./uploads`). O
  `config/WebConfig` serve essa pasta em `GET /uploads/**` (rota pública).

## Segurança

- Stateless, CSRF desligado, `Authorization: Bearer <jwt>`.
- Token gerado em `TokenService`: `sub` = e-mail, claims `id`, `nome`, `roles` (lista), issuer `javaweb2`,
  validade `app.jwt.expiration-minutes`. As roles do token viram `ROLE_*` em `SecurityConfig.jwtAuthenticationConverter`.
- Regras (em `SecurityConfig`, a ordem importa):
  - públicas: `POST /auth/login`, `POST /auth/cadastro` (autocadastro de cliente), `GET /imoveis`, `GET /imoveis/{id}`, `POST /imoveis/sabermais/{id}`
    (formulário de contato), `GET /uploads/**`, `/error`
  - ADMIN: escrita em `/imoveis/**` (a regra do sabermais precisa vir antes), tudo em `/fotos/**` e `/user/**`,
    `GET /ofertas`, `PATCH /ofertas/{id}/status`, `GET /contratos`, `POST /contratos`, `POST /contratos/previa`,
    `POST /contratos/{id}/cancelar`
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
- `POST /auth/cadastro` cria sempre um usuário `BASIC` (o DTO não tem `role` e o `UserService.cadastrar` força a role)
  e devolve o token. Como o `sabermais`, não tem limite de taxa nem captcha.
- Os 6 bairros cadastrados hoje têm `cep_final = '9828000'` (7 dígitos) e `cep_inicial` nulo. Um `PUT` neles que
  reenvie esse valor recebe `400` até o CEP ser corrigido.
- `/fotos` só valida o `Content-Type` declarado pelo cliente, não o conteúdo do arquivo. Um `imovelId`/`fotoId`
  inexistente ainda responde `500`.
- O `ApiExceptionHandler` só trata erros de validação; os demais erros seguem o formato padrão do Spring. Não há testes reais.
- `sort` com um campo inexistente nas listagens responde `500` (não há tratamento para `PropertyReferenceException`).
- Excluir um imóvel ou usuário que tem ofertas responde `500` (a FK de `ofertas` não tem cascade, de propósito,
  para preservar o histórico). Favoritos são apagados em cascata.
- O `tipo` da oferta não é conferido contra a `finalidade`/preços do imóvel. Como o admin pode mudar o `status` do
  imóvel manualmente, nada impede, por exemplo, marcar como `DISPONIVEL` um imóvel com oferta aceita e aceitar outra.
- A "assinatura" é só o aceite do cliente logado (status + data). Não há assinatura digital nem upload do PDF assinado.
- O limite de upload é de 10MB por arquivo e 11MB por requisição (`spring.servlet.multipart.*` no
  `application.properties`) e vale para fotos e contratos. Acima disso, a API responde
  `400 { arquivo: "O arquivo excede o tamanho máximo de 10MB." }` (a mensagem lê o valor configurado).
- O modelo de contrato gerado é genérico (não foi revisado juridicamente) e não traz os dados do proprietário,
  que não existem no modelo de dados. O PDF gerado é gravado no momento da criação: editar o imóvel ou o template
  depois não altera contratos já criados.
