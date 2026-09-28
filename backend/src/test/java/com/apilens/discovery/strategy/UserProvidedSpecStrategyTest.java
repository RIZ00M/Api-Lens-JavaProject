package com.apilens.discovery.strategy;

import com.apilens.discovery.model.DiscoveryContext;
import com.apilens.domain.model.Confidence;
import com.apilens.infrastructure.http.SecureFetchResult;
import com.apilens.infrastructure.http.SecureHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProvidedSpecStrategyTest {

    @Mock
    private SecureHttpClient httpClient;

    private UserProvidedSpecStrategy strategy;

    @Test
    void skipsWhenNoHintProvided() {
        strategy = new UserProvidedSpecStrategy(httpClient);
        DiscoveryContext context = new DiscoveryContext(UUID.randomUUID(), "https://api.example.com", null, null);

        DiscoveryStrategy.Result result = strategy.attempt(context);

        assertThat(result.attempts()).isEmpty();
        assertThat(result.successfulAttempt()).isEmpty();
    }

    @Test
    void succeedsWithHighConfidenceWhenHintResolvesToASpec() {
        strategy = new UserProvidedSpecStrategy(httpClient);
        String hintUrl = "https://api.example.com/spec.json";
        byte[] body = "{\"openapi\":\"3.0.3\"}".getBytes(StandardCharsets.UTF_8);
        when(httpClient.fetch(hintUrl)).thenReturn(
                new SecureFetchResult(SecureFetchResult.Outcome.SUCCESS, hintUrl, 200, "application/json",
                        body, body.length, 0, null));

        DiscoveryContext context = new DiscoveryContext(UUID.randomUUID(), "https://api.example.com", hintUrl, null);
        DiscoveryStrategy.Result result = strategy.attempt(context);

        assertThat(result.successfulAttempt()).isPresent();
        assertThat(result.successfulAttempt().get().confidence()).isEqualTo(Confidence.HIGH);
        assertThat(result.successfulAttempt().get().detectedFormat()).isEqualTo("OpenAPI 3.0.3");
    }

    @Test
    void doesNotAcceptA200ResponseThatIsNotActuallyASpec() {
        strategy = new UserProvidedSpecStrategy(httpClient);
        String hintUrl = "https://api.example.com/not-a-spec";
        byte[] body = "<html>hi</html>".getBytes(StandardCharsets.UTF_8);
        when(httpClient.fetch(hintUrl)).thenReturn(
                new SecureFetchResult(SecureFetchResult.Outcome.SUCCESS, hintUrl, 200, "text/html",
                        body, body.length, 0, null));

        DiscoveryContext context = new DiscoveryContext(UUID.randomUUID(), "https://api.example.com", hintUrl, null);
        DiscoveryStrategy.Result result = strategy.attempt(context);

        assertThat(result.successfulAttempt()).isEmpty();
        assertThat(result.attempts()).hasSize(1);
        assertThat(result.attempts().get(0).success()).isFalse();
    }

    @Test
    void recordsBlockedFetchAsAFailedAttempt() {
        strategy = new UserProvidedSpecStrategy(httpClient);
        String hintUrl = "https://169.254.169.254/spec.json";
        when(httpClient.fetch(hintUrl)).thenReturn(
                new SecureFetchResult(SecureFetchResult.Outcome.BLOCKED_IP, hintUrl, null, null, null, 0, 0,
                        "Host resolves to a disallowed address"));

        DiscoveryContext context = new DiscoveryContext(UUID.randomUUID(), "https://api.example.com", hintUrl, null);
        DiscoveryStrategy.Result result = strategy.attempt(context);

        assertThat(result.successfulAttempt()).isEmpty();
        assertThat(result.attempts().get(0).errorMessage()).contains("disallowed address");
    }
}
