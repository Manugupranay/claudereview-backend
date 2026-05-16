package com.claudereview.backend.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * The full review response sent back to clients.
 * Shape is locked by the API contract in {@code API_CONTRACT.md}.
 */
@JsonPropertyOrder({
        "reviewId", "language", "model", "summary", "findings", "metadata"
})
public record ReviewResponse(
        String reviewId,
        Language language,
        String model,
        String summary,
        List<Finding> findings,
        Metadata metadata
) {
}