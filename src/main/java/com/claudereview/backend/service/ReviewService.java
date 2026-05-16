package com.claudereview.backend.service;

import com.claudereview.backend.claude.ClaudeApiException;
import com.claudereview.backend.claude.ClaudeClient;
import com.claudereview.backend.claude.PromptLoader;
import com.claudereview.backend.dto.Finding;
import com.claudereview.backend.dto.Metadata;
import com.claudereview.backend.dto.ReviewRequest;
import com.claudereview.backend.dto.ReviewResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Orchestrates a code review.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Build the user message from the {@link ReviewRequest}.</li>
 *   <li>Call {@link ClaudeClient} with the system prompt and user message.</li>
 *   <li>Parse Claude's text response into the structured {@link ReviewResponse}.</li>
 *   <li>Reject malformed Claude output rather than try to "fix" it heuristically.</li>
 * </ul>
 */
@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ClaudeClient claudeClient;
    private final PromptLoader promptLoader;
    private final ObjectMapper objectMapper;

    public ReviewService(ClaudeClient claudeClient,
                         PromptLoader promptLoader,
                         ObjectMapper objectMapper) {
        this.claudeClient = claudeClient;
        this.promptLoader = promptLoader;
        this.objectMapper = objectMapper;
    }

    public ReviewResponse review(ReviewRequest request) {
        String reviewId = "rev_" + UUID.randomUUID().toString().substring(0, 8);

        String userMessage = buildUserMessage(request);
        String systemPrompt = promptLoader.getSystemPrompt();

        log.info("Starting review: reviewId={} language={} codeLength={}",
                reviewId, request.language(), request.code().length());

        ClaudeClient.ClaudeCallResult result = claudeClient.call(systemPrompt, userMessage);

        ClaudeReviewPayload parsed = parseClaudeOutput(result.text(), reviewId);

        Metadata metadata = new Metadata(
                request.code().length(),
                result.latencyMs(),
                result.inputTokens(),
                result.outputTokens()
        );

        log.info("Review complete: reviewId={} findings={} latencyMs={}",
                reviewId,
                parsed.findings() == null ? 0 : parsed.findings().size(),
                result.latencyMs());

        return new ReviewResponse(
                reviewId,
                request.language(),
                result.model(),
                parsed.summary(),
                parsed.findings() == null ? List.of() : parsed.findings(),
                metadata
        );
    }

    private String buildUserMessage(ReviewRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Language: ").append(request.language().promptLabel()).append("\n\n");

        if (request.context() != null && !request.context().isBlank()) {
            sb.append("Context provided by the engineer:\n")
                    .append(request.context())
                    .append("\n\n");
        }

        sb.append("Code to review:\n```\n")
                .append(request.code())
                .append("\n```\n\nReturn ONLY the JSON object as instructed in the system prompt.");

        return sb.toString();
    }

    private ClaudeReviewPayload parseClaudeOutput(String rawText, String reviewId) {
        String cleaned = stripMarkdownFences(rawText).trim();

        try {
            return objectMapper.readValue(cleaned, ClaudeReviewPayload.class);
        } catch (JsonProcessingException e) {
            log.warn("Claude returned unparseable review JSON: reviewId={} firstChars={}",
                    reviewId,
                    cleaned.substring(0, Math.min(80, cleaned.length())));
            throw new ClaudeApiException(
                    "Claude returned a response that did not match the expected review schema",
                    e
            );
        }
    }

    private String stripMarkdownFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed;
    }

    private record ClaudeReviewPayload(String summary, List<Finding> findings) {
    }
}