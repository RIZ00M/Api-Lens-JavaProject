package com.apilens.api.dto;

import java.time.Instant;

/**
 * Standard error payload for every non-2xx response. Mirrors the shape
 * defined in the product spec's error-handling section so the frontend
 * (and any future API consumer) can rely on one consistent contract
 * instead of parsing framework-default error bodies.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
