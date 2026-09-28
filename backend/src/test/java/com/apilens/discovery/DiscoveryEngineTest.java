package com.apilens.discovery;

import com.apilens.discovery.model.DiscoveryAttemptOutcome;
import com.apilens.discovery.model.DiscoveryContext;
import com.apilens.discovery.model.DiscoveryResult;
import com.apilens.discovery.strategy.DiscoveryStrategy;
import com.apilens.domain.model.Confidence;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoveryEngineTest {

    private static DiscoveryAttemptOutcome failedAttempt(String url) {
        return new DiscoveryAttemptOutcome(url, "TEST", 404, null, 0, false, "not found", null, null, null);
    }

    private static DiscoveryAttemptOutcome successfulAttempt(String url) {
        return new DiscoveryAttemptOutcome(url, "TEST", 200, "application/json", 42, true, null,
                Confidence.HIGH, "OpenAPI 3.0.3", null);
    }

    @Test
    void stopsAtTheFirstStrategyThatSucceeds() {
        DiscoveryAttemptOutcome firstFailed = failedAttempt("https://api.example.com/first");
        DiscoveryAttemptOutcome secondSucceeded = successfulAttempt("https://api.example.com/second");

        DiscoveryStrategy strategyA = context -> new DiscoveryStrategy.Result(List.of(firstFailed), Optional.empty());
        DiscoveryStrategy strategyB = context -> new DiscoveryStrategy.Result(List.of(secondSucceeded), Optional.of(secondSucceeded));
        DiscoveryStrategy strategyC = context -> {
            throw new AssertionError("strategyC should never run once strategyB succeeded");
        };

        DiscoveryEngine engine = new DiscoveryEngine(List.of(strategyA, strategyB, strategyC));
        DiscoveryResult result = engine.discover(new DiscoveryContext(UUID.randomUUID(), "https://api.example.com", null, null));

        assertThat(result.specificationFound()).isTrue();
        assertThat(result.sourceUrl()).isEqualTo("https://api.example.com/second");
        assertThat(result.confidence()).isEqualTo(Confidence.HIGH);
        assertThat(result.attempts()).containsExactly(firstFailed, secondSucceeded);
    }

    @Test
    void reportsNotFoundWhenNoStrategySucceeds() {
        DiscoveryAttemptOutcome failedA = failedAttempt("https://api.example.com/a");
        DiscoveryAttemptOutcome failedB = failedAttempt("https://api.example.com/b");

        DiscoveryStrategy strategyA = context -> new DiscoveryStrategy.Result(List.of(failedA), Optional.empty());
        DiscoveryStrategy strategyB = context -> new DiscoveryStrategy.Result(List.of(failedB), Optional.empty());

        DiscoveryEngine engine = new DiscoveryEngine(List.of(strategyA, strategyB));
        DiscoveryResult result = engine.discover(new DiscoveryContext(UUID.randomUUID(), "https://api.example.com", null, null));

        assertThat(result.specificationFound()).isFalse();
        assertThat(result.sourceUrl()).isNull();
        assertThat(result.attempts()).containsExactly(failedA, failedB);
    }
}
