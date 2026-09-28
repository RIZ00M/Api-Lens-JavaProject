package com.apilens.discovery.model;

import java.util.UUID;

/**
 * Everything a DiscoveryStrategy needs to know about the API it's trying
 * to discover a specification for. Deliberately holds only the plain
 * hints the user gave (base URL, optional explicit OpenAPI/docs URLs) —
 * not the JPA entity — so strategies stay decoupled from persistence.
 */
public record DiscoveryContext(
        UUID apiId,
        String baseUrl,
        String openApiUrlHint,
        String documentationUrlHint
) {
}
