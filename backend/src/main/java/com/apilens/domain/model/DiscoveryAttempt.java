package com.apilens.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A single, immutable record of "we tried to fetch this URL as part of
 * discovering an API's specification, and here's what happened" — the
 * provenance trail described in product spec section 5. Written once,
 * never updated.
 */
@Entity
@Table(name = "discovery_attempts")
public class DiscoveryAttempt {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "api_id", nullable = false)
    private Api api;

    @Column(nullable = false)
    private String url;

    @Column(name = "discovery_method", nullable = false)
    private String discoveryMethod;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "response_size_bytes", nullable = false)
    private long responseSizeBytes;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "error_message")
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    private Confidence confidence;

    @Column(name = "detected_format")
    private String detectedFormat;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    protected DiscoveryAttempt() {
        // JPA
    }

    public DiscoveryAttempt(UUID id, Api api, String url, String discoveryMethod, Integer httpStatus,
                             String contentType, long responseSizeBytes, boolean success, String errorMessage,
                             Confidence confidence, String detectedFormat) {
        this.id = id;
        this.api = api;
        this.url = url;
        this.discoveryMethod = discoveryMethod;
        this.httpStatus = httpStatus;
        this.contentType = contentType;
        this.responseSizeBytes = responseSizeBytes;
        this.success = success;
        this.errorMessage = errorMessage;
        this.confidence = confidence;
        this.detectedFormat = detectedFormat;
    }

    @PrePersist
    void onCreate() {
        this.attemptedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Api getApi() {
        return api;
    }

    public String getUrl() {
        return url;
    }

    public String getDiscoveryMethod() {
        return discoveryMethod;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getContentType() {
        return contentType;
    }

    public long getResponseSizeBytes() {
        return responseSizeBytes;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Confidence getConfidence() {
        return confidence;
    }

    public String getDetectedFormat() {
        return detectedFormat;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }
}
