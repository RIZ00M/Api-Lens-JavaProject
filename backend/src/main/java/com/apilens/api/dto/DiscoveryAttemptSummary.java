package com.apilens.api.dto;

import com.apilens.domain.model.Confidence;
import com.apilens.domain.model.DiscoveryAttempt;

import java.time.Instant;
import java.util.UUID;

public record DiscoveryAttemptSummary(
        UUID id,
        String url,
        String discoveryMethod,
        Integer httpStatus,
        String contentType,
        long responseSizeBytes,
        boolean success,
        String errorMessage,
        Confidence confidence,
        String detectedFormat,
        Instant attemptedAt
) {
    public static DiscoveryAttemptSummary from(DiscoveryAttempt attempt) {
        return new DiscoveryAttemptSummary(
                attempt.getId(),
                attempt.getUrl(),
                attempt.getDiscoveryMethod(),
                attempt.getHttpStatus(),
                attempt.getContentType(),
                attempt.getResponseSizeBytes(),
                attempt.isSuccess(),
                attempt.getErrorMessage(),
                attempt.getConfidence(),
                attempt.getDetectedFormat(),
                attempt.getAttemptedAt()
        );
    }
}
