package com.claudereview.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A single finding produced by the code review.
 * Immutable by design (Java record) — once Claude returns a response,
 * findings are not mutated.
 *
 * <p>Field order in JSON output follows {@link JsonPropertyOrder} so
 * consumers get a predictable shape regardless of JVM hash ordering.
 */
@JsonPropertyOrder({
        "id", "severity", "category", "lineNumber",
        "title", "description", "suggestion", "confidence"
})
@JsonInclude(JsonInclude.Include.ALWAYS)
public record Finding(
        String id,
        Severity severity,
        Category category,
        Integer lineNumber, // nullable — Claude may not be able to localize
        String title,
        String description,
        String suggestion,
        Confidence confidence
) {
}