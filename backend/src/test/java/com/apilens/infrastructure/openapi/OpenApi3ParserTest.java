package com.apilens.infrastructure.openapi;

import com.apilens.domain.model.Api;
import com.apilens.domain.model.ApiContract;
import com.apilens.domain.model.ApiEndpoint;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApi3ParserTest {

    private static final String DEMO_SPEC = """
            {
              "openapi": "3.0.3",
              "info": { "title": "Demo Store API", "description": "A tiny demo", "version": "1.0.0" },
              "servers": [ { "url": "https://demo-api.example.com", "description": "Production" } ],
              "components": {
                "securitySchemes": {
                  "bearerAuth": { "type": "http", "scheme": "bearer", "bearerFormat": "JWT" }
                },
                "schemas": {
                  "Product": {
                    "type": "object",
                    "properties": {
                      "id": { "type": "integer" },
                      "name": { "type": "string" }
                    }
                  }
                }
              },
              "paths": {
                "/products": {
                  "get": {
                    "operationId": "listProducts",
                    "summary": "List products",
                    "tags": ["Products"],
                    "responses": {
                      "200": {
                        "description": "OK",
                        "content": { "application/json": { "schema": { "type": "array", "items": { "$ref": "#/components/schemas/Product" } } } }
                      }
                    }
                  },
                  "post": {
                    "operationId": "createProduct",
                    "security": [ { "bearerAuth": [] } ],
                    "requestBody": {
                      "required": true,
                      "content": { "application/json": { "schema": { "$ref": "#/components/schemas/Product" } } }
                    },
                    "responses": {
                      "201": { "description": "Created" }
                    }
                  }
                },
                "/products/{id}": {
                  "get": {
                    "operationId": "getProduct",
                    "deprecated": true,
                    "parameters": [
                      { "name": "id", "in": "path", "required": true, "schema": { "type": "integer" } },
                      { "name": "includeDetails", "in": "query", "required": false, "schema": { "type": "boolean", "default": false } }
                    ],
                    "responses": {
                      "200": { "description": "OK" },
                      "404": { "description": "Not found" }
                    }
                  }
                }
              }
            }
            """;

    private final OpenApi3Parser parser = new OpenApi3Parser();

    private Api demoApi() {
        // owner is never read by the parser; null keeps this test independent of User's
        // JPA-only (no public constructor) design.
        return new Api(UUID.randomUUID(), null, "Demo Store API", "https://demo-api.example.com", null, null);
    }

    @Test
    void parsesEndpointsParametersSchemasAndSecurity() {
        ContractParser.ParseResult result = parser.parse(
                demoApi(), "https://demo-api.example.com/openapi.json", DEMO_SPEC.getBytes(StandardCharsets.UTF_8));

        assertThat(result.success()).isTrue();
        ApiContract contract = result.contract();
        assertThat(contract.getSpecificationVersion()).isEqualTo("3.0.3");
        assertThat(contract.getTitle()).isEqualTo("Demo Store API");
        assertThat(contract.getBaseUrl()).isEqualTo("https://demo-api.example.com");
        assertThat(contract.getServers()).hasSize(1);
        assertThat(contract.getSecuritySchemes()).hasSize(1);
        assertThat(contract.getSchemas()).extracting("name").containsExactly("Product");
        assertThat(contract.getEndpoints()).hasSize(3);

        ApiEndpoint getById = findEndpoint(contract, "GET", "/products/{id}");
        assertThat(getById.isDeprecated()).isTrue();
        assertThat(getById.getParameters()).hasSize(2);
        assertThat(getById.getParameters().get(0).getName()).isEqualTo("id");
        assertThat(getById.getParameters().get(0).isRequired()).isTrue();
        assertThat(getById.getResponses()).hasSize(2);

        ApiEndpoint createProduct = findEndpoint(contract, "POST", "/products");
        assertThat(createProduct.getRequestBody()).isNotNull();
        assertThat(createProduct.getRequestBody().isRequired()).isTrue();
        assertThat(createProduct.getRequiredSecuritySchemeNames()).containsExactly("bearerAuth");

        ApiEndpoint listProducts = findEndpoint(contract, "GET", "/products");
        assertThat(listProducts.getTags()).containsExactly("Products");
        assertThat(listProducts.getRequiredSecuritySchemeNames()).isEmpty();
    }

    @Test
    void reportsFailureForGarbageInput() {
        ContractParser.ParseResult result = parser.parse(
                demoApi(), "https://example.com/nope", "not a spec at all".getBytes(StandardCharsets.UTF_8));

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).isNotEmpty();
    }

    private ApiEndpoint findEndpoint(ApiContract contract, String method, String path) {
        Optional<ApiEndpoint> found = contract.getEndpoints().stream()
                .filter(e -> e.getMethod().equalsIgnoreCase(method) && e.getPath().equals(path))
                .findFirst();
        assertThat(found).as("endpoint %s %s", method, path).isPresent();
        return found.get();
    }
}
