package com.apilens.api.dto;

import com.apilens.domain.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full nested view of one parsed contract -- servers, security schemes,
 * schemas, and every endpoint with its parameters/request body/responses
 * inlined. A per-endpoint and per-schema fetch-by-id API (GET
 * /api/endpoints/{id}, etc.) is added in Phase 6 alongside the API
 * explorer UI, once there's a real need to fetch just one at a time
 * rather than the whole contract.
 */
public record ContractDetail(
        UUID id,
        String specificationVersion,
        String title,
        String description,
        String baseUrl,
        String sourceUrl,
        Instant discoveredAt,
        List<ServerView> servers,
        List<SecuritySchemeView> securitySchemes,
        List<SchemaView> schemas,
        List<EndpointView> endpoints
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ContractDetail from(ApiContract contract) {
        return new ContractDetail(
                contract.getId(),
                contract.getSpecificationVersion(),
                contract.getTitle(),
                contract.getDescription(),
                contract.getBaseUrl(),
                contract.getSourceUrl(),
                contract.getDiscoveredAt(),
                contract.getServers().stream().map(ServerView::from).toList(),
                contract.getSecuritySchemes().stream().map(SecuritySchemeView::from).toList(),
                contract.getSchemas().stream().map(SchemaView::from).toList(),
                contract.getEndpoints().stream().map(EndpointView::from).toList()
        );
    }

    static JsonNode parseJson(String text) {
        if (text == null || text.isBlank()) {
            return NullNode.getInstance();
        }
        try {
            return MAPPER.readTree(text);
        } catch (Exception e) {
            return NullNode.getInstance();
        }
    }

    public record ServerView(String url, String description) {
        static ServerView from(ApiServer server) {
            return new ServerView(server.getUrl(), server.getDescription());
        }
    }

    public record SecuritySchemeView(String name, String type, String scheme, String bearerFormat,
                                      String inLocation, String keyName, String description) {
        static SecuritySchemeView from(ApiSecurityScheme scheme) {
            return new SecuritySchemeView(scheme.getName(), scheme.getType(), scheme.getScheme(),
                    scheme.getBearerFormat(), scheme.getInLocation(), scheme.getKeyName(), scheme.getDescription());
        }
    }

    public record SchemaView(String name, JsonNode definition) {
        static SchemaView from(ApiSchema schema) {
            return new SchemaView(schema.getName(), parseJson(schema.getDefinition()));
        }
    }

    public record ParameterView(String name, String location, boolean required, String type, String format,
                                 String description, String defaultValue, List<String> enumValues) {
        static ParameterView from(ApiParameter parameter) {
            return new ParameterView(parameter.getName(), parameter.getLocation(), parameter.isRequired(),
                    parameter.getType(), parameter.getFormat(), parameter.getDescription(),
                    parameter.getDefaultValue(), parameter.getEnumValues());
        }
    }

    public record RequestBodyView(boolean required, JsonNode content) {
        static RequestBodyView from(ApiRequestBody requestBody) {
            return new RequestBodyView(requestBody.isRequired(), parseJson(requestBody.getContent()));
        }
    }

    public record ResponseView(String statusCode, String description, JsonNode content) {
        static ResponseView from(ApiResponse response) {
            return new ResponseView(response.getStatusCode(), response.getDescription(), parseJson(response.getContent()));
        }
    }

    public record EndpointView(UUID id, String path, String method, String operationId, String summary,
                                String description, boolean deprecated, List<String> tags,
                                List<String> requiredSecuritySchemeNames, List<ParameterView> parameters,
                                RequestBodyView requestBody, List<ResponseView> responses) {
        static EndpointView from(ApiEndpoint endpoint) {
            return new EndpointView(
                    endpoint.getId(), endpoint.getPath(), endpoint.getMethod(), endpoint.getOperationId(),
                    endpoint.getSummary(), endpoint.getDescription(), endpoint.isDeprecated(),
                    endpoint.getTags(), List.copyOf(endpoint.getRequiredSecuritySchemeNames()),
                    endpoint.getParameters().stream().map(ParameterView::from).toList(),
                    endpoint.getRequestBody() != null ? RequestBodyView.from(endpoint.getRequestBody()) : null,
                    endpoint.getResponses().stream().map(ResponseView::from).toList()
            );
        }
    }
}
