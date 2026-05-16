package com.claudereview.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration for the REST API.
 *
 * <p>The frontend runs at a different origin than the backend in both
 * development (localhost:5173 → localhost:8080) and production
 * (netlify domain → railway domain). Browsers block cross-origin
 * requests by default, so we explicitly whitelist the frontend's
 * origin for our /api/* paths.
 *
 * <p>Allowed origins are read from {@code claude.cors.allowed-origins}
 * in application.properties — comma-separated. This keeps the dev and
 * prod origins out of source code and easy to change per environment.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${claude.cors.allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = allowedOrigins.split(",");
        for (int i = 0; i < this.allowedOrigins.length; i++) {
            this.allowedOrigins[i] = this.allowedOrigins[i].trim();
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}