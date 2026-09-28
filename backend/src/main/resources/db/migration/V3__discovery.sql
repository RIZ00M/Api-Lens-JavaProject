-- V3: Discovery attempts.
--
-- One row per URL actually tried during discovery (successful or not),
-- per product spec section 5 ("For every discovery attempt, record...").
-- This table is the provenance the UI shows in the "Discovery" view added
-- alongside the API explorer in a later phase.

CREATE TABLE discovery_attempts (
    id                    UUID PRIMARY KEY,
    api_id                UUID NOT NULL REFERENCES apis(id) ON DELETE CASCADE,
    url                   VARCHAR(2048) NOT NULL,
    discovery_method      VARCHAR(100) NOT NULL,
    http_status           INTEGER,
    content_type          VARCHAR(255),
    response_size_bytes   BIGINT NOT NULL DEFAULT 0,
    success               BOOLEAN NOT NULL,
    error_message         TEXT,
    confidence            VARCHAR(20),
    detected_format       VARCHAR(100),
    attempted_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_discovery_attempts_api_id ON discovery_attempts (api_id, attempted_at DESC);
