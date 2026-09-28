package com.apilens.infrastructure.http;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Test-only Spring configuration overriding the production SecureHttpClient
 * bean with one that allows loopback addresses, so integration tests can
 * exercise real discovery HTTP calls against a local test server. Lives in
 * the same package as SecureHttpClient specifically to reach its
 * package-private, IP-policy-injecting constructor -- production code
 * never has access to this, only tests that @Import this class.
 */
@TestConfiguration
public class TestSecureHttpClientConfig {

    @Bean
    @Primary
    public SecureHttpClient secureHttpClient() {
        SecureHttpClientProperties properties = new SecureHttpClientProperties(
                false,                 // allow http:// so the local test server doesn't need TLS
                2 * 1024 * 1024,
                3,
                2000,
                2000,
                List.of()              // no content-type restriction; discovery itself checks via SpecificationSniffer
        );
        return new SecureHttpClient(properties, address -> false); // never block -- test server runs on loopback
    }
}
