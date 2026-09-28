package com.apilens.discovery;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Bound from `apilens.discovery.*`. Product spec section 5: "The
 * discovery system should be configurable" -- the standard OpenAPI/Swagger
 * paths tried by OpenApiDiscoveryStrategy live here rather than as a
 * hardcoded constant, so they can be extended (or trimmed) per deployment
 * without a code change.
 */
@ConfigurationProperties(prefix = "apilens.discovery")
public record DiscoveryProperties(List<String> standardOpenApiPaths) {

    private static final List<String> DEFAULT_PATHS = List.of(
            "/openapi.json",
            "/openapi.yaml",
            "/openapi.yml",
            "/swagger.json",
            "/swagger.yaml",
            "/swagger.yml",
            "/v3/api-docs",
            "/v3/api-docs.yaml",
            "/v2/api-docs"
    );

    public DiscoveryProperties {
        if (standardOpenApiPaths == null || standardOpenApiPaths.isEmpty()) {
            standardOpenApiPaths = DEFAULT_PATHS;
        }
    }
}
