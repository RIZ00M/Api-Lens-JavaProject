package com.apilens.discovery.strategy;

import com.apilens.discovery.model.DiscoveryAttemptOutcome;
import com.apilens.discovery.model.DiscoveryContext;
import com.apilens.domain.model.Confidence;
import com.apilens.infrastructure.http.SecureFetchResult;
import com.apilens.infrastructure.http.SecureHttpClient;
import com.apilens.infrastructure.openapi.SpecificationSniffer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Tries the OpenAPI URL the user explicitly supplied when registering the
 * API, if any. Runs first (product spec section 38: "OpenAPI direct URL
 * succeeds: stop.") since an explicit URL is the strongest possible
 * signal -- confidence is HIGH whenever it resolves to a real spec.
 */
@Component
@Order(1)
public class UserProvidedSpecStrategy implements DiscoveryStrategy {

    private static final String METHOD = "USER_PROVIDED_OPENAPI_URL";

    private final SecureHttpClient httpClient;

    public UserProvidedSpecStrategy(SecureHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String name() {
        return METHOD;
    }

    @Override
    public Result attempt(DiscoveryContext context) {
        if (context.openApiUrlHint() == null || context.openApiUrlHint().isBlank()) {
            return Result.noAttempts();
        }

        SecureFetchResult fetchResult = httpClient.fetch(context.openApiUrlHint());
        DiscoveryAttemptOutcome outcome = toOutcome(context.openApiUrlHint(), fetchResult);

        return outcome.success()
                ? new Result(List.of(outcome), Optional.of(outcome))
                : new Result(List.of(outcome), Optional.empty());
    }

    private DiscoveryAttemptOutcome toOutcome(String url, SecureFetchResult fetchResult) {
        if (!fetchResult.isSuccess()) {
            return new DiscoveryAttemptOutcome(url, METHOD, fetchResult.statusCode(), fetchResult.contentType(),
                    fetchResult.sizeBytes(), false, fetchResult.message(), null, null, null);
        }

        Optional<String> detectedFormat = SpecificationSniffer.sniff(fetchResult.bodyAsString());
        if (detectedFormat.isEmpty()) {
            return new DiscoveryAttemptOutcome(url, METHOD, fetchResult.statusCode(), fetchResult.contentType(),
                    fetchResult.sizeBytes(), false,
                    "Response did not look like an OpenAPI/Swagger document", null, null, null);
        }

        return new DiscoveryAttemptOutcome(url, METHOD, fetchResult.statusCode(), fetchResult.contentType(),
                fetchResult.sizeBytes(), true, null, Confidence.HIGH, detectedFormat.get(), fetchResult.body());
    }
}
