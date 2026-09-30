# API Lens

API Lens is currently a personal concept project aiming to automate api
discovery aiming to automate finding endpoints provided by api documenentation.
Currently this build aims to check allowed endpoints, however later in the project
the aim is to efficiently retrieve all endpoints from dashboards.

> **Status: Phase 6 (API explorer frontend) complete.** Open the app,
> add an API, click into it, and use Overview → Run Discovery, then
> browse Endpoints (searchable/filterable) and Schemas. Quality scoring,
> snapshots/diffing, and breaking-change detection are still ahead. See
> "Roadmap" below and `docs/architecture.md`.
>
> **Standing caveat from Phase 5, still unresolved**: the backend's
> `swagger-parser` dependency has not been build-verified in the authoring
> sandbox (no network access there) — run `mvn -B clean verify` before
> relying on it; see `docs/adr/ADR-005-openapi-parsing-strategy.md`.

## What API Lens does (target functionality)

- Discovers OpenAPI/Swagger specifications from supported, standard
  locations for a given API base URL — not arbitrary crawling.
- Parses OpenAPI 3.x (and Swagger 2.x where practical) into a normalized
  internal contract model.
- Presents the contract as a searchable, filterable API explorer:
  endpoints, parameters, request/response bodies, schemas, auth
  requirements.
- Analyzes documentation and design quality with transparent, per-category
  scores backed by individual, click-through findings.
- Stores contract snapshots over time and diffs them semantically (not as
  raw JSON diffs).
- Classifies changes as breaking / potentially breaking / non-breaking,
  with a plain-language explanation for each.
- Generates example requests (cURL, JavaScript, Java) from the contract.
- Shows the source and confidence level behind every discovered fact.

## Tech stack

| Layer     | Technology |
|-----------|------------|
| Backend   | Java 21, Spring Boot 3, Spring Web, Spring Data JPA, Flyway |
| OpenAPI parsing | swagger-parser (io.swagger.parser.v3), behind our own ContractParser interface |
| Database  | PostgreSQL |
| Frontend  | React 18, TypeScript, Vite, React Router |
| Infra     | Docker, Docker Compose, GitHub Actions |
| Testing   | JUnit 5, Mockito, Testcontainers, Vitest |

## Getting started

### Prerequisites
- Docker and Docker Compose
- (For local, non-Docker development) Java 21 + Maven, Node 20+

### Run with Docker Compose
```bash
docker compose up --build
```
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080/api/status
- Backend health: http://localhost:8080/actuator/health

### Run locally without Docker
```bash
# Postgres (or point DATABASE_URL at your own instance)
docker run -d --name apilens-postgres -p 5432:5432 \
  -e POSTGRES_DB=apilens -e POSTGRES_USER=apilens -e POSTGRES_PASSWORD=apilens \
  postgres:16-alpine

# Backend
cd backend
mvn spring-boot:run

# Frontend (separate terminal)
cd frontend
npm install
npm run dev
```

## Environment variables

See `.env.example`. Key variables:

| Variable | Purpose |
|---|---|
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | PostgreSQL connection |
| `APILENS_HTTPS_ONLY` | Restrict discovery fetches to HTTPS (default `true`) |
| `MAX_RESPONSE_SIZE` | Max bytes accepted from a discovered spec |
| `MAX_REDIRECTS` | Max redirects followed during discovery |
| `APILENS_CONNECT_TIMEOUT_MS`, `APILENS_READ_TIMEOUT_MS` | SecureHttpClient timeouts |
| `CORS_ALLOWED_ORIGINS` | Origins allowed to call the backend API |

Secrets are never committed; only `.env.example` is checked in.

## Testing
```bash
cd backend && mvn test        # unit + Testcontainers integration tests
cd frontend && npm run test   # frontend tests
```

## Security

API Lens fetches URLs supplied by users, which is an SSRF risk. Outbound
requests are routed exclusively through `SecureHttpClient`
(`backend/src/main/java/com/apilens/infrastructure/http`), enforcing
HTTPS-only, private/link-local/metadata-IP blocking, validated redirects,
timeouts, response size limits, and a content-type allow-list. Full details,
including one explicitly documented limitation (DNS rebinding), are in
`docs/security.md` and `docs/adr/ADR-004-ssrf-protection-strategy.md`.
API Lens analyzes API contracts from documentation and specifications — it
does not brute-force endpoints, bypass authentication, or scan arbitrary
networks.

## Architecture

See `docs/architecture.md` for the layered backend design, package layout,
and planned request flows, and `docs/adr/` for individual design decisions.

## Roadmap

Built incrementally, in this order (see `docs/architecture.md` for detail):

1. **Project setup** ✅
2. **API management (CRUD)** ✅
3. **Secure HTTP client (SSRF-hardened)** ✅
4. **OpenAPI discovery** ✅
5. **OpenAPI parsing → normalized contract model** ✅
6. **API explorer (frontend)** ✅ (this revision)
7. Quality analysis
8. Contract snapshots + canonical hashing
9. Contract diff
10. Breaking change detection
11. Request generation (cURL / JS / Java)
12. Polish, docs, test coverage

Future (post-MVP): HAR-based observed-traffic discovery, scheduled drift
monitoring with notifications, a CI/CD CLI, GitHub PR integration.
