package com.apilens.infrastructure.openapi;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight check of whether a fetched document looks like an
 * OpenAPI/Swagger specification, and if so which version -- just enough
 * to report "Detected: OpenAPI 3.0.3" during discovery (product spec
 * section 5). This is NOT a parser: it does a cheap textual scan for the
 * `openapi:`/`swagger:` version field in both JSON and YAML documents.
 * Full, structural parsing into the normalized contract model is
 * ContractParser's job (Phase 5) -- this class only decides "does this
 * response deserve to be treated as a found specification at all".
 */
public final class SpecificationSniffer {

    private static final Pattern OPENAPI_JSON = Pattern.compile("\"openapi\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SWAGGER_JSON = Pattern.compile("\"swagger\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern OPENAPI_YAML = Pattern.compile("(?m)^openapi:\\s*['\"]?([\\w.]+)['\"]?");
    private static final Pattern SWAGGER_YAML = Pattern.compile("(?m)^swagger:\\s*['\"]?([\\w.]+)['\"]?");

    private SpecificationSniffer() {
    }

    /** @return e.g. "OpenAPI 3.0.3" or "Swagger 2.0", or empty if the text doesn't look like a spec. */
    public static Optional<String> sniff(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        Optional<String> openApi = firstMatch(OPENAPI_JSON, text).or(() -> firstMatch(OPENAPI_YAML, text));
        if (openApi.isPresent()) {
            return openApi.map(version -> "OpenAPI " + version);
        }
        return firstMatch(SWAGGER_JSON, text).or(() -> firstMatch(SWAGGER_YAML, text))
                .map(version -> "Swagger " + version);
    }

    private static Optional<String> firstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
