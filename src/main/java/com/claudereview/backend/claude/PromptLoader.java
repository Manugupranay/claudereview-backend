package com.claudereview.backend.claude;

import com.claudereview.backend.config.ClaudeConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Loads the Claude system prompt from a classpath resource at startup
 * and exposes it as an in-memory string.
 *
 * <p>Loading once at startup (rather than on every request) avoids
 * repeated file I/O on the request path. The prompt is treated as
 * immutable for the life of the JVM — to change it, redeploy.
 *
 * <p>Failure to load the prompt is fatal: the service refuses to start.
 * Operating without a system prompt would let Claude run unconstrained,
 * which violates the production-readiness contract.
 */
@Component
public class PromptLoader {

    private static final Logger log = LoggerFactory.getLogger(PromptLoader.class);

    private final ClaudeConfig config;
    private String systemPrompt;

    public PromptLoader(ClaudeConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void load() {
        try (var stream = config.systemPromptResource().getInputStream()) {
            this.systemPrompt = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            log.info("Loaded system prompt: source={} chars={}",
                    config.systemPromptResource().getDescription(),
                    systemPrompt.length());
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load Claude system prompt from "
                            + config.systemPromptResource().getDescription(),
                    e);
        }
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }
}