# china-kekao

Web application for learning Chinese characters with FSRS-5 spaced repetition.
See [`AGENTS.md`](./AGENTS.md) for the full roadmap and conventions.

## Stack

- **Backend:** Spring Boot 3.3, Java 21, PostgreSQL 16, Flyway, Bucket4j
- **Frontend:** React 18 + Vite + TypeScript (TASK-017, see [`frontend/README.md`](./frontend/README.md))
- **Infra:** Docker Compose for local dev, no Redis (JWT stateless, in-memory rate limit)

## Repository layout

```
.
├── backend/            Spring Boot service (Maven)
│   ├── src/main/...    Production code, organized by domain package
│   ├── src/test/...    Unit + integration tests (Testcontainers PostgreSQL)
│   └── Dockerfile      Multi-stage build (Maven → JRE 21)
├── frontend/           React 18 + Vite SPA (TypeScript, Tailwind, TanStack Query)
│   ├── src/            App entrypoint, pages, components, auth store, API client
│   └── Dockerfile      dev / build / nginx-prod targets
├── docker-compose.yml  postgres + backend (frontend behind `frontend` profile)
├── .env.example        Template for environment variables
└── AGENTS.md           Task list / conventions for AI agents
```

## Local development with Docker

Prerequisites: Docker 24+ with Compose v2.

```bash
cp .env.example .env          # adjust passwords / ports as needed
docker compose up --build     # builds backend image and starts the stack
```

The backend exposes:

- `http://localhost:8080/actuator/health` — liveness/readiness probe

PostgreSQL data is persisted in the `postgres-data` named volume.

To shut everything down (and wipe the database):

```bash
docker compose down -v
```

## Local development without Docker

Prerequisites: JDK 21, Maven 3.9+, a running PostgreSQL with database `kekao`.

```bash
cd backend
mvn spring-boot:run
```

The application picks up `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` from the
environment and falls back to the defaults shown in `.env.example`.

## Tests

```bash
cd backend
mvn test
```

Integration tests use Testcontainers and require a working Docker daemon.

## Refreshing HSK / CC-CEDICT data

The bundled hanzi catalogue covers HSK 1–3 (~600 characters). Sources:

- HSK 2.0 official 2012 word lists from the
  [`glxxyz/hskhsk.com`](https://github.com/glxxyz/hskhsk.com) repo
  (single hanzi extracted from L1/L2/L3 vocabulary).
- [CC-CEDICT](https://www.mdbg.net/chinese/dictionary?page=cc-cedict)
  (CC BY-SA 4.0) — single-character subset for the listed hanzi.

To regenerate `backend/src/main/resources/imports/hsk{1,2,3}.txt` and
the bundled `cedict.sample.txt` from the upstream sources:

```bash
node scripts/import-data/fetch-hsk.mjs
```

The script is idempotent and only writes the four files above. For
production deployments you can point `KEKAO_IMPORT_CEDICT_PATH` at the
full CC-CEDICT dump instead of the bundled subset.

## Profiles

`SPRING_PROFILES_ACTIVE` selects between:

- `dev` — verbose SQL logging, full health details
- `prod` — minimal logging, forwarded headers enabled for reverse proxies

## Conventions

See [`AGENTS.md`](./AGENTS.md). Highlights:

- One TASK ↔ one PR
- Conventional Commits (`feat:`, `fix:`, `refactor:`, …)
- Never edit an applied migration — always add a new `V###__*.sql` file
