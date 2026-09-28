package com.apilens.infrastructure.http;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the HTTP mechanics (success, error status, redirects, size
 * limits, content-type filtering, timeouts) against a real local server,
 * and the SSRF blocking behaviour against the default, production policy.
 *
 * The local server is deliberately reached using the test-only constructor
 * that allows loopback addresses -- see SecureHttpClient's package-private
 * constructor javadoc. Production code always uses the public constructor,
 * which always blocks loopback (proven by blockedIpTests below).
 */
class SecureHttpClientTest {

    private static final Predicate<InetAddress> ALLOW_ALL = address -> false;

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private HttpServer startServer() throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.start();
        this.server = s;
        return s;
    }

    private SecureHttpClientProperties defaultTestProperties() {
        return new SecureHttpClientProperties(
                false,              // httpsOnly=false so the test server can use plain HTTP
                2 * 1024 * 1024,
                3,
                1000,
                1000,
                List.of("application/json")
        );
    }

    @Test
    void fetchesSuccessfulJsonResponse() throws IOException {
        HttpServer s = startServer();
        s.createContext("/openapi.json", exchange -> {
            byte[] body = "{\"openapi\":\"3.0.3\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        SecureHttpClient client = new SecureHttpClient(defaultTestProperties(), ALLOW_ALL);
        SecureFetchResult result = client.fetch(baseUrl(s) + "/openapi.json");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.bodyAsString()).contains("3.0.3");
        assertThat(result.redirectCount()).isZero();
    }

    @Test
    void reportsHttpErrorStatusWithoutThrowing() throws IOException {
        HttpServer s = startServer();
        s.createContext("/missing", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });

        SecureHttpClient client = new SecureHttpClient(defaultTestProperties(), ALLOW_ALL);
        SecureFetchResult result = client.fetch(baseUrl(s) + "/missing");

        assertThat(result.outcome()).isEqualTo(SecureFetchResult.Outcome.HTTP_ERROR);
        assertThat(result.statusCode()).isEqualTo(404);
    }

    @Test
    void followsRedirectsAndRevalidatesTheTarget() throws IOException {
        HttpServer s = startServer();
        s.createContext("/start", exchange -> {
            exchange.getResponseHeaders().set("Location", "/final");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        s.createContext("/final", exchange -> {
            byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        SecureHttpClient client = new SecureHttpClient(defaultTestProperties(), ALLOW_ALL);
        SecureFetchResult result = client.fetch(baseUrl(s) + "/start");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.redirectCount()).isEqualTo(1);
        assertThat(result.bodyAsString()).contains("ok");
    }

    @Test
    void stopsFollowingAfterMaxRedirects() throws IOException {
        HttpServer s = startServer();
        s.createContext("/loop", exchange -> {
            exchange.getResponseHeaders().set("Location", "/loop");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        SecureHttpClientProperties props = new SecureHttpClientProperties(
                false, 2 * 1024 * 1024, 2, 1000, 1000, List.of());
        SecureHttpClient client = new SecureHttpClient(props, ALLOW_ALL);
        SecureFetchResult result = client.fetch(baseUrl(s) + "/loop");

        assertThat(result.outcome()).isEqualTo(SecureFetchResult.Outcome.TOO_MANY_REDIRECTS);
    }

    @Test
    void rejectsResponsesLargerThanTheConfiguredLimit() throws IOException {
        HttpServer s = startServer();
        s.createContext("/big", exchange -> {
            byte[] body = new byte[50_000];
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        SecureHttpClientProperties props = new SecureHttpClientProperties(
                false, 1024, 3, 1000, 1000, List.of()); // 1 KiB limit
        SecureHttpClient client = new SecureHttpClient(props, ALLOW_ALL);
        SecureFetchResult result = client.fetch(baseUrl(s) + "/big");

        assertThat(result.outcome()).isEqualTo(SecureFetchResult.Outcome.RESPONSE_TOO_LARGE);
    }

    @Test
    void rejectsDisallowedContentType() throws IOException {
        HttpServer s = startServer();
        s.createContext("/page", exchange -> {
            byte[] body = "<html></html>".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        SecureHttpClient client = new SecureHttpClient(defaultTestProperties(), ALLOW_ALL); // only application/json allowed
        SecureFetchResult result = client.fetch(baseUrl(s) + "/page");

        assertThat(result.outcome()).isEqualTo(SecureFetchResult.Outcome.UNSUPPORTED_CONTENT_TYPE);
    }

    @Test
    void timesOutOnASlowServer() throws IOException {
        HttpServer s = startServer();
        s.createContext("/slow", exchange -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });

        SecureHttpClientProperties props = new SecureHttpClientProperties(
                false, 2 * 1024 * 1024, 3, 200, 200, List.of()); // 200ms timeouts
        SecureHttpClient client = new SecureHttpClient(props, ALLOW_ALL);
        SecureFetchResult result = client.fetch(baseUrl(s) + "/slow");

        assertThat(result.outcome()).isEqualTo(SecureFetchResult.Outcome.TIMEOUT);
    }

    @Test
    void blocksLoopbackAddressesByDefault() {
        // Uses the real, public constructor -- the production IP-blocking policy applies.
        SecureHttpClient client = new SecureHttpClient(defaultTestProperties());
        SecureFetchResult result = client.fetch("http://127.0.0.1:1/anything");

        assertThat(result.outcome()).isEqualTo(SecureFetchResult.Outcome.BLOCKED_IP);
    }

    @Test
    void blocksNonHttpsSchemesWhenHttpsOnlyIsEnabled() {
        SecureHttpClientProperties httpsOnly = new SecureHttpClientProperties(
                true, 2 * 1024 * 1024, 3, 1000, 1000, List.of());
        SecureHttpClient client = new SecureHttpClient(httpsOnly, ALLOW_ALL);
        SecureFetchResult result = client.fetch("http://example.com/openapi.json");

        assertThat(result.outcome()).isEqualTo(SecureFetchResult.Outcome.BLOCKED_URL);
    }

    private String baseUrl(HttpServer s) {
        return "http://127.0.0.1:" + s.getAddress().getPort();
    }
}
