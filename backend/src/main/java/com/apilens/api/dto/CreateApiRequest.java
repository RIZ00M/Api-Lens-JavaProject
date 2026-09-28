package com.apilens.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for registering a new API.
 *
 * URL fields only get a well-formedness check here (must be an absolute
 * http/https URL). They are treated as untrusted input end-to-end; the
 * stronger SSRF-aware validation happens in SecureHttpClient (Phase 3)
 * at the point they are actually fetched, not here.
 */
public record CreateApiRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = 255, message = "must be 255 characters or fewer")
        String name,

        @NotBlank(message = "must not be blank")
        @Pattern(regexp = "^https?://.+", message = "must be an absolute http(s) URL")
        @Size(max = 2048)
        String baseUrl,

        @Pattern(regexp = "^https?://.+", message = "must be an absolute http(s) URL")
        @Size(max = 2048)
        String openApiUrl,

        @Pattern(regexp = "^https?://.+", message = "must be an absolute http(s) URL")
        @Size(max = 2048)
        String documentationUrl
) {
}
