package com.apilens.discovery;

import com.apilens.discovery.model.DiscoveryAttemptOutcome;
import com.apilens.discovery.model.DiscoveryContext;
import com.apilens.discovery.model.DiscoveryResult;
import com.apilens.discovery.strategy.DiscoveryStrategy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs each configured DiscoveryStrategy in order (Spring injects the
 * list respecting each strategy's @Order) and stops at the first one
 * that finds a usable specification -- this "when to stop" decision is
 * the engine's whole job (product spec section 38); the strategies
 * themselves only know how to try their own kind of URL.
 */
@Component
public class DiscoveryEngine {

    private final List<DiscoveryStrategy> strategies;

    public DiscoveryEngine(List<DiscoveryStrategy> strategies) {
        this.strategies = strategies;
    }

    public DiscoveryResult discover(DiscoveryContext context) {
        List<DiscoveryAttemptOutcome> allAttempts = new ArrayList<>();

        for (DiscoveryStrategy strategy : strategies) {
            DiscoveryStrategy.Result result = strategy.attempt(context);
            allAttempts.addAll(result.attempts());

            if (result.successfulAttempt().isPresent()) {
                DiscoveryAttemptOutcome success = result.successfulAttempt().get();
                return new DiscoveryResult(true, success.url(), success.detectedFormat(),
                        success.confidence(), success.body(), allAttempts);
            }
        }

        return new DiscoveryResult(false, null, null, null, null, allAttempts);
    }
}
