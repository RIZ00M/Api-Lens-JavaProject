package com.apilens.domain.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One parameter (path, query, header, or cookie) on an endpoint. */
@Entity
@Table(name = "api_parameters")
public class ApiParameter {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private ApiEndpoint endpoint;

    @Column(nullable = false)
    private String name;

    /** "path" | "query" | "header" | "cookie" -- OpenAPI's own parameter `in` values. */
    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private boolean required;

    private String type;
    private String format;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "default_value")
    private String defaultValue;

    @ElementCollection
    @CollectionTable(name = "api_parameter_enum_values", joinColumns = @JoinColumn(name = "parameter_id"))
    @Column(name = "value")
    private List<String> enumValues = new ArrayList<>();

    protected ApiParameter() {
        // JPA
    }

    public ApiParameter(UUID id, String name, String location, boolean required, String type, String format,
                         String description, String defaultValue, List<String> enumValues) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.required = required;
        this.type = type;
        this.format = format;
        this.description = description;
        this.defaultValue = defaultValue;
        this.enumValues = enumValues == null ? new ArrayList<>() : new ArrayList<>(enumValues);
    }

    void assignEndpoint(ApiEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public boolean isRequired() {
        return required;
    }

    public String getType() {
        return type;
    }

    public String getFormat() {
        return format;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public List<String> getEnumValues() {
        return enumValues;
    }
}
