package com.apilens.domain.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** One (path, method) operation from the specification (product spec section 8). */
@Entity
@Table(name = "api_endpoints")
public class ApiEndpoint {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private ApiContract contract;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private String method;

    @Column(name = "operation_id")
    private String operationId;

    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean deprecated;

    @ElementCollection
    @CollectionTable(name = "api_endpoint_tags", joinColumns = @JoinColumn(name = "endpoint_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    /** Names of ApiSecurityScheme entries (on the same contract) this operation requires, if any. */
    @ElementCollection
    @CollectionTable(name = "api_endpoint_security_schemes", joinColumns = @JoinColumn(name = "endpoint_id"))
    @Column(name = "security_scheme_name")
    private Set<String> requiredSecuritySchemeNames = new HashSet<>();

    @OneToMany(mappedBy = "endpoint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApiParameter> parameters = new ArrayList<>();

    @OneToOne(mappedBy = "endpoint", cascade = CascadeType.ALL, orphanRemoval = true)
    private ApiRequestBody requestBody;

    @OneToMany(mappedBy = "endpoint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApiResponse> responses = new ArrayList<>();

    protected ApiEndpoint() {
        // JPA
    }

    public ApiEndpoint(UUID id, String path, String method, String operationId, String summary,
                        String description, boolean deprecated, List<String> tags,
                        Set<String> requiredSecuritySchemeNames) {
        this.id = id;
        this.path = path;
        this.method = method;
        this.operationId = operationId;
        this.summary = summary;
        this.description = description;
        this.deprecated = deprecated;
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
        this.requiredSecuritySchemeNames = requiredSecuritySchemeNames == null
                ? new HashSet<>() : new HashSet<>(requiredSecuritySchemeNames);
    }

    void assignContract(ApiContract contract) {
        this.contract = contract;
    }

    public void addParameter(ApiParameter parameter) {
        parameter.assignEndpoint(this);
        parameters.add(parameter);
    }

    public void setRequestBody(ApiRequestBody requestBody) {
        if (requestBody != null) {
            requestBody.assignEndpoint(this);
        }
        this.requestBody = requestBody;
    }

    public void addResponse(ApiResponse response) {
        response.assignEndpoint(this);
        responses.add(response);
    }

    public UUID getId() {
        return id;
    }

    public ApiContract getContract() {
        return contract;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public List<String> getTags() {
        return tags;
    }

    public Set<String> getRequiredSecuritySchemeNames() {
        return requiredSecuritySchemeNames;
    }

    public List<ApiParameter> getParameters() {
        return parameters;
    }

    public ApiRequestBody getRequestBody() {
        return requestBody;
    }

    public List<ApiResponse> getResponses() {
        return responses;
    }
}
