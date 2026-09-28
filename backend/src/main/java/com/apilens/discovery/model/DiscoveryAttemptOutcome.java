package com.apilens.discovery.model;

import com.apilens.domain.model.Confidence;

/**
 * What happened when one candidate URL was tried. One of these is
 * produced per URL a strategy attempts, regardless of success --
 * DiscoveryApplicationService persists every one of them as a
 * DiscoveryAttempt row.
 */
public record DiscoveryAttemptOutcome(
        String url,
        String discoveryMethod,
        Integer httpStatus,
        String contentType,
        long responseSizeBytes,
        boolean success,
        String errorMessage,
        Confidence confidence,
        String detectedFormat,
        byte[] body
) {
}
