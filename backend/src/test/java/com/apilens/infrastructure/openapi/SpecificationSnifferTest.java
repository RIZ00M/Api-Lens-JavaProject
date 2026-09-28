package com.apilens.infrastructure.openapi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpecificationSnifferTest {

    @Test
    void detectsOpenApiJson() {
        String body = "{\"openapi\":\"3.0.3\",\"info\":{\"title\":\"Demo\"}}";
        assertThat(SpecificationSniffer.sniff(body)).contains("OpenAPI 3.0.3");
    }

    @Test
    void detectsOpenApiYaml() {
        String body = "openapi: 3.1.0\ninfo:\n  title: Demo\n";
        assertThat(SpecificationSniffer.sniff(body)).contains("OpenAPI 3.1.0");
    }

    @Test
    void detectsSwaggerJson() {
        String body = "{\"swagger\":\"2.0\",\"info\":{\"title\":\"Demo\"}}";
        assertThat(SpecificationSniffer.sniff(body)).contains("Swagger 2.0");
    }

    @Test
    void detectsSwaggerYaml() {
        String body = "swagger: '2.0'\ninfo:\n  title: Demo\n";
        assertThat(SpecificationSniffer.sniff(body)).contains("Swagger 2.0");
    }

    @Test
    void returnsEmptyForUnrelatedJson() {
        String body = "{\"hello\":\"world\"}";
        assertThat(SpecificationSniffer.sniff(body)).isEmpty();
    }

    @Test
    void returnsEmptyForBlankOrNull() {
        assertThat(SpecificationSniffer.sniff(null)).isEmpty();
        assertThat(SpecificationSniffer.sniff("   ")).isEmpty();
    }
}
