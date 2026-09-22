# Voting API

API REST para assembleias de cooperativa: cadastro de pautas, abertura de sessão de votação por tempo determinado, recebimento de um voto Sim/Não por associado e apuração do resultado. Solução do [desafio técnico](CHALLENGE.md).

## Stack

| Item | Escolha |
|---|---|
| Linguagem / build | Java 21, Maven (wrapper incluído) |
| Framework | Spring Boot 4.1 (Spring MVC, Spring Data JPA, Bean Validation, Actuator) |
| Banco | PostgreSQL 16 com migrations Flyway |
| Documentação da API | springdoc-openapi (Swagger UI) |
| Testes | JUnit 5, Mockito, MockMvc, Testcontainers |
| Qualidade | JaCoCo (cobertura), Spotless com palantir-java-format |
| Carga | k6 via Docker |

## Como executar

Pré-requisito: Docker em execução. Não é necessário Java nem Maven para a primeira opção.

**Opção 1: tudo em containers**

```bash
docker compose --profile full up --build
```

Sobe o PostgreSQL e a aplicação em http://localhost:8080. Para parar e apagar os dados: `docker compose --profile full down -v`.

**Opção 2: banco em container, aplicação local** (requer Java 21)

```bash
./mvnw spring-boot:run
```

O próprio Spring Boot sobe o serviço `db` do `docker-compose.yml` antes de iniciar. Se preferir subir o banco à parte: `docker compose up -d db`.

Depois de subir, os pontos de entrada são:

| URL | O que é |
|---|---|
| http://localhost:8080/swagger-ui.html | Swagger UI com todos os endpoints e exemplos prontos |
| http://localhost:8080/v3/api-docs | Documento OpenAPI (o `v3` é a versão da especificação OpenAPI, não da API) |
| http://localhost:8080/actuator/health | Health check |

O arquivo [requests.http](requests.http) traz uma requisição pronta para cada cenário (sucesso, validação, 404, 409, 422) e pode ser executado direto no IntelliJ ou no VS Code (extensão REST Client).

### Variáveis de ambiente

Todas opcionais; os defaults casam com o `docker-compose.yml`.

| Variável | Default | Uso |
|---|---|---|
| `DB_HOST` | `localhost` | Host do PostgreSQL |
| `DB_PORT` | `5433` | Porta do PostgreSQL. 5433 (e não 5432) para não colidir com um Postgres instalado na máquina |
| `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `voting` | Credenciais |
| `DB_POOL_SIZE` | `20` | Tamanho do pool HikariCP |
| `APP_PORT` | `8080` | Porta publicada pelo serviço `app` do compose |

Conexão direta ao banco (DBeaver, psql): host `localhost`, porta `5433`, banco/usuário/senha `voting`.

## Endpoints

Prefixo `/api/v1`. Corpo e resposta em JSON. Ids são UUID.

| Método | Rota | O que faz | Sucesso |
|---|---|---|---|
| `POST` | `/agendas` | Cadastra uma pauta | 201 + `Location` |
| `GET` | `/agendas/{id}` | Consulta uma pauta | 200 |
| `GET` | `/agendas?page=0&size=20` | Lista pautas, mais recentes primeiro (`size` máximo 100) | 200 |
| `POST` | `/agendas/{id}/session` | Abre a sessão de votação (`durationMinutes` opcional, default 1) | 201 |
| `GET` | `/agendas/{id}/session` | Consulta a sessão, com status `OPEN` ou `CLOSED` | 200 |
| `POST` | `/agendas/{id}/votes` | Registra o voto de um associado (`cpf`, `choice` = `YES`/`NO`) | 201 |
| `GET` | `/agendas/{id}/result` | Contagem de votos e resultado | 200 |

Fluxo completo com curl:

```bash
B=localhost:8080/api/v1; J='Content-Type: application/json'

ID=$(curl -s -X POST $B/agendas -H "$J" -d '{"title":"Aprovar orçamento 2026"}' | grep -oE '"id":"[^"]+"' | cut -d'"' -f4)
curl -s -X POST $B/agendas/$ID/session -H "$J" -d '{"durationMinutes":5}'
curl -s -X POST $B/agendas/$ID/votes -H "$J" -d '{"cpf":"12345678909","choice":"YES"}'
curl -s -X POST $B/agendas/$ID/votes -H "$J" -d '{"cpf":"98765432100","choice":"NO"}'
curl -s $B/agendas/$ID/result
```

Um dos votos pode responder 422 em vez de 201: o client de CPF sorteia se o associado pode votar (ver bônus 1). Basta repetir com outro CPF válido, como `11122233396` ou `52998224725`.

Resposta do resultado enquanto a sessão está aberta:

```json
{"agendaId":"...","title":"Aprovar orçamento 2026","status":"OPEN","yesVotes":1,"noVotes":1,"totalVotes":2}
```

Depois de encerrada, o campo `outcome` aparece com `APPROVED`, `REJECTED` ou `TIED`. Enquanto a pauta não tem sessão, `status` é `NOT_OPENED`.

### Erros

Toda resposta de erro segue a RFC 7807 (`application/problem+json`) e carrega `timestamp`. Erros de validação trazem também `errors[{field, message}]`.

| Status | Quando |
|---|---|
| 400 | Corpo malformado, campo inválido (título em branco, CPF fora do formato, `choice` diferente de `YES`/`NO`), `page`/`size` fora da faixa |
| 404 | Pauta ou sessão inexistente; CPF com dígito verificador inválido (exigência do desafio, ver bônus 1) |
| 409 | Pauta já tem sessão; associado já votou nesta pauta |
| 422 | Sessão ainda não aberta ou já encerrada; associado sem permissão para votar (`UNABLE_TO_VOTE`) |

```json
{"detail":"Validation failed","instance":"/api/v1/agendas","status":400,"title":"Bad Request","timestamp":"2026-09-22T00:00:00Z","errors":[{"field":"title","message":"must not be blank"}]}
```

## Decisões de projeto

Interpretações do enunciado adotadas:

- **Uma sessão por pauta.** "Abrir uma sessão em uma pauta" e "um voto por associado por pauta" ficam literais: a pauta tem no máximo uma sessão (`voting_session.agenda_id` é `UNIQUE`), e a unicidade do voto é `(agenda_id, member_cpf)`.
- **Duração em minutos**, informada em `durationMinutes` (1 a 1440); sem o campo, 1 minuto.
- **O associado é identificado pelo CPF**, que também alimenta a validação do bônus 1. O CPF não é devolvido em nenhuma resposta e aparece mascarado nos logs (`***.***.***-09`).
- **Resultado sempre consultável.** Enquanto a sessão está aberta a contagem é parcial e sem veredito; `outcome` só existe com a sessão encerrada. Empate ou zero votos resulta em `TIED`.

Escolhas técnicas e o porquê:

- **Sessão encerra sem scheduler.** O status é derivado na leitura comparando `closesAt` com o relógio. Nenhum job, nada a perder em restart, e o limite exato é testável com um `Clock` fixo injetado nos services.
- **Unicidade garantida pelo banco, não por consulta prévia.** Sessão duplicada e voto duplicado são detectados pela constraint `UNIQUE` (via `saveAndFlush` e captura de `DataIntegrityViolationException`). Duas requisições simultâneas do mesmo associado não conseguem ambas ter sucesso, e o caminho feliz do voto custa um `SELECT` e um `INSERT`.
- **Pacotes por feature** (`agenda`, `session`, `vote`, `result`), cada um com `dto/` e `exception/`; `common/` guarda o handler de erros e o client de CPF, que por natureza seria reutilizado. `result` é um pacote próprio porque agrega pauta, sessão e votos.
- **Erros centralizados** em um `@RestControllerAdvice` que estende `ResponseEntityExceptionHandler`. Cada exceção de domínio carrega seu próprio `HttpStatus`, então adicionar uma regra nova não exige mexer no handler.
- **Schema só por Flyway**, com `ddl-auto=validate`: o Hibernate confere que as entidades batem com as tabelas, mas nunca as altera.
- **`open-in-view=false`** e transações explícitas nos services, para nenhuma query vazar para a camada web.
- **Paginação com `page` e `size` validados no controller** (400 fora da faixa, `size` máximo 100) e ordenação fixa no backend, evitando que um `size` gigante derrube o banco.
- **Ids UUID** gerados pela aplicação.

Fora do escopo, por decisão consciente: segurança (o enunciado a abstrai) e o Anexo 1 (telas `FORMULARIO`/`SELECAO`), já que o foco foi a API de domínio.

## Bônus 1: validação de CPF

`CpfValidationClient` é a facade do serviço externo. A única implementação, `FakeCpfValidationClient`, valida os dígitos verificadores com a constraint `@CPF` do Hibernate Validator e sorteia `ABLE_TO_VOTE` ou `UNABLE_TO_VOTE`, como o enunciado pede. O `VoteService` só consulta o client depois de confirmar que a sessão está aberta, para não gastar chamada externa com um voto que seria recusado de qualquer forma.

CPF inválido responde **404**, por exigência explícita do desafio. Semanticamente um 400 ou 422 seria mais natural, e a troca é uma linha em `InvalidCpfException`. `UNABLE_TO_VOTE` responde 422.

Como o sorteio é aleatório, o mesmo CPF pode votar em uma chamada e ser recusado na seguinte. Nos testes de integração o client é substituído por uma versão determinística (`AlwaysAbleCpfClientConfiguration`) que mantém a validação dos dígitos.

## Bônus 2: performance

O que foi feito para centenas de milhares de votos:

- Registrar um voto custa um `SELECT` (sessão pela pauta, coluna única indexada) e um `INSERT`. Não há consulta de "já votou?": a constraint resolve.
- O resultado é uma única query agregada, `SELECT choice, count(*) ... GROUP BY choice`, e nunca carrega votos em memória. O índice `(agenda_id, choice)` permite responder só pelo índice (*index-only scan*). A JPQL usa `count(*)` de propósito: `count(v)` viraria `count(v.id)` e obrigaria a ler a tabela.
- Pool de conexões dimensionável por `DB_POOL_SIZE`.

Teste de carga com k6 ([perf/vote-load.js](perf/vote-load.js)), sem instalar nada:

```bash
docker run --rm -i -e BASE_URL=http://host.docker.internal:8080 grafana/k6 run - < perf/vote-load.js
# opcionais: -e RATE=500 -e DURATION=120s
```

O script cria uma pauta e uma sessão, dispara votos com CPFs válidos e únicos a uma taxa constante, aceita 201 e 422 como respostas esperadas (o client fake sorteia) e imprime o resultado ao final. Limiares: p95 abaixo de 200 ms e menos de 1 % de falhas.

Medições de referência em um notebook, com Postgres em Docker:

| Cenário | Resultado |
|---|---|
| 300 req/s por 60 s (18 mil votos) | p95 3,55 ms, 0 % de falhas |
| Mesma carga com a tabela já em 509 mil votos | p95 3,94 ms: o insert não degrada |
| `GET /result` de uma pauta com 509 mil votos | ~24 ms |
| `GET /result` de uma pauta pequena em uma tabela de 509 mil linhas | 0,05 ms, *index-only scan* |

## Bônus 3: versionamento da API

A versão vai na URI: `/api/v1/...`. É a forma mais explícita para um cliente mobile e para tudo que fica no caminho (proxies, caches, logs, Swagger), e não custa nada de roteamento.

- Mudanças aditivas e compatíveis (campo opcional novo, endpoint novo) ficam na versão atual.
- Só mudanças que quebram contrato (campo removido ou renomeado, semântica ou status alterados) criam `/api/v2`: novos controllers sob o prefixo `v2` convivem com os de `v1` enquanto os clientes migram; services e entidades são compartilhados.
- Uma versão a ser desligada avisa com os headers `Deprecation` e `Sunset` antes de sair do ar.

Versionamento por header ou por media type não foi escolhido: fica invisível na URL, é mais difícil de testar no navegador ou no curl, e é fácil de errar na camada HTTP de um app mobile. O Spring Framework 7 traz versionamento nativo (`@RequestMapping(version = "1")`), evolução natural caso várias versões precisem coexistir.

## Testes e qualidade

```bash
./mvnw test      # unitários (Mockito, MockMvc), sem Docker
./mvnw verify    # + integração em PostgreSQL real via Testcontainers, cobertura JaCoCo e checagem de formatação
```

- 81 testes unitários e 18 de integração. Os de integração (`*IT`) sobem o contexto completo contra um PostgreSQL descartável.
- Relatório de cobertura em `target/site/jacoco/index.html` após o `verify`.
- Formatação com Spotless (palantir-java-format): `./mvnw spotless:apply` formata, e o `verify` falha se algo estiver fora do padrão.

## Logs

Eventos de negócio em `INFO` (pauta criada, sessão aberta, voto registrado), regras violadas em `WARN` com status e caminho, erros inesperados em `ERROR` com stack trace. O CPF nunca aparece completo. A violação de constraint por voto ou sessão duplicados é um resultado esperado e não gera log do Hibernate.

## Estrutura

```
src/main/java/com/gabrieldeborba/voting/
  agenda/      pauta: entidade, repositório, service, controller, dto/, exception/
  session/     sessão de votação
  vote/        voto
  result/      apuração
  common/      cpf/ (client de CPF), exception/ (RFC 7807)
  config/      Clock, OpenAPI, prefixo de versão
src/main/resources/db/migration/   V1..V4 (Flyway)
src/test/java/...                  espelha o main; *Test unitários, *IT integração
perf/vote-load.js                  teste de carga k6
```
