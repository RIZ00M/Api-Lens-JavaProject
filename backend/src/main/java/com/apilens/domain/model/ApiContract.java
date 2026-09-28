package com.apilens.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The normalized representation of one parsed OpenAPI/Swagger
 * specification for an API (product spec sections 7-8). Aggregate root:
 * saving an ApiContract cascades to its servers, security schemes,
 * schemas and endpoints (which in turn cascade their own parameters,
 * request body and responses) -- callers only ever save/load through
 * this class, never the child repositories directly.
 *
 * Every successful discovery+parse currently creates a *new* ApiContract
 * row (no dedup). Comparing against the previous one, hashing, and only
 * keeping a new snapshot when something actually changed is Phase 8's
 * job ("Contract Snapshots") -- deliberately not implemented here yet.
 */
@Entity
@Table(name = "api_contracts")
public class ApiContract {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "api_id", nullable = false)
    private Api api;

    @Column(name = "specification_version")
    private String specificationVersion;

    private String title;

    private String description;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    /** The original spec exactly as fetched, kept for later phases (diffing, re-parsing). */
    @Column(name = "raw_specification", nullable = false, columnDefinition = "TEXT")
    private String rawSpecification;

    @Column(name = "discovered_at", nullable = false)
    private Instant discoveredAt;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApiServer> servers = new ArrayList<>();

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApiSecurityScheme> securitySchemes = new ArrayList<>();

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApiSchema> schemas = new ArrayList<>();

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApiEndpoint> endpoints = new ArrayList<>();

    protected ApiContract() {
        // JPA
    }

    public ApiContract(UUID id, Api api, String specificationVersion, String title, String description,
                        String baseUrl, String sourceUrl, String rawSpecification) {
        this.id = id;
        this.api = api;
        this.specificationVersion = specificationVersion;
        this.title = title;
        this.description = description;
        this.baseUrl = baseUrl;
        this.sourceUrl = sourceUrl;
        this.rawSpecification = rawSpecification;
    }

    @PrePersist
    void onCreate() {
        this.discoveredAt = Instant.now();
    }

    public void addServer(ApiServer server) {
        server.assignContract(this);
        servers.add(server);
    }

    public void addSecurityScheme(ApiSecurityScheme scheme) {
        scheme.assignContract(this);
        securitySchemes.add(scheme);
    }

    public void addSchema(ApiSchema schema) {
        schema.assignContract(this);
        schemas.add(schema);
    }

    public void addEndpoint(ApiEndpoint endpoint) {
        endpoint.assignContract(this);
        endpoints.add(endpoint);
    }

    public UUID getId() {
        return id;
    }

    public Api getApi() {
        return api;
    }

    public String getSpecificationVersion() {
        return specificationVersion;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getRawSpecification() {
        return rawSpecification;
    }

    public Instant getDiscoveredAt() {
        return discoveredAt;
    }

    public List<ApiServer> getServers() {
        return servers;
    }

    public List<ApiSecurityScheme> getSecuritySchemes() {
        return securitySchemes;
    }

    public List<ApiSchema> getSchemas() {
        return schemas;
    }

    public List<ApiEndpoint> getEndpoints() {
        return endpoints;
    }
}
