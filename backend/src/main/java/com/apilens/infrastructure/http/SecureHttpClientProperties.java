package com.apilens.infrastructure.http;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Bound from `apilens.http.*` (see application.yml). Kept as one property
 * object, rather than scattered @Value fields, so every SSRF-relevant
 * limit is visible and reviewable in one place.
 */
@ConfigurationProperties(prefix = "apilens.http")
public record SecureHttpClientProperties(
        boolean httpsOnly,
        long maxResponseSizeBytes,
        int maxRedirects,
        long connectTimeoutMs,
        long readTimeoutMs,
        List<String> allowedContentTypePrefixes
) {
    public SecureHttpClientProperties {
        if (allowedContentTypePrefixes == null) {
            allowedContentTypePrefixes = List.of();
        }
    }
}
