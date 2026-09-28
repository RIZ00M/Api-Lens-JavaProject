package com.apilens.discovery.strategy;

import com.apilens.discovery.model.DiscoveryAttemptOutcome;
import com.apilens.discovery.model.DiscoveryContext;

import java.util.List;
import java.util.Optional;

/**
 * One way of trying to locate an API's specification. Implementations
 * are run in order by DiscoveryEngine, which stops at the first
 * successful one (product spec section 38) -- a strategy itself may try
 * several candidate URLs internally (see OpenApiDiscoveryStrategy) but
 * must stop at its own first success too, never trying every candidate
 * regardless of outcome.
 */
public interface DiscoveryStrategy {

    /** Short, stable name used as part of DiscoveryAttempt.discoveryMethod. */
    default String name() {
        return getClass().getSimpleName();
    }

    Result attempt(DiscoveryContext context);

    /**
     * @param attempts          every URL this strategy tried, in order, whether it succeeded or not
     * @param successfulAttempt present if one of {@code attempts} found a usable specification
     */
    record Result(List<DiscoveryAttemptOutcome> attempts, Optional<DiscoveryAttemptOutcome> successfulAttempt) {

        public static Result noAttempts() {
            return new Result(List.of(), Optional.empty());
        }
    }
}
