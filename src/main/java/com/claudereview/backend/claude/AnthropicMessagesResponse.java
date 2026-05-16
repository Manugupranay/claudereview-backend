package com.claudereview.backend.claude;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response body from the Anthropic Messages API.
 * Maps the fields we care about; extra fields are ignored.
 *
 * <p>{@link JsonIgnoreProperties} lets Anthropic add new fields without
 * breaking our parser — important for forward-compatibility.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnthropicMessagesResponse(
        String id,
        String model,
        List<ContentBlock> content,
        @JsonProperty("stop_reason") String stopReason,
        Usage usage
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentBlock(String type, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            @JsonProperty("input_tokens") int inputTokens,
            @JsonProperty("output_tokens") int outputTokens
    ) {
    }

    /**
     * Convenience: pull the first text block from the response.
     * Anthropic returns content as an array, but for our use case
     * we expect a single text block containing the JSON review.
     */
    public String firstTextOrThrow() {
        if (content == null || content.isEmpty()) {
            throw new IllegalStateException("Anthropic response had no content blocks");
        }
        return content.stream()
                .filter(b -> "text".equals(b.type()))
                .map(ContentBlock::text)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Anthropic response had no text content block"));
    }
}