# Meu Jiu Jitsu — Planejamento

Plataforma/app para praticantes de jiu-jitsu, vinculados à sua academia, acompanharem faixa, frequência, aprendizado, competições e pausas no treino.

> Projeto de estudo. O objetivo é construir uma API REST completa **sem seguir tutorial passo a passo**, consultando a documentação quando travar.

---

## 1. Stack

- Java + Spring Boot (Web, Data JPA, Validation, Security)
- PostgreSQL
- Flyway (versionamento do schema, com `spring.jpa.hibernate.ddl-auto=validate`)
- jjwt (geração e validação de JWT)
- Testcontainers (testes de integração com Postgres real)
- Docker Compose (Postgres local)

Dependência do `spring-boot-starter-security` só entra no passo de segurança (ver seção 9). Não é necessário `oauth2-resource-server` nem login com Google no MVP.

---

## 2. Roadmap

| Versão | Escopo |
| --- | --- |
| **v1 (MVP)** | Usuário, academia, treinos, pausas, promoções de faixa e estatísticas. Tudo em autoatendimento (o aluno registra os próprios dados). |
| **v2** | Perfil de professor, convites para a academia, turmas, confirmação de presença pelo professor, competições e medalhas. |
| **v3** | Cards de técnicas, módulos de aprendizado (história, teoria) e progresso do aluno. |

MVP = *Minimum Viable Product* (Produto Mínimo Viável): a menor versão que já entrega valor e pode ser usada de verdade.

---

## 3. Perfis (roles)

Professor não tem tabela própria: é um `app_user` com `role = INSTRUCTOR`.

| Perfil | Pode |
| --- | --- |
| `STUDENT` | Ver e editar apenas os próprios dados |
| `INSTRUCTOR` | Tudo do aluno, mais ver alunos da própria academia, confirmar presença e registrar graduações |
| `ADMIN` | Tudo do professor, mais gerenciar a academia, professores e convites |

No MVP todo mundo é `STUDENT`. O campo `role` já existe no banco para não precisar de migração depois.

---

## 4. Modelo de dados completo

```
academy              (id, name, city)
academy_invite       (id, academy_id, code, expires_at)            -- v2: aluno se cadastra com o código
app_user             (id, academy_id, name, email, password_hash, role)

class_schedule       (id, academy_id, instructor_id, name, weekday, start_time)      -- v2: turmas
training_session     (id, user_id, class_id NULL, trained_on, type, duration_minutes,
                      learned_notes, status [SELF_REPORTED|CONFIRMED], confirmed_by NULL)

belt_promotion       (id, user_id, belt, degree, promoted_on, promoted_by NULL, notes)
break_period         (id, user_id, start_date, end_date NULL, reason, notes)

competition          (id, user_id, name, event_date, location, category,
                      result [GOLD|SILVER|BRONZE|NONE])                              -- v2: medalhas

technique            (id, name, category, position, belt_level, description)         -- v3: cards de golpes
user_technique       (user_id, technique_id, status [LEARNING|KNOWN|FAVORITE], notes)
training_technique   (training_id, technique_id)                                     -- o que foi treinado em cada aula

lesson               (id, title, content, category, belt_level, order_index)         -- v3: módulos de história/teoria
user_lesson_progress (user_id, lesson_id, completed_at)
```

### Decisões de modelagem

- **A faixa atual não é um campo do usuário.** Ela é a última linha de `belt_promotion`. Assim o histórico fica completo e não há dois lugares para manter sincronizados.
- **Frequência e estatísticas são derivadas.** Vêm de contar `training_session`, então não têm tabela própria. Viram queries e um DTO (`StatsResponse`).
- **`end_date` nulo em `break_period` significa "ainda parado".** Evita um campo de status.
- **`status` no treino** (`SELF_REPORTED` / `CONFIRMED`): o aluno registra sozinho e, quando houver turmas (v2), o professor confirma. A v2 não exige refazer a v1.
- **Medalha não tem tabela própria:** é o `result` de uma competição.
- **`technique` e `lesson` são conteúdo do app**, igual para todos. O que é do usuário (progresso, notas) fica nas tabelas `user_*`.
- **Uma academia por usuário** no começo. Se alguém treinar em duas, troque `academy_id` por uma tabela de vínculo (`membership`).

### Entidades JPA, enums e DTOs

- Cada tabela vira uma `@Entity`.
- Valores como `role`, `type`, `reason` e `belt` são **enums Java**, guardados como coluna. Use sempre `@Enumerated(EnumType.STRING)` (o padrão `ORDINAL` grava a posição e quebra se você reordenar o enum).
- Estatísticas e "faixa atual" **não** são entidades: são resultado de query.
- **Request/response da API são DTOs, nunca entidades** (`CreateTrainingRequest`, `TrainingResponse`...). Isso evita expor `password_hash` e impede o cliente de alterar campos que não deveria. Pode usar `record` do Java.

---

## 5. Migration V1 (Flyway)

Arquivo sugerido: `src/main/resources/db/migration/V1__create_base_tables.sql`

Cobre só o escopo da v1. Mantenha `promoted_by` nulo por enquanto (é usado na v2).

```sql
CREATE TABLE academy (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    city        VARCHAR(80),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE app_user (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    academy_id     BIGINT NOT NULL REFERENCES academy (id),
    name           VARCHAR(120) NOT NULL,
    email          VARCHAR(160) NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    role           VARCHAR(20) NOT NULL DEFAULT 'STUDENT'
                   CHECK (role IN ('STUDENT', 'INSTRUCTOR', 'ADMIN')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE training_session (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    trained_on        DATE NOT NULL,
    type              VARCHAR(20) NOT NULL
                      CHECK (type IN ('GI', 'NOGI', 'OPEN_MAT')),
    duration_minutes  INTEGER NOT NULL CHECK (duration_minutes > 0),
    learned_notes     TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_training_session_user_date ON training_session (user_id, trained_on);

CREATE TABLE belt_promotion (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    belt         VARCHAR(20) NOT NULL
                 CHECK (belt IN ('WHITE', 'BLUE', 'PURPLE', 'BROWN', 'BLACK')),
    degree       SMALLINT NOT NULL DEFAULT 0 CHECK (degree BETWEEN 0 AND 4),
    promoted_on  DATE NOT NULL,
    promoted_by  BIGINT REFERENCES app_user (id),
    notes        TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_belt_promotion_user_date ON belt_promotion (user_id, promoted_on);

CREATE TABLE break_period (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    start_date  DATE NOT NULL,
    end_date    DATE,
    reason      VARCHAR(20) NOT NULL
                CHECK (reason IN ('INJURY', 'STUDY', 'PERSONAL', 'OTHER')),
    notes       TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (end_date IS NULL OR end_date >= start_date)
);
CREATE INDEX idx_break_period_user ON break_period (user_id, start_date);

-- Academia de teste para desenvolvimento (remova ou mova para uma migration de seed)
INSERT INTO academy (name, city) VALUES ('Academia Teste', 'Marília');
```

Observação: faixas infantis (cinza, amarela, laranja, verde) não estão no `CHECK`. Se for atender o público kids, inclua-as depois por uma nova migration.

---

## 6. Estrutura de pacotes (por funcionalidade)

```
com.meujiujitsu
├── auth/          JwtService, JwtFilter, SecurityConfig, CurrentUser, AuthController
├── academy/       Academy, AcademyInvite, controller, service, repository
├── user/          AppUser, Role, controller, service, repository
├── training/      TrainingSession + DTOs
├── promotion/     BeltPromotion + DTOs
├── breakperiod/   BreakPeriod + DTOs
├── competition/   (v2)
├── classes/       (v2) ClassSchedule, confirmação de presença
├── learning/      (v3) Technique, Lesson, progresso
├── stats/         consultas e DTOs de estatística
└── common/        exceções, @ControllerAdvice, utilitários
```

Dentro de cada pacote: controller, service, repository, entidade e DTOs. Agrupar por funcionalidade (e não "todos os controllers juntos") facilita achar tudo de um recurso.

---

## 7. Endpoints

### MVP (v1)

```
POST   /auth/register
POST   /auth/login
GET    /me

GET    /me/promotions            POST /me/promotions
GET    /me/trainings?from=&to=   POST /me/trainings
PUT    /me/trainings/{id}        DELETE /me/trainings/{id}
GET    /me/breaks                POST /me/breaks
PUT    /me/breaks/{id}           DELETE /me/breaks/{id}
GET    /me/stats?from=&to=
```

O prefixo `/me` evita que o usuário passe o id de outra pessoa na URL: o usuário vem sempre do token (ou, enquanto não há segurança, do `CurrentUser` temporário).

### Versões seguintes

```
Aluno (v2/v3)   /me/competitions  /me/techniques  /me/lessons/progress

Professor (v2)  GET  /academy/students                    (só da própria academia)
                GET  /academy/students/{id}/summary
                POST /academy/students/{id}/promotions
                POST /trainings/{id}/confirm
                CRUD /academy/classes

Admin (v2)      POST /academy/invites    PUT /academy    PATCH /users/{id}/role

Conteúdo (v3)   GET /techniques   GET /lessons            (leitura para todos)
```

---

## 8. Ordem de implementação do MVP

### 8.1 Qual CRUD primeiro

1. **`Academy` e `AppUser` (só o mínimo).** O usuário precisa de uma academia. A academia pode vir da migration (seed). No usuário, basta o cadastro com a senha já em hash. Edição e exclusão ficam para depois.
2. **`TrainingSession` (treinos).** O mais simples e sem regra complicada: criar, listar por período, editar, excluir. Serve para pegar o ritmo de controller, service, repository, DTO e validação, e é onde entra o primeiro teste de integração.
3. **`BreakPeriod` (pausas).** Mesma estrutura, com uma regra pequena: a data final não pode ser anterior à inicial, e `end_date` nulo significa "ainda parado".
4. **`BeltPromotion` (promoções).** Por último porque tem mais regra: a faixa atual é a última promoção, graus de 0 a 4, e a data não pode ser futura nem anterior à promoção anterior.
5. **Estatísticas.** Não é CRUD: são consultas sobre os treinos.

### 8.2 Passo a passo

- [ ] 1. Projeto base: Docker Compose com Postgres + Flyway, com a migration V1.
- [ ] 2. Entidade `AppUser` e `CurrentUser` temporário (lê um header `X-User-Id` ou um id fixo). Para testar, crie o usuário direto no banco ou por um endpoint simples de cadastro, já com hash de senha via `spring-security-crypto` (`BCryptPasswordEncoder`).
- [ ] 3. CRUD de treinos + primeiro teste de integração.
- [ ] 4. (Opcional) Segurança aqui, logo depois do CRUD de treinos: você já tem um recurso real para proteger e testar, e promoções e pausas nascem com autenticação.
- [ ] 5. Pausas.
- [ ] 6. Promoções.
- [ ] 7. Estatísticas.
- [ ] 8. Segurança (se não fez no passo 4): `spring-boot-starter-security`, JWT, filtro, `SecurityFilterChain`, `/auth/login`, e troca da implementação do `CurrentUser`.
- [ ] 9. Testes de integração completos, incluindo "um usuário não acessa os dados de outro".
- [ ] 10. Deploy e README do repositório.

**Por que CRUD antes da segurança:** ritmo (ver algo funcionando cedo), menos coisas quebrando ao mesmo tempo, e você entende melhor a segurança depois de saber o que está protegendo. Se você sentir que vai adiar a segurança para sempre, faça o contrário: começar por ela te obriga a enfrentar o assunto cedo.

### 8.3 Para a troca para a segurança ser indolor

- **Isole "quem é o usuário logado" em um único lugar** (`CurrentUser`). Nada fora dele depende do header temporário.
- **Filtre por usuário nas queries** (`findByIdAndUserId`), mesmo sem segurança ligada. Acessar o treino de outra pessoa já resulta em "não encontrado" (404) desde o primeiro dia.
- **Não adicione `spring-boot-starter-security` antes da hora:** ao incluí-lo, o Spring bloqueia todos os endpoints (401 em tudo) e gera uma senha aleatória no console.

---

## 9. Segurança (quando chegar a hora)

### 9.1 Conceitos

- **Autenticação** = provar quem você é (email e senha).
- **Autorização** = o que você pode fazer.
- **JWT** = formato de token assinado. É o "crachá", não autentica nada por si só.
- **OAuth2 / OpenID Connect** = para delegar login a terceiros (Google, Keycloak). **Não é necessário no MVP.**
- Quem autentica de fato é o seu `/auth/login`: confere email e senha (BCrypt) e, só se baterem, gera o JWT. O token apenas prova que a autenticação já aconteceu.

### 9.2 Peças a escrever

1. **`PasswordEncoder`** (bean com `BCryptPasswordEncoder`).
2. **`POST /auth/register`**: valida os dados, confere se o email já existe, salva com a senha em hash. Comece só criando o usuário; o cliente faz login em seguida.
3. **`POST /auth/login`**: confere email e senha (via `AuthenticationManager`) e devolve um JWT.
4. **Serviço de JWT**: um lugar só que gera e valida o token (id do usuário, validade). A **chave secreta vem de variável de ambiente**, nunca fixa no código.
5. **Filtro JWT** (`OncePerRequestFilter`): lê `Authorization: Bearer ...`, valida e coloca o usuário no contexto de segurança.
6. **`SecurityFilterChain`** (bean): libera `/auth/**`, exige autenticação no resto, sessão `STATELESS`, registra o filtro.
7. **`CurrentUser`**: passa a ler o usuário do contexto de segurança.

### 9.3 Fluxo de uma requisição

```
Cliente → Authorization: Bearer <token>
        ↓
Filtro JWT: valida assinatura e validade
        ↓
   válido   → coloca o usuário no contexto de segurança → controller roda
   inválido → resposta 401, controller nem é chamado
```

O Spring **filtra** as requisições antes de chegarem ao controller; não "redireciona" (isso é comportamento de app web com sessão). O servidor não guarda sessão: por isso o token vai em todas as requisições.

### 9.4 Códigos de erro

- **401**: não autenticado (sem token, token inválido ou expirado).
- **403**: autenticado, mas sem permissão para aquilo (por exemplo, aluno acessando rota de professor).

### 9.5 Fica para depois

OAuth2 / login com Google, refresh token (no MVP, um token com validade de algumas horas ou dias resolve), recuperação de senha por email, regras por perfil (`INSTRUCTOR`, `ADMIN`).

### 9.6 Cuidados

- Nunca devolva `password_hash` nas respostas (use DTO).
- Mensagem genérica no login: "email ou senha inválidos", sem dizer qual estava errado.
- Confira a versão: a configuração do Spring Security e a API da biblioteca jjwt mudam entre versões. Se o código de um tutorial não compilar, o problema costuma ser a versão.

---

## 10. Testes

Comece em doses pequenas. O `spring-boot-starter-test` já traz JUnit 5, Mockito e AssertJ.

- **Unitário só onde há regra de negócio:** cálculo das estatísticas, dedução da faixa atual, validação de datas da pausa. Classe comum, sem Spring nem banco.
- **Um teste de integração por recurso, não por método:** ex.: POST de treino, depois GET, e confere que voltou. Use Testcontainers com Postgres real: pega mapeamento JPA, migration errada e JSON incorreto.
- **O teste mais importante do projeto:** um usuário não acessa nem edita os dados de outro. Escreva quando fizer a segurança.

Evite: perseguir porcentagem de cobertura, testar getter/setter, e mockar o repositório em tudo (gera teste que passa mesmo com a query quebrada).

Encaixe: faça o CRUD de treinos e escreva o primeiro teste logo depois, no mesmo recurso. Depois, cada nova regra de negócio ganha seu teste na hora.

---

## 11. Pontos de atenção do produto completo

- **Escopo por academia (v2):** toda consulta do professor precisa filtrar por `academy_id`. Esquecer isso deixa um professor enxergar alunos de outra academia, um erro clássico de autorização. Escreva teste de integração para isso.
- **Motivo da pausa é dado sensível:** `INJURY` é informação de saúde, com proteção especial na LGPD. Considere que o professor veja só "em pausa" e apenas o próprio aluno veja o motivo.
- **Nada além da v1 é MVP.** Turmas, competições, técnicas e lições são extensões. Modelar tudo de uma vez trava o projeto antes do primeiro endpoint.
- **Kotlin:** a migração depois é possível (Java e Kotlin convivem no mesmo projeto, classe por classe), mas faça o MVP em Java para não misturar dificuldades. Evite `data class` em entidades JPA.

---

## 12. Referências

- Spring Initializr: https://start.spring.io (confira a versão do Spring Boot do projeto)
- Documentação do Spring Security (servlet, arquitetura e autenticação): https://docs.spring.io/spring-security/reference/
- Seção de testes da documentação do Spring Boot e documentação do JUnit 5, Mockito e AssertJ.
- Termos de busca úteis: `Spring Boot 3 Spring Security 6 JWT authentication OncePerRequestFilter`, `Spring Security JWT jjwt SecurityFilterChain register login`.

---

## 13. Lista rápida do que não esquecer

- [ ] `ddl-auto=validate` + Flyway (nunca deixar o Hibernate criar tabelas)
- [ ] `@Enumerated(EnumType.STRING)` em todos os enums
- [ ] DTOs na API, entidades só dentro do domínio
- [ ] `CurrentUser` como único ponto de "quem é o usuário"
- [ ] Queries sempre filtradas por usuário (`findByIdAndUserId`)
- [ ] Senha só com BCrypt; chave do JWT em variável de ambiente
- [ ] Teste de "usuário A não acessa dados do usuário B"
- [ ] README do repositório e deploy no final
