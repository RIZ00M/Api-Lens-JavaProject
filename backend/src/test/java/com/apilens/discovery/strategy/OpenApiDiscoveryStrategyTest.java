package com.apilens.discovery.strategy;

import com.apilens.discovery.DiscoveryProperties;
import com.apilens.discovery.model.DiscoveryContext;
import com.apilens.infrastructure.http.SecureFetchResult;
import com.apilens.infrastructure.http.SecureHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenApiDiscoveryStrategyTest {

    @Mock
    private SecureHttpClient httpClient;

    private static SecureFetchResult notFound(String url) {
        return new SecureFetchResult(SecureFetchResult.Outcome.HTTP_ERROR, url, 404, null, null, 0, 0, "not found");
    }

    @Test
    void stopsAtTheFirstPathThatSucceeds() {
        DiscoveryProperties props = new DiscoveryProperties(List.of("/openapi.json", "/swagger.json", "/v2/api-docs"));
        OpenApiDiscoveryStrategy strategy = new OpenApiDiscoveryStrategy(httpClient, props);

        byte[] spec = "{\"openapi\":\"3.0.3\"}".getBytes(StandardCharsets.UTF_8);
        when(httpClient.fetch("https://api.example.com/openapi.json")).thenReturn(notFound("https://api.example.com/openapi.json"));
        when(httpClient.fetch("https://api.example.com/swagger.json")).thenReturn(
                new SecureFetchResult(SecureFetchResult.Outcome.SUCCESS, "https://api.example.com/swagger.json",
                        200, "application/json", spec, spec.length, 0, null));

        DiscoveryContext context = new DiscoveryContext(UUID.randomUUID(), "https://api.example.com", null, null);
        DiscoveryStrategy.Result result = strategy.attempt(context);

        assertThat(result.successfulAttempt()).isPresent();
        assertThat(result.successfulAttempt().get().url()).isEqualTo("https://api.example.com/swagger.json");
        assertThat(result.attempts()).hasSize(2); // stopped before trying /v2/api-docs
        verify(httpClient, times(0)).fetch("https://api.example.com/v2/api-docs");
    }

    @Test
    void returnsEveryAttemptWhenNothingIsFound() {
        DiscoveryProperties props = new DiscoveryProperties(List.of("/openapi.json", "/swagger.json"));
        OpenApiDiscoveryStrategy strategy = new OpenApiDiscoveryStrategy(httpClient, props);

        when(httpClient.fetch("https://api.example.com/openapi.json")).thenReturn(notFound("https://api.example.com/openapi.json"));
        when(httpClient.fetch("https://api.example.com/swagger.json")).thenReturn(notFound("https://api.example.com/swagger.json"));

        DiscoveryContext context = new DiscoveryContext(UUID.randomUUID(), "https://api.example.com/", null, null);
        DiscoveryStrategy.Result result = strategy.attempt(context);

        assertThat(result.successfulAttempt()).isEmpty();
        assertThat(result.attempts()).hasSize(2);
    }

    @Test
    void stripsTrailingSlashFromBaseUrlBeforeAppendingPaths() {
        DiscoveryProperties props = new DiscoveryProperties(List.of("/openapi.json"));
        OpenApiDiscoveryStrategy strategy = new OpenApiDiscoveryStrategy(httpClient, props);
        when(httpClient.fetch("https://api.example.com/openapi.json")).thenReturn(notFound("https://api.example.com/openapi.json"));

        DiscoveryContext context = new DiscoveryContext(UUID.randomUUID(), "https://api.example.com/", null, null);
        strategy.attempt(context);

        verify(httpClient).fetch("https://api.example.com/openapi.json");
    }
}
