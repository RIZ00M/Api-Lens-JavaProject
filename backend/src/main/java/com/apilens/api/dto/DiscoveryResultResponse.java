package com.apilens.api.dto;

import com.apilens.application.service.DiscoveryApplicationService;
import com.apilens.discovery.model.DiscoveryResult;
import com.apilens.domain.model.Confidence;

import java.util.List;
import java.util.UUID;

public record DiscoveryResultResponse(
        boolean specificationFound,
        String sourceUrl,
        String specificationFormat,
        Confidence confidence,
        UUID contractId,
        boolean contractParsed,
        List<String> parseMessages,
        List<DiscoveryAttemptSummaryLite> attempts
) {
    public static DiscoveryResultResponse from(DiscoveryApplicationService.DiscoveryOutcome outcome) {
        DiscoveryResult result = outcome.discoveryResult();
        List<DiscoveryAttemptSummaryLite> attempts = result.attempts().stream()
                .map(a -> new DiscoveryAttemptSummaryLite(
                        a.url(), a.discoveryMethod(), a.httpStatus(), a.success(), a.errorMessage()))
                .toList();

        UUID contractId = outcome.ingestionOutcome()
                .filter(i -> i.isSuccess())
                .map(i -> i.contract().getId())
                .orElse(null);
        boolean contractParsed = contractId != null;
        List<String> parseMessages = outcome.ingestionOutcome().map(i -> i.messages()).orElse(List.of());

        return new DiscoveryResultResponse(
                result.specificationFound(), result.sourceUrl(), result.specificationFormat(),
                result.confidence(), contractId, contractParsed, parseMessages, attempts);
    }

    /** Trimmed view for the discover-response body; the full record is available via GET .../discovery-attempts. */
    public record DiscoveryAttemptSummaryLite(
            String url, String discoveryMethod, Integer httpStatus, boolean success, String errorMessage) {
    }
}
