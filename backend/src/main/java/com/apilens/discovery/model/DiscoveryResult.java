package com.apilens.discovery.model;

import com.apilens.domain.model.Confidence;

import java.util.List;

/**
 * Overall outcome of running the DiscoveryEngine once for an API (product
 * spec section 38). `attempts` includes every URL tried, by every
 * strategy, whether or not it succeeded -- this is what gets persisted
 * for provenance regardless of the final found/not-found verdict.
 * `specificationBody` is the raw bytes of the successful fetch, handed
 * straight to ContractParser by the caller (Phase 5) rather than
 * re-fetching the same URL a second time.
 */
public record DiscoveryResult(
        boolean specificationFound,
        String sourceUrl,
        String specificationFormat,
        Confidence confidence,
        byte[] specificationBody,
        List<DiscoveryAttemptOutcome> attempts
) {
}
