# API Lens — Architecture

Status: living document, updated each phase. This revision covers through
**Phase 6 (API explorer frontend)**.

## 1. System overview

```
Frontend (React/TS)
        |
        v
   REST API (Spring Boot controllers)
        |
        v
Application Services (use cases)
        |
        v
   Domain (model, domain services)
        |
        v
Infrastructure (persistence, HTTP, security, OpenAPI parsing)
        |
        v
    PostgreSQL
```

Once discovery is implemented (Phase 3–4):

```
Discovery Engine
        |
        v
Secure HTTP Client  --->  External API (untrusted, over HTTPS)
```

The Secure HTTP Client is the *only* component permitted to make outbound
requests to user-supplied URLs. No controller or service is allowed to hold
a `WebClient`/`HttpClient` pointed at an arbitrary external host directly —
this is an architectural rule, not just a convention, because it's the
single chokepoint where SSRF protections are enforced (see
`docs/security.md`, added in Phase 3).

## 2. Backend package layout

```
com.apilens
    api            REST controllers + DTOs (thin; no business logic)
    application    Use cases / orchestration services
    domain         Core model + domain services + repository interfaces
    infrastructure http, openapi parsing, persistence, security, config
    analysis       quality rules, breaking-change rules, diff engine
    discovery      discovery strategies + discovery-specific models
```

Dependency direction is inward: `api` depends on `application`, which
depends on `domain`; `infrastructure` implements interfaces defined in
`domain`/`application` rather than the reverse. This keeps the domain model
(`ApiContract`, `ApiEndpoint`, ...) free of persistence or HTTP concerns.

## 3. Phase 6 scope (this revision)

Adds the frontend API explorer on top of Phase 5's read API — no backend
changes in this phase:

- `react-router-dom` routing: `/` (Dashboard) and `/apis/:apiId` (`ApiLayout`
  — header + Overview/Endpoints/Schemas tabs), with `endpoints` and
  `endpoints/:endpointId` as further nested routes.
- `ApiLayout` fetches the API and its most recent contract **once** and
  shares it with child routes via react-router's outlet context, so
  Overview/Endpoints/Schemas/EndpointDetail don't each re-fetch.
- `EndpointsPage` — client-side search (path/operationId/summary/
  description/tags) plus method/tag/deprecated filters (product spec
  sections 27 & 29), filtered against the already-loaded contract rather
  than a server-side query.
- `EndpointDetailPage` — parameters, authentication, request body,
  responses (section 10). Request/response schemas and reusable
  component schemas render via a shared `JsonViewer` (formatted JSON,
  read-only) rather than a custom per-field renderer, matching Phase 5's
  own decision to keep that data as JSON rather than fully decomposed.
- `SchemasPage` — expandable list of reusable component schemas.
- `ApiOverviewPage` — contract summary, a manual "Run Discovery" action
  (`POST .../discover`), and an expandable discovery-attempt history
  table (section 14 provenance).

**Deliberately not added this phase**: a backend `GET /api/endpoints/{id}`
(or per-schema) endpoint. `ContractDetail` already returns every endpoint
fully nested, so the frontend finds one by id client-side; adding a
narrower backend endpoint before anything needs the smaller payload would
be speculative. Also not added: quality scores, change history, and
diff views — those tabs return once Phases 7-10 exist to populate them.

## 4. Phase 5 scope

Adds real parsing on top of Phase 4's discovery, chained into the same
`POST /api/apis/{id}/discover` request:

- `infrastructure.openapi.ContractParser` (interface) / `OpenApi3Parser`
  (implementation, backed by `swagger-parser`) — turns raw spec bytes into
  the normalized domain model. See
  `docs/adr/ADR-005-openapi-parsing-strategy.md` for the library choice,
  the JSON-as-TEXT schema storage decision, the Swagger 2.0 limitation,
  and an **explicit, important caveat**: this dependency could not be
  build-verified in the authoring sandbox (no network access) — run
  `mvn -B clean verify` before relying on it.
- Normalized entities (`ApiContract` as aggregate root, cascading to
  `ApiEndpoint` → `ApiParameter`/`ApiRequestBody`/`ApiResponse`, plus
  `ApiSchema`, `ApiSecurityScheme`, `ApiServer`), persisted via Flyway `V4`.
- `ContractIngestionService` — bridges "found a spec" (Phase 4) and
  "understood it" (this phase); a spec can be found but fail to parse,
  and callers can tell the two apart (`contractParsed` in the discover
  response, alongside `specificationFound`).
- Read API: `GET /api/apis/{id}/contracts` (summaries), `GET
  /api/contracts/{id}` (full nested detail — servers, security schemes,
  schemas, every endpoint with parameters/request body/responses inlined).
  Per-endpoint and per-schema fetch-by-id (`GET /api/endpoints/{id}`, per
  section 20) is deferred to Phase 6 alongside the API explorer UI that
  will actually need it, rather than built speculatively now.
- Tests: `OpenApi3ParserTest` against a literal OpenAPI 3.0.3 fixture
  (endpoints, parameters, request bodies, responses, operation- vs
  global-level security resolution, schema extraction), plus two new
  discovery-integration scenarios: a full success path (OpenAPI 3.x found,
  parsed, and fetchable via the read API) and the realistic partial case
  (a Swagger 2.0 doc is *found* but correctly reported as not yet parsed).

**Also deliberately not done yet**: every successful parse creates a
*new* `ApiContract` row — there's no dedup, hashing, or "did anything
actually change" logic. That's Phase 8 ("Contract Snapshots"), which will
sit on top of this table rather than replace it.

## 5. Phase 4 scope

Adds OpenAPI discovery on top of the Phase 3 secure HTTP layer:

- `discovery.strategy.DiscoveryStrategy` — interface for one way of trying
  to locate a spec, run in `@Order`: `UserProvidedSpecStrategy` (tries the
  user-supplied OpenAPI URL, if any) then `OpenApiDiscoveryStrategy`
  (tries a configurable list of standard paths — `apilens.discovery.
  standard-openapi-paths`). Both stop at their own first success.
- `discovery.DiscoveryEngine` — runs the strategies in order, stops at the
  first strategy that finds a spec (product spec section 38); does not
  itself know about HTTP or persistence.
- `infrastructure.openapi.SpecificationSniffer` — a deliberately shallow
  textual check ("does this look like an OpenAPI/Swagger document, and
  what version") used to decide whether a 200 response actually counts as
  "found". This is not the parser — `ContractParser` (Phase 5) does real
  structural parsing into the normalized domain model.
- `discovery_attempts` table (Flyway `V3`) + `DiscoveryApplicationService`
  — every URL tried, by every strategy, is persisted regardless of
  outcome, matching the provenance requirement in product spec section 5.
- `POST /api/apis/{id}/discover` and `GET /api/apis/{id}/discovery-attempts`.

**Deliberately deferred**: `POST .../discover` runs synchronously and
returns the result directly, rather than the 202-Accepted/job-polling
pattern from product spec sections 30-31. A single discovery run (a
handful of HTTP requests through `SecureHttpClient`, each with its own
timeout) is fast enough that the added complexity of a job queue isn't
worth it yet. This is revisited once Phase 5's parsing and later phases'
analysis are added to the same request — that's when a single "discover"
call could plausibly take long enough to need async handling.

## 6. Phase 3 scope

Adds the SSRF-hardened outbound HTTP layer, ahead of any code that
actually needs to fetch a user-supplied URL (that's Phase 4):

- `SecureHttpClient` (`infrastructure/http`) — the sole permitted way to
  fetch an external, user-influenced URL. Full design and the enforced
  protections are documented in `docs/security.md`; the SSRF-specific
  design decision (including a known, documented limitation around DNS
  rebinding) is in `docs/adr/ADR-004-ssrf-protection-strategy.md`.
- `IpAddressValidator` — pure, directly-testable logic deciding whether a
  *resolved* IP address is safe to connect to.
- `SecureHttpClientProperties` (`apilens.http.*`) — every SSRF-relevant
  limit (timeouts, max response size, max redirects, allowed content
  types, https-only) in one reviewable place.
- `SecureFetchResult` — a result value, not an exception, so failed
  attempts can be recorded (URL, outcome, message) rather than only
  succeeding or throwing; this is what Phase 4's `DiscoveryAttempt`
  persistence will consume.
- Unit tests: `IpAddressValidatorTest` (pure blocklist logic against IPv4,
  IPv6, and IPv4-mapped literals) and `SecureHttpClientTest` (HTTP
  mechanics — success, 4xx, redirects, redirect limits, oversized
  responses, disallowed content types, timeouts — against a local JDK
  `HttpServer`, plus the production blocked-IP and https-only behavior).

Nothing calls `SecureHttpClient` yet — that's Phase 4 (`DiscoveryEngine` /
`OpenApiDiscoveryStrategy`), so it's currently dead code from the
running application's point of view, verified only by its own tests.

## 7. Phase 2 scope

Adds API management on top of the Phase 1 foundation:

- `users` and `apis` tables (Flyway `V2__api_management.sql`). Every API
  has a non-null `owner_id`. Since real authentication doesn't exist yet,
  a single system user is seeded and used as the owner for all requests —
  see `docs/adr/ADR-003-ownership-without-auth.md` for why this is modeled
  now rather than deferred.
- `POST/GET /api/apis`, `GET/DELETE /api/apis/{id}` — thin controller,
  business logic (ownership resolution, persistence) in
  `ApiManagementService`.
- Bean-validated `CreateApiRequest` (name required; `baseUrl`,
  `openApiUrl`, `documentationUrl` must be well-formed absolute http(s)
  URLs if present). This is a basic well-formedness check only — the
  SSRF-aware validation that actually governs what gets *fetched* is
  `SecureHttpClient`, introduced in Phase 3.
- A global `@RestControllerAdvice` (`GlobalExceptionHandler`) producing
  the `{timestamp, status, error, message, path}` error shape from the
  product spec for not-found, validation, and unexpected errors, without
  leaking stack traces.
- Frontend: a real "My APIs" dashboard — list, add, remove — replacing the
  Phase 1 placeholder page. The Phase 1 status check now lives in the
  header as a small connectivity indicator.

## 8. Phase 1 scope

What exists as of Phase 1:

- Spring Boot 3 / Java 21 backend, packaged with Maven.
- PostgreSQL, managed via Flyway (`backend/src/main/resources/db/migration`).
- A single `/api/status` endpoint and Spring Boot Actuator's
  `/actuator/health`, used to confirm the backend is reachable and has
  completed startup (including migrations).
- A React 18 + TypeScript (Vite) frontend with one page, which calls
  `/api/status` and renders the result — proving the full request path
  (browser → nginx/dev-server proxy → backend → Postgres → response) works.
- Docker Compose wiring `postgres`, `backend`, and `frontend` together.
- GitHub Actions CI building and testing both backend and frontend, and
  building both Docker images.

What was deliberately not built yet at this point: API CRUD (added in
Phase 2, above), the secure HTTP client, discovery, OpenAPI parsing,
analysis, snapshots, diffing. See `docs/adr/` and the phase list in the
project brief for the build order.

## 9. Planned request flows (for later phases)

- **Discovery flow**: implemented as of Phase 4 — see section 4 above.
- **Contract analysis flow**: implemented for parsing/persistence as of
  Phase 5 (section 3 above); quality analysis and diffing against a prior
  snapshot are still Phase 7 and Phase 9-10.
- **Snapshot flow**: normalized contract is canonicalized and SHA-256
  hashed; a new snapshot row is only created when the hash changes.

These flows are documented ahead of implementation so each phase's PR can
be checked against the intended design rather than invented ad hoc.
