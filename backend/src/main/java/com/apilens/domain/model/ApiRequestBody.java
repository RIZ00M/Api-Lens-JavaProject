package com.apilens.domain.model;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * An endpoint's request body. `content` is a JSON object mapping content
 * type (e.g. "application/json") to that content type's schema, kept as
 * text for the same reason as ApiSchema.definition (see migration V4).
 */
@Entity
@Table(name = "api_request_bodies")
public class ApiRequestBody {

    @Id
    private UUID id;

    @OneToOne(optional = false)
    @JoinColumn(name = "endpoint_id", nullable = false, unique = true)
    private ApiEndpoint endpoint;

    @Column(nullable = false)
    private boolean required;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    protected ApiRequestBody() {
        // JPA
    }

    public ApiRequestBody(UUID id, boolean required, String content) {
        this.id = id;
        this.required = required;
        this.content = content;
    }

    void assignEndpoint(ApiEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public UUID getId() {
        return id;
    }

    public boolean isRequired() {
        return required;
    }

    public String getContent() {
        return content;
    }
}
