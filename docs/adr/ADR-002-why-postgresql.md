# ADR-002: Use PostgreSQL as the primary datastore

## Status
Accepted

## Context
API Lens's domain is fundamentally relational: an API has many contracts
(snapshots over time), a contract has many endpoints, an endpoint has many
parameters/responses, and quality findings and contract changes each
reference specific endpoints/parameters. At the same time, some data
(the raw OpenAPI document, complex nested schema structures) is naturally
document-shaped and doesn't benefit from full normalization.

## Decision
Use PostgreSQL for both: normalized relational tables for entities that
are commonly filtered, joined, or aggregated (apis, api_contracts,
api_endpoints, api_parameters, quality_findings, contract_changes, ...),
and `JSONB` columns for the raw specification and other document-shaped
data that is stored and displayed but not queried relationally (see
product spec sections 24-25).

## Consequences
- Positive: one database technology to run and test (via Testcontainers)
  covers both relational and document-shaped needs, avoiding a
  polyglot-persistence setup that would add operational complexity
  disproportionate to this project's scope.
- Positive: JSONB supports indexing and querying if a future phase needs
  to query inside the raw specification, without a storage migration.
- Trade-off: schema evolution for the relational tables goes through
  Flyway migrations, which is more upfront ceremony than a schemaless
  store — considered worthwhile given how central "compare two contracts
  precisely" is to the product.
