package com.claudereview.backend.controller;

import com.claudereview.backend.claude.ClaudeApiException;
import com.claudereview.backend.dto.ErrorResponse;
import com.claudereview.backend.dto.ReviewRequest;
import com.claudereview.backend.dto.ReviewResponse;
import com.claudereview.backend.service.ReviewService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Handles code review submissions.
 *
 * <p>This controller is intentionally thin:
 * <ul>
 *   <li>Validates the request body via {@code @Valid} on the parameter.</li>
 *   <li>Delegates to {@link ReviewService} for all business logic.</li>
 *   <li>Translates exceptions into structured {@link ErrorResponse} payloads
 *       matching the contract in {@code API_CONTRACT.md}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/reviews")
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewRequest request) {
        log.info("Received review request: language={} codeLength={}",
                request.language(), request.code().length());

        ReviewResponse response = reviewService.review(request);
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request body");

        String requestId = newRequestId();
        log.warn("Validation failed: requestId={} message={}", requestId, message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("INVALID_INPUT", message, requestId));
    }

    @ExceptionHandler(ClaudeApiException.class)
    public ResponseEntity<ErrorResponse> handleClaudeApiException(ClaudeApiException ex) {
        String requestId = newRequestId();

        HttpStatus status;
        String code;
        String message;

        if (ex.isTimeout()) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            code = "CLAUDE_TIMEOUT";
            message = "The review service took too long to respond. Please retry.";
        } else if (ex.isRateLimit()) {
            status = HttpStatus.TOO_MANY_REQUESTS;
            code = "RATE_LIMITED";
            message = "The review service is currently rate-limited. Please retry shortly.";
        } else if (ex.getCause() instanceof com.fasterxml.jackson.core.JsonProcessingException) {
            status = HttpStatus.BAD_GATEWAY;
            code = "CLAUDE_MALFORMED";
            message = "The review service returned an unexpected response shape.";
        } else {
            status = HttpStatus.BAD_GATEWAY;
            code = "CLAUDE_MALFORMED";
            message = "The review service failed to produce a valid response.";
        }

        log.warn("Claude API failure: requestId={} status={} code={} cause={}",
                requestId, status.value(), code, ex.getClass().getSimpleName());

        return ResponseEntity
                .status(status)
                .body(ErrorResponse.of(code, message, requestId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        String requestId = newRequestId();
        log.error("Unhandled exception: requestId={} type={}", requestId, ex.getClass().getName(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred.", requestId));
    }

    private static String newRequestId() {
        return "req_" + UUID.randomUUID().toString().substring(0, 8);
    }
}