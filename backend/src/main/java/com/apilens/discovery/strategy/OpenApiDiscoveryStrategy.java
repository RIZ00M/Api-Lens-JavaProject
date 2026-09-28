package com.apilens.discovery.strategy;

import com.apilens.discovery.DiscoveryProperties;
import com.apilens.discovery.model.DiscoveryAttemptOutcome;
import com.apilens.discovery.model.DiscoveryContext;
import com.apilens.domain.model.Confidence;
import com.apilens.infrastructure.http.SecureFetchResult;
import com.apilens.infrastructure.http.SecureHttpClient;
import com.apilens.infrastructure.openapi.SpecificationSniffer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Tries a fixed, configurable list of standard OpenAPI/Swagger locations
 * relative to the API's base URL (product spec section 5, list itself in
 * apilens.discovery.standard-openapi-paths / DiscoveryProperties). Runs
 * after UserProvidedSpecStrategy, and -- like it -- stops at the very
 * first successful candidate rather than trying every path once one
 * works; this is the boundary that keeps discovery from becoming
 * uncontrolled crawling (section 38).
 */
@Component
@Order(2)
public class OpenApiDiscoveryStrategy implements DiscoveryStrategy {

    private static final String METHOD_PREFIX = "STANDARD_PATH:";

    private final SecureHttpClient httpClient;
    private final DiscoveryProperties properties;

    public OpenApiDiscoveryStrategy(SecureHttpClient httpClient, DiscoveryProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    @Override
    public String name() {
        return "STANDARD_PATH";
    }

    @Override
    public Result attempt(DiscoveryContext context) {
        String base = stripTrailingSlash(context.baseUrl());
        List<DiscoveryAttemptOutcome> attempts = new ArrayList<>();

        for (String path : properties.standardOpenApiPaths()) {
            String candidateUrl = base + path;
            SecureFetchResult fetchResult = httpClient.fetch(candidateUrl);
            DiscoveryAttemptOutcome outcome = toOutcome(candidateUrl, path, fetchResult);
            attempts.add(outcome);

            if (outcome.success()) {
                return new Result(attempts, Optional.of(outcome));
            }
        }

        return new Result(attempts, Optional.empty());
    }

    private DiscoveryAttemptOutcome toOutcome(String url, String path, SecureFetchResult fetchResult) {
        String method = METHOD_PREFIX + path;

        if (!fetchResult.isSuccess()) {
            return new DiscoveryAttemptOutcome(url, method, fetchResult.statusCode(), fetchResult.contentType(),
                    fetchResult.sizeBytes(), false, fetchResult.message(), null, null, null);
        }

        Optional<String> detectedFormat = SpecificationSniffer.sniff(fetchResult.bodyAsString());
        if (detectedFormat.isEmpty()) {
            return new DiscoveryAttemptOutcome(url, method, fetchResult.statusCode(), fetchResult.contentType(),
                    fetchResult.sizeBytes(), false,
                    "Response did not look like an OpenAPI/Swagger document", null, null, null);
        }

        return new DiscoveryAttemptOutcome(url, method, fetchResult.statusCode(), fetchResult.contentType(),
                fetchResult.sizeBytes(), true, null, Confidence.HIGH, detectedFormat.get(), fetchResult.body());
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
