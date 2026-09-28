package com.apilens.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test of API CRUD against a real PostgreSQL instance,
 * covering: create, validation failure, fetch, list, delete, and the
 * not-found path after deletion.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void fullCrudLifecycle() {
        Map<String, String> createRequest = Map.of(
                "name", "GitHub API",
                "baseUrl", "https://api.github.com"
        );

        ResponseEntity<Map> createResponse = restTemplate.postForEntity("/api/apis", createRequest, Map.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String id = (String) createResponse.getBody().get("id");
        assertThat(id).isNotBlank();

        ResponseEntity<Map> getResponse = restTemplate.getForEntity("/api/apis/" + id, Map.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().get("name")).isEqualTo("GitHub API");

        ResponseEntity<Map> listResponse = restTemplate.getForEntity("/api/apis", Map.class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Integer) listResponse.getBody().get("totalElements")).isGreaterThanOrEqualTo(1);

        restTemplate.delete("/api/apis/" + id);

        ResponseEntity<Map> afterDelete = restTemplate.getForEntity("/api/apis/" + id, Map.class);
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(afterDelete.getBody().get("error")).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void rejectsInvalidBaseUrl() {
        Map<String, String> invalidRequest = Map.of(
                "name", "Bad API",
                "baseUrl", "not-a-url"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/apis", invalidRequest, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("error")).isEqualTo("VALIDATION_FAILED");
    }
}
