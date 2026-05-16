package com.claudereview.backend.claude;

/**
 * Thrown when the Claude API call fails or returns an unusable response.
 * Lets upstream code distinguish Claude failures from other issues.
 */
public class ClaudeApiException extends RuntimeException {

    private final int statusCode;

    public ClaudeApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public ClaudeApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isTimeout() {
        return getCause() instanceof java.net.http.HttpTimeoutException
                || (getCause() != null && getCause().getCause() instanceof java.net.http.HttpTimeoutException);
    }

    public boolean isRateLimit() {
        return statusCode == 429;
    }
}