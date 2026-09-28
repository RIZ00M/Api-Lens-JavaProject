package com.apilens;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the API Lens backend.
 *
 * Phase 1 scope: application boots, connects to PostgreSQL, runs Flyway
 * migrations, and exposes health/status endpoints. Domain functionality
 * (API CRUD, discovery, parsing, analysis) is added in later phases.
 */
@SpringBootApplication
@ConfigurationPropertiesScan("com.apilens")
public class ApiLensApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiLensApplication.class, args);
    }
}
