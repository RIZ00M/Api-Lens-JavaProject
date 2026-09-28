package com.apilens.domain.model;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * A named, reusable schema from `components.schemas`. `definition` is the
 * schema's own JSON representation (property names, types, nesting) kept
 * as text rather than decomposed into per-property rows -- see the
 * rationale in Flyway migration V4 and ADR-005. The frontend's Schemas
 * explorer (Phase 6) renders this directly rather than querying into it.
 */
@Entity
@Table(name = "api_schemas")
public class ApiSchema {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private ApiContract contract;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String definition;

    protected ApiSchema() {
        // JPA
    }

    public ApiSchema(UUID id, String name, String definition) {
        this.id = id;
        this.name = name;
        this.definition = definition;
    }

    void assignContract(ApiContract contract) {
        this.contract = contract;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDefinition() {
        return definition;
    }
}
