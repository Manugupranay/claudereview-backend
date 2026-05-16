package com.claudereview.backend.dto;

/**
 * Languages supported by the code review service.
 * Using a strict enum (not free-text) prevents prompt injection and
 * unsupported language requests reaching the Claude API.
 */
public enum Language {
    JAVA,
    REACT;

    /**
     * Returns the language label used in the Claude prompt.
     * Kept as a separate method so we can change wording without changing the enum.
     */
    public String promptLabel() {
        return switch (this) {
            case JAVA -> "Java (Spring Boot ecosystem)";
            case REACT -> "React (with TypeScript)";
        };
    }
}