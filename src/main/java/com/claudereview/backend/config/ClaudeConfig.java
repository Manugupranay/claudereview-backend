package com.claudereview.backend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;

/**
 * Strongly-typed Claude API configuration.
 */
@ConfigurationProperties(prefix = "claude.api")
public record ClaudeConfig(
        String baseUrl,
        String version,
        String apiKey,
        String model,
        @DefaultValue("2048") int maxTokens,
        @DefaultValue("5") int connectTimeoutSeconds,
        @DefaultValue("60") int readTimeoutSeconds,
        Resource systemPromptResource
) {

    @PostConstruct
    public void validate() {
        System.out.println("=== ClaudeConfig DIAGNOSTIC ===");
        System.out.println("baseUrl: " + baseUrl);
        System.out.println("version: " + version);
        System.out.println("model: " + model);
        System.out.println("apiKey is null: " + (apiKey == null));
        System.out.println("apiKey is blank: " + (apiKey != null && apiKey.isBlank()));
        System.out.println("apiKey length: " + (apiKey == null ? "N/A" : String.valueOf(apiKey.length())));
        System.out.println("apiKey first 8 chars: " + (apiKey == null || apiKey.length() < 8 ? "N/A" : apiKey.substring(0, 8)));
        System.out.println("=== END DIAGNOSTIC ===");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "claude.api.key is missing or empty. Check secrets.properties."
            );
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("claude.api.base-url must be set");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalStateException("claude.api.model must be set");
        }
    }
}