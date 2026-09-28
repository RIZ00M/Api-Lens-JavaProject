package com.apilens.discovery.model;

/**
 * Where a discovered specification (or, eventually, an individual
 * endpoint) came from — product spec section 14. Only OPENAPI and
 * USER_PROVIDED are produced by any strategy in Phase 4; the rest are
 * forward-declared extension points (section 39, "Future Discovery
 * Features") so later strategies have a stable enum to slot into rather
 * than one invented ad hoc per feature.
 */
public enum DiscoverySource {
    OPENAPI,
    DOCUMENTATION,
    USER_PROVIDED,
    OBSERVED,
    INFERRED
}
