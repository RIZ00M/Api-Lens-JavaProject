package com.apilens.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cross-origin configuration for local development, where the React app
 * (Vite dev server) runs on a different origin than the backend.
 *
 * Allowed origins are read from configuration rather than hard-coded, so
 * production deployments can restrict this via the CORS_ALLOWED_ORIGINS
 * environment variable instead of editing code.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${apilens.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*");
    }
}
