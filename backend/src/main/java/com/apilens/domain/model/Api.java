package com.apilens.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;
import java.util.UUID;

/**
 * An API registered with API Lens: a name, a base URL, and optional hints
 * (an explicit OpenAPI URL, a documentation URL) that later phases'
 * discovery engine will try before falling back to standard spec
 * locations (see docs/architecture.md, "Discovery flow").
 *
 * This entity intentionally holds no contract data itself — contracts,
 * endpoints, schemas etc. are separate tables introduced in Phase 5,
 * linked back to an Api by id.
 */
@Entity
@Table(name = "apis")
public class Api {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String name;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(name = "open_api_url")
    private String openApiUrl;

    @Column(name = "documentation_url")
    private String documentationUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Api() {
        // JPA
    }

    public Api(UUID id, User owner, String name, String baseUrl, String openApiUrl, String documentationUrl) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.baseUrl = baseUrl;
        this.openApiUrl = openApiUrl;
        this.documentationUrl = documentationUrl;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getOpenApiUrl() {
        return openApiUrl;
    }

    public String getDocumentationUrl() {
        return documentationUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
