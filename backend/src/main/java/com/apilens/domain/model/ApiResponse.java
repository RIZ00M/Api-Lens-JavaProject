package com.apilens.domain.model;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * One documented response for an endpoint, keyed by status code (or
 * "default"). `content` is a JSON object mapping content type to that
 * content type's schema, kept as text for the same reason as
 * ApiSchema.definition (see migration V4).
 */
@Entity
@Table(name = "api_responses")
public class ApiResponse {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private ApiEndpoint endpoint;

    @Column(name = "status_code", nullable = false)
    private String statusCode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    protected ApiResponse() {
        // JPA
    }

    public ApiResponse(UUID id, String statusCode, String description, String content) {
        this.id = id;
        this.statusCode = statusCode;
        this.description = description;
        this.content = content;
    }

    void assignEndpoint(ApiEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public UUID getId() {
        return id;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public String getDescription() {
        return description;
    }

    public String getContent() {
        return content;
    }
}
