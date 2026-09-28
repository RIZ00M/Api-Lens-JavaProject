package com.apilens.api.dto;

import com.apilens.domain.model.Api;

import java.time.Instant;
import java.util.UUID;

/**
 * API representation returned to clients. A separate type from the
 * domain's Api entity so the REST contract can evolve independently of
 * the persistence model (e.g. once contracts/endpoints/quality summaries
 * are attached to this response in later phases).
 */
public record ApiSummary(
        UUID id,
        String name,
        String baseUrl,
        String openApiUrl,
        String documentationUrl,
        Instant createdAt,
        Instant updatedAt
) {
    public static ApiSummary from(Api api) {
        return new ApiSummary(
                api.getId(),
                api.getName(),
                api.getBaseUrl(),
                api.getOpenApiUrl(),
                api.getDocumentationUrl(),
                api.getCreatedAt(),
                api.getUpdatedAt()
        );
    }
}
