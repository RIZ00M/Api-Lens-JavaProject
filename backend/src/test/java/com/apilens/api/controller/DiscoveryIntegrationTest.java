package com.apilens.api.controller;

import com.apilens.infrastructure.http.TestSecureHttpClientConfig;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end: register an API pointing at a local test server, run
 * discovery through the real REST API, and confirm both the discovery
 * result and the persisted attempt history are correct. Uses
 * TestSecureHttpClientConfig to allow the loopback test server through
 * SecureHttpClient's IP check -- production SecureHttpClient still blocks
 * loopback (see SecureHttpClientTest.blocksLoopbackAddressesByDefault).
 */
@Testcontainers
@Import(TestSecureHttpClientConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiscoveryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String startDemoApiServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/openapi.json", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.createContext("/swagger.json", exchange -> {
            byte[] body = "{\"swagger\":\"2.0\",\"info\":{\"title\":\"Demo\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static final String OPENAPI_3_FIXTURE = """
            {
              "openapi": "3.0.3",
              "info": { "title": "Demo Store API", "version": "1.0.0" },
              "paths": {
                "/products": {
                  "get": {
                    "operationId": "listProducts",
                    "responses": { "200": { "description": "OK" } }
                  }
                }
              }
            }
            """;

    private String startOpenApi3DemoServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/openapi.json", exchange -> {
            byte[] body = OPENAPI_3_FIXTURE.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void discoversAndPersistsAttemptsForAnApiServingSwaggerAtAStandardPath() throws IOException {
        String baseUrl = startDemoApiServer();

        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
                "/api/apis", Map.of("name", "Demo API", "baseUrl", baseUrl), Map.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String apiId = (String) createResponse.getBody().get("id");

        ResponseEntity<Map> discoverResponse = restTemplate.postForEntity(
                "/api/apis/" + apiId + "/discover", null, Map.class);

        assertThat(discoverResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> body = discoverResponse.getBody();
        assertThat(body.get("specificationFound")).isEqualTo(true);
        assertThat(body.get("specificationFormat")).isEqualTo("Swagger 2.0");
        assertThat(body.get("sourceUrl")).isEqualTo(baseUrl + "/swagger.json");
        assertThat((List<?>) body.get("attempts")).hasSize(2); // openapi.json (404) then swagger.json (success)

        // Swagger 2.0 is *found* (the sniffer recognizes it) but not yet *parsed* --
        // see OpenApi3Parser's javadoc on why 2.0 support isn't wired up end to end yet.
        assertThat(body.get("contractParsed")).isEqualTo(false);
        assertThat(body.get("contractId")).isNull();

        ResponseEntity<List> attemptsResponse = restTemplate.getForEntity(
                "/api/apis/" + apiId + "/discovery-attempts", List.class);
        assertThat(attemptsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(attemptsResponse.getBody()).hasSize(2);
    }

    @Test
    void discoversParsesAndPersistsAContractForAnOpenApi3Document() throws IOException {
        String baseUrl = startOpenApi3DemoServer();

        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
                "/api/apis", Map.of("name", "Demo Store API", "baseUrl", baseUrl), Map.class);
        String apiId = (String) createResponse.getBody().get("id");

        ResponseEntity<Map> discoverResponse = restTemplate.postForEntity(
                "/api/apis/" + apiId + "/discover", null, Map.class);

        assertThat(discoverResponse.getBody().get("specificationFound")).isEqualTo(true);
        assertThat(discoverResponse.getBody().get("contractParsed")).isEqualTo(true);
        String contractId = (String) discoverResponse.getBody().get("contractId");
        assertThat(contractId).isNotBlank();

        ResponseEntity<List> contractsForApi = restTemplate.getForEntity(
                "/api/apis/" + apiId + "/contracts", List.class);
        assertThat(contractsForApi.getBody()).hasSize(1);

        ResponseEntity<Map> contractDetail = restTemplate.getForEntity(
                "/api/contracts/" + contractId, Map.class);
        assertThat(contractDetail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(contractDetail.getBody().get("title")).isEqualTo("Demo Store API");
        List<?> endpoints = (List<?>) contractDetail.getBody().get("endpoints");
        assertThat(endpoints).hasSize(1);
    }
}
