package com.apilens.infrastructure.http;

import java.nio.charset.StandardCharsets;

/**
 * Result of a {@link SecureHttpClient} fetch attempt.
 *
 * Deliberately a plain data result rather than something thrown as an
 * exception: callers (starting with the discovery engine in Phase 4) need
 * to record every attempt — success or failure — as a DiscoveryAttempt row
 * (URL, status, error, timestamp; see product spec section 5), so "it
 * failed" has to be a normal, inspectable value rather than control flow.
 */
public record SecureFetchResult(
        Outcome outcome,
        String requestedUrl,
        Integer statusCode,
        String contentType,
        byte[] body,
        long sizeBytes,
        int redirectCount,
        String message
) {
    public enum Outcome {
        SUCCESS,
        BLOCKED_URL,
        DNS_RESOLUTION_FAILED,
        BLOCKED_IP,
        TIMEOUT,
        TOO_MANY_REDIRECTS,
        RESPONSE_TOO_LARGE,
        UNSUPPORTED_CONTENT_TYPE,
        HTTP_ERROR,
        NETWORK_ERROR
    }

    public boolean isSuccess() {
        return outcome == Outcome.SUCCESS;
    }

    public String bodyAsString() {
        return body == null ? null : new String(body, StandardCharsets.UTF_8);
    }

    static SecureFetchResult blocked(Outcome outcome, String url, String message) {
        return new SecureFetchResult(outcome, url, null, null, null, 0, 0, message);
    }
}
