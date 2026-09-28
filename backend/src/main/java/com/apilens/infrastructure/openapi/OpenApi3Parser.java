package com.apilens.infrastructure.openapi;

import com.apilens.domain.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ContractParser backed by swagger-parser (io.swagger.parser.v3). Handles
 * OpenAPI 3.x natively; Swagger 2.0 documents are not yet supported end
 * to end (that needs the swagger-parser-v2-converter module, not yet a
 * dependency of this project -- tracked as a follow-up rather than
 * silently claimed to work, per docs/adr/ADR-005). A 2.0 document will
 * currently just fail to parse and surface as a normal ParseResult
 * failure with the parser's own diagnostic messages.
 *
 * Note: `io.swagger.v3.oas.models.responses.ApiResponse` (the library's
 * per-status-code response type) and our own domain ApiResponse share a
 * name; the library's type is always referenced fully-qualified below to
 * keep that unambiguous.
 */
@Component
public class OpenApi3Parser implements ContractParser {

    private static final Logger log = LoggerFactory.getLogger(OpenApi3Parser.class);

    @Override
    public ParseResult parse(Api api, String sourceUrl, byte[] rawContent) {
        String rawText = new String(rawContent, StandardCharsets.UTF_8);

        ParseOptions options = new ParseOptions();
        options.setResolve(true); // inline internal $ref pointers within the document

        SwaggerParseResult parseResult;
        try {
            parseResult = new OpenAPIV3Parser().readContents(rawText, null, options);
        } catch (RuntimeException e) {
            log.warn("OpenAPI parsing threw for {}: {}", sourceUrl, e.toString());
            return ParseResult.failure(List.of("Parser error: " + e.getMessage()));
        }

        OpenAPI openApi = parseResult == null ? null : parseResult.getOpenAPI();
        List<String> messages = parseResult != null && parseResult.getMessages() != null
                ? parseResult.getMessages() : List.of();

        if (openApi == null) {
            List<String> errors = messages.isEmpty()
                    ? List.of("The document could not be parsed as an OpenAPI or Swagger specification")
                    : messages;
            return ParseResult.failure(errors);
        }

        try {
            ApiContract contract = buildContract(api, sourceUrl, rawText, openApi);
            return ParseResult.success(contract, messages);
        } catch (RuntimeException e) {
            log.warn("Failed to normalize parsed OpenAPI document for {}: {}", sourceUrl, e.toString());
            return ParseResult.failure(List.of("Failed to normalize the parsed specification: " + e.getMessage()));
        }
    }

    private ApiContract buildContract(Api api, String sourceUrl, String rawText, OpenAPI openApi) {
        String title = openApi.getInfo() != null ? openApi.getInfo().getTitle() : null;
        String description = openApi.getInfo() != null ? openApi.getInfo().getDescription() : null;
        String baseUrl = (openApi.getServers() != null && !openApi.getServers().isEmpty())
                ? openApi.getServers().get(0).getUrl() : null;

        ApiContract contract = new ApiContract(UUID.randomUUID(), api, openApi.getOpenapi(), title, description,
                baseUrl, sourceUrl, rawText);

        if (openApi.getServers() != null) {
            for (Server server : openApi.getServers()) {
                contract.addServer(new ApiServer(UUID.randomUUID(), server.getUrl(), server.getDescription()));
            }
        }

        Components components = openApi.getComponents();
        if (components != null && components.getSecuritySchemes() != null) {
            components.getSecuritySchemes().forEach((name, scheme) ->
                    contract.addSecurityScheme(toSecurityScheme(name, scheme)));
        }
        if (components != null && components.getSchemas() != null) {
            components.getSchemas().forEach((name, schema) ->
                    contract.addSchema(new ApiSchema(UUID.randomUUID(), name, toJson(schema))));
        }

        Paths paths = openApi.getPaths();
        if (paths != null) {
            paths.forEach((path, pathItem) -> pathItem.readOperationsMap()
                    .forEach((httpMethod, operation) ->
                            contract.addEndpoint(toEndpoint(path, httpMethod.toString(), pathItem, operation, openApi))));
        }

        return contract;
    }

    private ApiSecurityScheme toSecurityScheme(String name, SecurityScheme scheme) {
        return new ApiSecurityScheme(
                UUID.randomUUID(),
                name,
                scheme.getType() != null ? scheme.getType().toString() : null,
                scheme.getScheme(),
                scheme.getBearerFormat(),
                scheme.getIn() != null ? scheme.getIn().toString() : null,
                scheme.getName(),
                scheme.getDescription()
        );
    }

    private ApiEndpoint toEndpoint(String path, String method, PathItem pathItem, Operation operation, OpenAPI openApi) {
        List<String> tags = operation.getTags() != null ? operation.getTags() : List.of();
        Set<String> requiredSchemes = resolveRequiredSecuritySchemes(operation, openApi);

        ApiEndpoint endpoint = new ApiEndpoint(
                UUID.randomUUID(),
                path,
                method,
                operation.getOperationId(),
                operation.getSummary(),
                operation.getDescription(),
                Boolean.TRUE.equals(operation.getDeprecated()),
                tags,
                requiredSchemes
        );

        List<Parameter> combinedParameters = new ArrayList<>();
        if (pathItem.getParameters() != null) {
            combinedParameters.addAll(pathItem.getParameters());
        }
        if (operation.getParameters() != null) {
            combinedParameters.addAll(operation.getParameters());
        }
        for (Parameter parameter : combinedParameters) {
            endpoint.addParameter(toParameter(parameter));
        }

        RequestBody requestBody = operation.getRequestBody();
        if (requestBody != null) {
            endpoint.setRequestBody(new ApiRequestBody(
                    UUID.randomUUID(),
                    Boolean.TRUE.equals(requestBody.getRequired()),
                    serializeContent(requestBody.getContent())
            ));
        }

        if (operation.getResponses() != null) {
            operation.getResponses().forEach((statusCode, response) ->
                    endpoint.addResponse(new ApiResponse(
                            UUID.randomUUID(),
                            statusCode,
                            response.getDescription(),
                            serializeContent(response.getContent())
                    )));
        }

        return endpoint;
    }

    private ApiParameter toParameter(Parameter parameter) {
        Schema<?> schema = parameter.getSchema();
        String type = schema != null ? schema.getType() : null;
        String format = schema != null ? schema.getFormat() : null;
        String defaultValue = (schema != null && schema.getDefault() != null)
                ? String.valueOf(schema.getDefault()) : null;
        List<String> enumValues = (schema != null && schema.getEnum() != null)
                ? schema.getEnum().stream().map(String::valueOf).toList()
                : List.of();

        return new ApiParameter(
                UUID.randomUUID(),
                parameter.getName(),
                parameter.getIn(),
                Boolean.TRUE.equals(parameter.getRequired()),
                type,
                format,
                parameter.getDescription(),
                defaultValue,
                enumValues
        );
    }

    /**
     * Operation-level `security` overrides the document's global security
     * per the OpenAPI spec (an empty list means "explicitly no auth");
     * only fall back to the global requirement when the operation doesn't
     * declare one at all.
     */
    private Set<String> resolveRequiredSecuritySchemes(Operation operation, OpenAPI openApi) {
        List<SecurityRequirement> requirements = operation.getSecurity() != null
                ? operation.getSecurity() : openApi.getSecurity();
        if (requirements == null) {
            return Set.of();
        }
        return requirements.stream()
                .flatMap(requirement -> requirement.keySet().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String serializeContent(Content content) {
        if (content == null || content.isEmpty()) {
            return "{}";
        }
        Map<String, Schema<?>> schemasByContentType = new LinkedHashMap<>();
        for (Map.Entry<String, MediaType> entry : content.entrySet()) {
            schemasByContentType.put(entry.getKey(), entry.getValue() != null ? entry.getValue().getSchema() : null);
        }
        return toJson(schemasByContentType);
    }

    private String toJson(Object value) {
        try {
            return Json.mapper().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize parsed schema fragment to JSON: {}", e.toString());
            return "{}";
        }
    }
}
