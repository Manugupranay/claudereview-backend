package com.claudereview.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Incoming review request body.
 * Validation annotations are enforced by Spring's @Valid in the controller,
 * so invalid requests are rejected at the boundary before any business logic runs.
 *
 * <p>The {@code language} field is an enum, which Jackson deserializes only
 * if the input string matches a defined value — additional protection against
 * arbitrary string injection.
 */
public record ReviewRequest(

        @NotNull(message = "language is required")
        Language language,

        @NotNull(message = "code is required")
        @Size(min = 1, max = 50_000, message = "code must be between 1 and 50,000 characters")
        String code,

        @Size(max = 500, message = "context must be 500 characters or fewer")
        String context
) {
}