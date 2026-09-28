-- V1: Baseline migration.
--
-- Phase 1 only establishes that Flyway is wired up correctly end-to-end
-- (backend -> Flyway -> PostgreSQL). Domain tables (apis, api_contracts,
-- api_endpoints, etc. -- see docs/architecture.md) are introduced starting
-- in Phase 2, one migration per phase, so schema history stays readable.

CREATE TABLE schema_info (
    id          SMALLINT PRIMARY KEY DEFAULT 1,
    note        TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT schema_info_singleton CHECK (id = 1)
);

INSERT INTO schema_info (id, note)
VALUES (1, 'API Lens schema initialized (Phase 1).');
