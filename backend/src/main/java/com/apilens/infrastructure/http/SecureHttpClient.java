package com.apilens.infrastructure.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import io.netty.channel.ChannelOption;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/**
 * The only component in the application permitted to fetch a
 * user-supplied or discovery-derived URL. See docs/security.md for the
 * full threat model; summary of what's enforced here:
 *
 *  - HTTPS only by default (apilens.http.https-only)
 *  - hostnames are resolved and every resolved IP is checked against
 *    {@link IpAddressValidator} before any request is made
 *  - redirects are never auto-followed by the HTTP layer; each redirect
 *    target is re-validated by recursing through this same method, up to
 *    apilens.http.max-redirects
 *  - connect/read timeouts and a maximum response size are enforced
 *  - the response Content-Type is checked against an allow-list
 *  - no headers or credentials from the inbound request are ever forwarded
 *
 * Known limitation (documented, not hidden — see
 * docs/adr/ADR-004-ssrf-protection-strategy.md): the IP validated during
 * the pre-flight DNS lookup is not the literal socket the HTTP client
 * connects to, so a small window exists for DNS-rebinding between
 * validation and connection. This is called out as a explicit follow-up
 * rather than silently left unhandled.
 */
@Component
public class SecureHttpClient {

    private static final Logger log = LoggerFactory.getLogger(SecureHttpClient.class);
    private static final Set<String> HTTPS_ONLY = Set.of("https");
    private static final Set<String> HTTP_AND_HTTPS = Set.of("http", "https");
    private static final String USER_AGENT = "ApiLens-Discovery/0.1 (+https://github.com/api-lens)";

    private final SecureHttpClientProperties properties;
    private final WebClient webClient;
    private final java.util.function.Predicate<InetAddress> isIpBlocked;

    public SecureHttpClient(SecureHttpClientProperties properties) {
        this(properties, IpAddressValidator::isBlocked);
    }

    /**
     * Test-only entry point: lets tests substitute the IP-blocking policy
     * (e.g. to allow 127.0.0.1 so a local mock HTTP server can be used)
     * without weakening {@link #SecureHttpClient(SecureHttpClientProperties)},
     * which always uses the real {@link IpAddressValidator}. Package-private
     * so only tests in this package can reach it.
     */
    SecureHttpClient(SecureHttpClientProperties properties, java.util.function.Predicate<InetAddress> isIpBlocked) {
        this.properties = properties;
        this.isIpBlocked = isIpBlocked;

        HttpClient reactorHttpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.connectTimeoutMs())
                .responseTimeout(Duration.ofMillis(properties.readTimeoutMs()))
                .followRedirect(false); // redirects are handled explicitly in fetch(), never automatically

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(clampToInt(properties.maxResponseSizeBytes())))
                .build();

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(reactorHttpClient))
                .exchangeStrategies(strategies)
                .build();
    }

    public SecureFetchResult fetch(String url) {
        return fetch(url, 0);
    }

    private SecureFetchResult fetch(String url, int redirectCount) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return blockedAndLogged(SecureFetchResult.Outcome.BLOCKED_URL, url, "Malformed URL");
        }

        String scheme = uri.getScheme();
        Set<String> allowedSchemes = properties.httpsOnly() ? HTTPS_ONLY : HTTP_AND_HTTPS;
        if (scheme == null || !allowedSchemes.contains(scheme.toLowerCase(Locale.ROOT))) {
            return blockedAndLogged(SecureFetchResult.Outcome.BLOCKED_URL, url,
                    "Scheme not allowed: " + scheme);
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return blockedAndLogged(SecureFetchResult.Outcome.BLOCKED_URL, url, "URL has no host");
        }

        InetAddress[] resolved;
        try {
            resolved = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            return blockedAndLogged(SecureFetchResult.Outcome.DNS_RESOLUTION_FAILED, url,
                    "Could not resolve host: " + host);
        }

        for (InetAddress address : resolved) {
            if (isIpBlocked.test(address)) {
                return blockedAndLogged(SecureFetchResult.Outcome.BLOCKED_IP, url,
                        "Host " + host + " resolves to a disallowed address (" + address.getHostAddress() + ")");
            }
        }

        try {
            ClientResponse response = webClient.method(HttpMethod.GET)
                    .uri(uri)
                    .header(HttpHeaders.USER_AGENT, USER_AGENT)
                    .header(HttpHeaders.ACCEPT, "application/json, application/yaml, text/yaml, */*;q=0.1")
                    .exchangeToMono(Mono::just)
                    .block(Duration.ofMillis(properties.connectTimeoutMs() + properties.readTimeoutMs()));

            if (response == null) {
                return new SecureFetchResult(SecureFetchResult.Outcome.NETWORK_ERROR, url, null, null, null, 0,
                        redirectCount, "No response received");
            }

            if (response.statusCode().is3xxRedirection()) {
                return followRedirect(url, uri, response, redirectCount);
            }

            String contentType = response.headers().contentType().map(MediaType::toString).orElse(null);

            if (response.statusCode().is2xxSuccessful() && !isContentTypeAllowed(contentType)) {
                response.releaseBody().block();
                return new SecureFetchResult(SecureFetchResult.Outcome.UNSUPPORTED_CONTENT_TYPE, url,
                        response.statusCode().value(), contentType, null, 0, redirectCount,
                        "Content type not allowed: " + contentType);
            }

            byte[] body = response.bodyToMono(byte[].class).block(Duration.ofMillis(properties.readTimeoutMs()));
            body = body == null ? new byte[0] : body;

            SecureFetchResult.Outcome outcome = response.statusCode().is2xxSuccessful()
                    ? SecureFetchResult.Outcome.SUCCESS
                    : SecureFetchResult.Outcome.HTTP_ERROR;

            return new SecureFetchResult(outcome, url, response.statusCode().value(), contentType, body,
                    body.length, redirectCount,
                    outcome == SecureFetchResult.Outcome.HTTP_ERROR
                            ? "Non-success HTTP status: " + response.statusCode().value()
                            : null);

        } catch (org.springframework.core.io.buffer.DataBufferLimitException e) {
            return new SecureFetchResult(SecureFetchResult.Outcome.RESPONSE_TOO_LARGE, url, null, null, null, 0,
                    redirectCount, "Response exceeded max size of " + properties.maxResponseSizeBytes() + " bytes");
        } catch (RuntimeException e) {
            if (isTimeout(e)) {
                return new SecureFetchResult(SecureFetchResult.Outcome.TIMEOUT, url, null, null, null, 0,
                        redirectCount, "Request timed out");
            }
            log.warn("Discovery fetch failed for {}: {}", url, e.toString());
            return new SecureFetchResult(SecureFetchResult.Outcome.NETWORK_ERROR, url, null, null, null, 0,
                    redirectCount, "Network error while fetching URL");
        }
    }

    private SecureFetchResult followRedirect(String originalUrl, URI requestUri, ClientResponse response, int redirectCount) {
        String location = response.headers().header(HttpHeaders.LOCATION).stream().findFirst().orElse(null);
        int status = response.statusCode().value();
        response.releaseBody().block();

        if (location == null) {
            return new SecureFetchResult(SecureFetchResult.Outcome.HTTP_ERROR, originalUrl, status, null, null, 0,
                    redirectCount, "Redirect response had no Location header");
        }
        if (redirectCount >= properties.maxRedirects()) {
            return new SecureFetchResult(SecureFetchResult.Outcome.TOO_MANY_REDIRECTS, originalUrl, status, null,
                    null, 0, redirectCount, "Exceeded max redirects (" + properties.maxRedirects() + ")");
        }

        URI redirectTarget = requestUri.resolve(location);
        // Recurse through fetch() from the top so the redirect target gets the exact same
        // scheme/host/IP validation as any other URL -- this is what "validate every redirect" means.
        return fetch(redirectTarget.toString(), redirectCount + 1);
    }

    private boolean isContentTypeAllowed(String contentType) {
        if (properties.allowedContentTypePrefixes().isEmpty()) {
            return true; // no restriction configured
        }
        if (contentType == null) {
            return false;
        }
        return properties.allowedContentTypePrefixes().stream().anyMatch(contentType::startsWith);
    }

    private boolean isTimeout(Throwable t) {
        Throwable current = t;
        while (current != null) {
            String className = current.getClass().getSimpleName();
            String message = current.getMessage();
            if (className.toLowerCase(Locale.ROOT).contains("timeout")
                    || (message != null && message.toLowerCase(Locale.ROOT).contains("timeout"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private SecureFetchResult blockedAndLogged(SecureFetchResult.Outcome outcome, String url, String reason) {
        log.warn("SSRF_REQUEST_BLOCKED url={} reason={}", url, reason);
        return SecureFetchResult.blocked(outcome, url, reason);
    }

    private static int clampToInt(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE - 1024);
    }
}
