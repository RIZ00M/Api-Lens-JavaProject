package com.apilens.api.dto;

import java.time.Instant;

/**
 * Minimal application status payload used by the frontend to confirm
 * connectivity to the backend during local development.
 *
 * This is intentionally separate from Spring Boot Actuator's
 * /actuator/health endpoint: /actuator/health is for infrastructure
 * (load balancers, orchestrators) while /api/status is a stable,
 * public-facing contract the frontend is meant to depend on.
 */
public record StatusResponse(
        String application,
        String status,
        String version,
        Instant timestamp
) {
}
