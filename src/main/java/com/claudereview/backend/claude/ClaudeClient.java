package com.claudereview.backend.claude;

import com.claudereview.backend.config.ClaudeConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;

/**
 * Thin HTTP client for the Anthropic Messages API.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Build the request payload from a system prompt + user content.</li>
 *   <li>Apply explicit connect and read timeouts (no infinite hangs).</li>
 *   <li>Translate transport failures and non-2xx responses into typed exceptions.</li>
 *   <li>Log structured metadata (status, latency, token usage) — never request or response bodies.</li>
 * </ul>
 *
 * <p>Deliberately does NOT:
 * <ul>
 *   <li>Retry — that decision belongs in the calling service.</li>
 *   <li>Parse Claude's content as JSON — that's the service's job too.</li>
 *   <li>Know anything about reviews, findings, or the domain — strictly Anthropic plumbing.</li>
 * </ul>
 */
@Component
public class ClaudeClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeClient.class);

    private static final String MESSAGES_ENDPOINT = "/v1/messages";

    private final ClaudeConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ClaudeClient(ClaudeConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.connectTimeoutSeconds()))
                .build();
    }

    /**
     * Send a single user message to Claude with the given system prompt.
     * Returns the raw text content from Claude's first text block.
     *
     * @param systemPrompt the system prompt to send (never logged)
     * @param userContent  the user-role message content (never logged)
     * @return the raw text Claude returned
     * @throws ClaudeApiException for any transport, timeout, or non-2xx response failure
     */
    public ClaudeCallResult call(String systemPrompt, String userContent) {
        AnthropicMessagesRequest payload = new AnthropicMessagesRequest(
                config.model(),
                config.maxTokens(),
                systemPrompt,
                List.of(new AnthropicMessagesRequest.Message("user", userContent))
        );

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ClaudeApiException("Failed to serialize Claude request", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.baseUrl() + MESSAGES_ENDPOINT))
                .timeout(Duration.ofSeconds(config.readTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("x-api-key", config.apiKey())
                .header("anthropic-version", config.version())
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        long start = System.nanoTime();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            long ms = (System.nanoTime() - start) / 1_000_000;
            log.warn("Claude API timeout after {} ms", ms);
            throw new ClaudeApiException("Claude API timed out", e);
        } catch (Exception e) {
            long ms = (System.nanoTime() - start) / 1_000_000;
            log.warn("Claude API transport failure after {} ms: {}", ms, e.getClass().getSimpleName());
            throw new ClaudeApiException("Claude API transport failure", e);
        }

        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        int status = response.statusCode();

        if (status < 200 || status >= 300) {
            // Log status and latency, but not response body (may contain sensitive details).
            log.warn("Claude API returned non-2xx: status={} latencyMs={}", status, latencyMs);
            throw new ClaudeApiException(
                    "Claude API returned HTTP " + status,
                    status
            );
        }

        AnthropicMessagesResponse parsed;
        try {
            parsed = objectMapper.readValue(response.body(), AnthropicMessagesResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("Claude API returned unparseable JSON: latencyMs={}", latencyMs);
            throw new ClaudeApiException("Claude API returned unparseable response", e);
        }

        String text = parsed.firstTextOrThrow();
        log.info("Claude call succeeded: latencyMs={} inputTokens={} outputTokens={}",
                latencyMs,
                parsed.usage() != null ? parsed.usage().inputTokens() : -1,
                parsed.usage() != null ? parsed.usage().outputTokens() : -1);

        return new ClaudeCallResult(
                text,
                parsed.model(),
                latencyMs,
                parsed.usage() != null ? parsed.usage().inputTokens() : 0,
                parsed.usage() != null ? parsed.usage().outputTokens() : 0
        );
    }

    /**
     * Result of a successful Claude call.
     * Bundles the response text with observability metadata so callers can
     * propagate latency and token counts into their own responses.
     */
    public record ClaudeCallResult(
            String text,
            String model,
            long latencyMs,
            int inputTokens,
            int outputTokens
    ) {
    }
}