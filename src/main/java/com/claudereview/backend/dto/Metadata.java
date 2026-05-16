package com.claudereview.backend.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Observability metadata returned with every review.
 * Lets clients see how long the review took and how many tokens
 * Claude used — useful for cost monitoring and debugging slow calls.
 */
@JsonPropertyOrder({
        "codeLengthChars", "latencyMs", "promptTokens", "completionTokens"
})
public record Metadata(
        int codeLengthChars,
        long latencyMs,
        int promptTokens,
        int completionTokens
) {
}