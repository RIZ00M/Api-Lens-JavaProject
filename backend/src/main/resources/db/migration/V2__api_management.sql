-- V2: API management.
--
-- Introduces `users` (minimal, so ownership exists per the architecture
-- decision even before real authentication is implemented — see
-- docs/adr/ADR-003-ownership-without-auth.md) and `apis`, the first real
-- domain table. Every API belongs to a user; for now all APIs are created
-- under a single seeded system user.

CREATE TABLE users (
    id           UUID PRIMARY KEY,
    email        VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE apis (
    id                  UUID PRIMARY KEY,
    owner_id            UUID NOT NULL REFERENCES users(id),
    name                VARCHAR(255) NOT NULL,
    base_url            VARCHAR(2048) NOT NULL,
    open_api_url        VARCHAR(2048),
    documentation_url   VARCHAR(2048),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_apis_owner_id ON apis (owner_id);

-- Seeded system user, used as the owner of every API until real
-- authentication (JWT/OAuth2) is implemented.
INSERT INTO users (id, email, display_name)
VALUES ('00000000-0000-0000-0000-000000000001', 'local-dev@api-lens.local', 'Local Development User');
