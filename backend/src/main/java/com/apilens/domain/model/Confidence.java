package com.apilens.domain.model;

/**
 * How sure API Lens is about a piece of discovered information (product
 * spec section 14, "Discovery Provenance"). Used on DiscoveryAttempt now;
 * will also be attached per-endpoint once discovery sources beyond
 * OpenAPI (documentation pages, observed traffic) exist.
 */
public enum Confidence {
    HIGH,
    MEDIUM,
    LOW
}
