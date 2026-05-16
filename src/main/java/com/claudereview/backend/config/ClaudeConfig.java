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