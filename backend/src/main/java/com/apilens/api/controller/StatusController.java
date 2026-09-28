package com.apilens.api.controller;

import com.apilens.api.dto.StatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Thin controller exposing basic application status.
 *
 * Controllers stay thin per the project's architecture rules: there is no
 * business logic here, only translation from a trivial internal fact
 * ("the app is up") into an HTTP response.
 */
@RestController
public class StatusController {

    private static final String VERSION = "0.1.0-SNAPSHOT";

    @GetMapping("/api/status")
    public StatusResponse status() {
        return new StatusResponse("API Lens", "UP", VERSION, Instant.now());
    }
}
