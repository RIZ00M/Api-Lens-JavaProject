# ADR-001: Use Spring Boot for the backend

## Status
Accepted

## Context
API Lens needs a backend that can: serve a REST API, perform outbound HTTP
calls to arbitrary (untrusted) third-party APIs under strict controls,
persist a fairly relational domain model (APIs, contracts, endpoints,
parameters, schemas, findings, changes) in PostgreSQL, run background/async
analysis jobs, and expose operational metrics — while remaining
approachable to review as a portfolio project.

## Decision
Use Java 21 with Spring Boot 3 as the backend framework, with:
- Spring Web for the REST API,
- Spring WebFlux's `WebClient` specifically as the transport underlying the
  `SecureHttpClient` component (not for serving the app — the app remains a
  standard servlet-based Spring MVC application),
- Spring Data JPA + Hibernate for persistence,
- Flyway for schema migrations,
- Bean Validation for request validation.

## Consequences
- Positive: mature ecosystem for exactly the cross-cutting concerns this
  project needs (validation, actuator health/metrics, structured
  configuration, testing support via `spring-boot-starter-test` and
  Testcontainers integration).
- Positive: `WebClient` supports fine-grained control over connection
  timeouts, read timeouts, response size limits and redirect handling,
  which the SSRF-hardened `SecureHttpClient` (Phase 3) depends on.
- Trade-off: mixing a servlet-stack application with a reactive HTTP client
  is slightly unusual; this is intentional and scoped narrowly — only
  `SecureHttpClient` and its direct collaborators use reactive types, and
  application/domain code stays synchronous and blocking. This is called
  out in `docs/architecture.md` so it doesn't read as an accident.
- Neutral: introduces a JVM/Maven build in CI, in addition to the frontend's
  Node/npm build.
