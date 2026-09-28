# ADR-005: OpenAPI parsing strategy

## Status
Accepted, with limitations tracked explicitly below

## Context
Product spec section 22 asks for a `ContractParser` interface with
implementations (`OpenApi3Parser`, `Swagger2Parser`) sitting behind our
own domain model, using "a mature OpenAPI parsing library rather than
writing a full parser from scratch." Section 25 explicitly allows JSONB
(here: serialized text) for complex/nested schema structures rather than
requiring full relational decomposition.

## Decisions

1. **Library**: `io.swagger.parser.v3:swagger-parser`, used only inside
   `infrastructure.openapi.OpenApi3Parser`. Nothing outside that class
   touches `io.swagger.v3.oas.models.*` types directly — `ContractParser`
   is the seam, matching the architectural rule already established for
   `SecureHttpClient`.

2. **Schema storage**: reusable component schemas (`ApiSchema.definition`)
   and per-content-type request/response schemas
   (`ApiRequestBody.content`, `ApiResponse.content`) are stored as
   serialized JSON *text*, not decomposed into per-property rows. Fully
   normalizing arbitrary JSON Schema (nested objects, `$ref`, `oneOf` /
   `allOf` / `anyOf`, arrays-of-arrays...) into relational tables would be
   a large amount of complexity for data the UI only ever needs to
   render, never join against — section 25 anticipated exactly this
   trade-off.

3. **Not true `jsonb` columns yet**: these text columns are Postgres
   `TEXT`, not `jsonb`. Making them queryable `jsonb` via JPA needs a
   Hibernate custom type (e.g. `hypersistence-utils`) — a new dependency
   with its own version-compatibility surface. Adding it now, on top of
   an already-unverified new dependency (`swagger-parser`, see below),
   would compound risk for no immediate benefit (nothing queries inside
   these fields yet). Tracked as a follow-up once/if a feature actually
   needs to query into a schema's structure.

4. **Swagger 2.0 support is not implemented end-to-end.** `OpenAPIV3Parser`
   can auto-upgrade a 2.0 document to the 3.x object model, but only with
   the separate `swagger-parser-v2-converter` module on the classpath,
   which is not yet a dependency here. A 2.0 document is still correctly
   *discovered* (Phase 4's `SpecificationSniffer` recognizes the
   `swagger: 2.0` field), it just fails to *parse*, surfacing as a normal
   `ParseResult` failure — degrading gracefully rather than crashing, and
   visible to the caller as `contractParsed: false` alongside
   `specificationFound: true`. Adding the converter module is the
   obvious next step if 2.0 support becomes a priority.

5. **This phase's code could not be compiled in the authoring
   environment.** Unlike every previous phase, adding `swagger-parser`
   introduces a dependency whose exact object-model API (`OpenAPI`,
   `Operation`, `Schema`, `SecurityScheme`, etc.) I could not verify
   against real compiler output — this sandbox has no network access, so
   Maven cannot resolve the new dependency and `mvn test` cannot be run
   here. The object model used (`getPaths()`, `readOperationsMap()`,
   `getComponents().getSchemas()`, etc.) has been stable across
   swagger-core v3 releases for years and I'm confident in the shapes
   used, but this is flagged rather than implied to be verified, per the
   same standard applied to the DNS-rebinding note in ADR-004: an
   explicit, acknowledged gap is preferable to a silent one. **Action for
   whoever picks this up**: run `mvn -B clean verify` (or let CI do it)
   as the first step before building on top of this phase.

## Consequences
- Positive: parsing is isolated, testable (`OpenApi3ParserTest` covers
  endpoints, parameters, request bodies, responses, security requirement
  resolution, and schema extraction from a literal fixture), and doesn't
  leak a third-party object model into the rest of the app.
- Positive: discovery and parsing are chained in one request
  (`ContractIngestionService`), so a successful `POST .../discover`
  either returns a usable `contractId` or tells the caller exactly why
  not (`parseMessages`), without a separate "now parse it" step.
- Trade-off: Swagger 2.0 users get a clear "found but not parsed" result
  rather than a working import, until the converter module is added.
- Risk (explicit): this phase's correctness depends on a library
  integration that has not been build-verified in this environment.
