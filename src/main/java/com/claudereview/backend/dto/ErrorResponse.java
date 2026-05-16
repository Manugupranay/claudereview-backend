package com.claudereview.backend.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Standard error envelope returned for every non-2xx response.
 * Keeping a consistent error shape makes frontend handling much simpler —
 * one error parser, every endpoint.
 */
public record ErrorResponse(
        @JsonPropertyOrder({ "code", "message", "requestId" })
        ErrorBody error
) {
    public record ErrorBody(
            String code,
            String message,
            String requestId
    ) {
    }

    /**
     * Convenience factory for the common case.
     */
    public static ErrorResponse of(String code, String message, String requestId) {
        return new ErrorResponse(new ErrorBody(code, message, requestId));
    }
}