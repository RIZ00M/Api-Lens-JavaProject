-- V4: Normalized contract model (product spec sections 7-8, 24-25).
--
-- Relational tables for entities that are commonly filtered/joined
-- (contracts, endpoints, parameters, responses, servers, security
-- schemes). Nested schema *content* (request/response bodies, reusable
-- component schemas) is stored as serialized JSON text rather than fully
-- decomposed into per-property rows -- section 25 explicitly allows this
-- for "complex schema structures", and full JSON-Schema normalization
-- (refs, oneOf/allOf, nested objects) would add a lot of ceremony for
-- data the UI only ever needs to render, not query relationally.
--
-- These content columns are plain TEXT for now, not `jsonb`. Making them
-- real jsonb columns queryable via JPA needs a Hibernate custom type
-- (e.g. hypersistence-utils) that isn't part of this build yet -- see
-- docs/adr/ADR-005-openapi-parsing-strategy.md.

CREATE TABLE api_contracts (
    id                     UUID PRIMARY KEY,
    api_id                 UUID NOT NULL REFERENCES apis(id) ON DELETE CASCADE,
    specification_version  VARCHAR(50),
    title                  VARCHAR(255),
    description            TEXT,
    base_url               VARCHAR(2048),
    source_url             VARCHAR(2048) NOT NULL,
    raw_specification      TEXT NOT NULL,
    discovered_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_api_contracts_api_id ON api_contracts (api_id, discovered_at DESC);

CREATE TABLE api_servers (
    id           UUID PRIMARY KEY,
    contract_id  UUID NOT NULL REFERENCES api_contracts(id) ON DELETE CASCADE,
    url          VARCHAR(2048) NOT NULL,
    description  TEXT
);
CREATE INDEX idx_api_servers_contract_id ON api_servers (contract_id);

CREATE TABLE api_security_schemes (
    id             UUID PRIMARY KEY,
    contract_id    UUID NOT NULL REFERENCES api_contracts(id) ON DELETE CASCADE,
    name           VARCHAR(255) NOT NULL,
    type           VARCHAR(50),
    scheme         VARCHAR(50),
    bearer_format  VARCHAR(50),
    in_location    VARCHAR(50),
    key_name       VARCHAR(255),
    description    TEXT
);
CREATE INDEX idx_api_security_schemes_contract_id ON api_security_schemes (contract_id);

CREATE TABLE api_schemas (
    id           UUID PRIMARY KEY,
    contract_id  UUID NOT NULL REFERENCES api_contracts(id) ON DELETE CASCADE,
    name         VARCHAR(255) NOT NULL,
    definition   TEXT NOT NULL
);
CREATE INDEX idx_api_schemas_contract_id ON api_schemas (contract_id);

CREATE TABLE api_endpoints (
    id            UUID PRIMARY KEY,
    contract_id   UUID NOT NULL REFERENCES api_contracts(id) ON DELETE CASCADE,
    path          VARCHAR(2048) NOT NULL,
    method        VARCHAR(10) NOT NULL,
    operation_id  VARCHAR(255),
    summary       VARCHAR(1024),
    description   TEXT,
    deprecated    BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX idx_api_endpoints_contract_id ON api_endpoints (contract_id);
CREATE INDEX idx_api_endpoints_path ON api_endpoints (path);

CREATE TABLE api_endpoint_tags (
    endpoint_id  UUID NOT NULL REFERENCES api_endpoints(id) ON DELETE CASCADE,
    tag          VARCHAR(255) NOT NULL
);
CREATE INDEX idx_api_endpoint_tags_endpoint_id ON api_endpoint_tags (endpoint_id);

CREATE TABLE api_endpoint_security_schemes (
    endpoint_id          UUID NOT NULL REFERENCES api_endpoints(id) ON DELETE CASCADE,
    security_scheme_name VARCHAR(255) NOT NULL
);
CREATE INDEX idx_api_endpoint_security_endpoint_id ON api_endpoint_security_schemes (endpoint_id);

CREATE TABLE api_parameters (
    id             UUID PRIMARY KEY,
    endpoint_id    UUID NOT NULL REFERENCES api_endpoints(id) ON DELETE CASCADE,
    name           VARCHAR(255) NOT NULL,
    location       VARCHAR(20) NOT NULL,
    required       BOOLEAN NOT NULL DEFAULT false,
    type           VARCHAR(50),
    format         VARCHAR(50),
    description    TEXT,
    default_value  VARCHAR(1024)
);
CREATE INDEX idx_api_parameters_endpoint_id ON api_parameters (endpoint_id);

CREATE TABLE api_parameter_enum_values (
    parameter_id  UUID NOT NULL REFERENCES api_parameters(id) ON DELETE CASCADE,
    value         VARCHAR(1024) NOT NULL
);
CREATE INDEX idx_api_parameter_enum_values_parameter_id ON api_parameter_enum_values (parameter_id);

CREATE TABLE api_request_bodies (
    id           UUID PRIMARY KEY,
    endpoint_id  UUID NOT NULL UNIQUE REFERENCES api_endpoints(id) ON DELETE CASCADE,
    required     BOOLEAN NOT NULL DEFAULT false,
    content      TEXT NOT NULL   -- JSON: { contentType: schemaJson, ... }
);

CREATE TABLE api_responses (
    id           UUID PRIMARY KEY,
    endpoint_id  UUID NOT NULL REFERENCES api_endpoints(id) ON DELETE CASCADE,
    status_code  VARCHAR(10) NOT NULL,   -- e.g. "200", "404", "default"
    description  TEXT,
    content      TEXT NOT NULL           -- JSON: { contentType: schemaJson, ... }
);
CREATE INDEX idx_api_responses_endpoint_id ON api_responses (endpoint_id);
