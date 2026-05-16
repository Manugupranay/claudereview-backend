package com.claudereview.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Main entry point for the ClaudeReview backend.
 *
 * <p>{@link ConfigurationPropertiesScan} enables auto-discovery of
 * {@code @ConfigurationProperties} records across the application,
 * so we don't have to register each config class individually.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}