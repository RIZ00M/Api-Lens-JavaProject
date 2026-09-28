package com.apilens.domain.model;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * One entry from `components.securitySchemes` (product spec section 8,
 * ApiSecurityRequirement). Named "scheme" here (matching the OpenAPI term)
 * rather than "requirement" -- individual endpoints reference these by
 * name via ApiEndpoint.requiredSecuritySchemeNames rather than duplicating
 * the scheme's definition per endpoint.
 */
@Entity
@Table(name = "api_security_schemes")
public class ApiSecurityScheme {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private ApiContract contract;

    @Column(nullable = false)
    private String name;

    private String type;
    private String scheme;

    @Column(name = "bearer_format")
    private String bearerFormat;

    @Column(name = "in_location")
    private String inLocation;

    @Column(name = "key_name")
    private String keyName;

    private String description;

    protected ApiSecurityScheme() {
        // JPA
    }

    public ApiSecurityScheme(UUID id, String name, String type, String scheme, String bearerFormat,
                              String inLocation, String keyName, String description) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.scheme = scheme;
        this.bearerFormat = bearerFormat;
        this.inLocation = inLocation;
        this.keyName = keyName;
        this.description = description;
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

    public String getType() {
        return type;
    }

    public String getScheme() {
        return scheme;
    }

    public String getBearerFormat() {
        return bearerFormat;
    }

    public String getInLocation() {
        return inLocation;
    }

    public String getKeyName() {
        return keyName;
    }

    public String getDescription() {
        return description;
    }
}
