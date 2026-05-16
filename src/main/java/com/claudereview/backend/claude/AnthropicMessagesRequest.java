package com.claudereview.backend.claude;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * Request body for the Anthropic Messages API.
 * Maps exactly to https://docs.anthropic.com/en/api/messages
 *
 * <p>Kept separate from our internal DTOs because this is Anthropic's contract,
 * not ours — if they change theirs, only this file changes.
 */
@JsonPropertyOrder({ "model", "max_tokens", "system", "messages" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnthropicMessagesRequest(
        String model,
        @JsonProperty("max_tokens") int maxTokens,
        String system,
        List<Message> messages
) {
    public record Message(String role, String content) {
    }
}