package com.apilens.domain.model;

import jakarta.persistence.*;

import java.util.UUID;

/** One entry from the OpenAPI document's top-level `servers` list. */
@Entity
@Table(name = "api_servers")
public class ApiServer {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private ApiContract contract;

    @Column(nullable = false)
    private String url;

    private String description;

    protected ApiServer() {
        // JPA
    }

    public ApiServer(UUID id, String url, String description) {
        this.id = id;
        this.url = url;
        this.description = description;
    }

    void assignContract(ApiContract contract) {
        this.contract = contract;
    }

    public UUID getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }
}
