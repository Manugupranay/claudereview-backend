package com.claudereview.backend.controller;

import com.claudereview.backend.dto.Category;
import com.claudereview.backend.dto.Confidence;
import com.claudereview.backend.dto.ErrorResponse;
import com.claudereview.backend.dto.Finding;
import com.claudereview.backend.dto.Metadata;
import com.claudereview.backend.dto.ReviewRequest;
import com.claudereview.backend.dto.ReviewResponse;
import com.claudereview.backend.dto.Severity;
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

import java.util.List;
import java.util.UUID;

/**
 * Handles code review submissions.
 *
 * <p>This controller is intentionally thin:
 * <ul>
 *   <li>Validates the request body via {@code @Valid} on the parameter.</li>
 *   <li>Returns a stubbed response for now (real Claude integration arrives in Phase 2.7).</li>
 *   <li>Translates validation failures into structured {@link ErrorResponse} payloads.</li>
 * </ul>
 *
 * <p>Business logic (calling Claude, parsing findings, calculating metadata)
 * lives in a service layer, not here. Controllers should never know how the work gets done.
 */
@RestController
@RequestMapping("/api/v1")
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    /**
     * Submit code for review. Currently returns a hardcoded stub response.
     * Real Claude integration is wired in a later sub-step.
     */
    @PostMapping("/reviews")
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewRequest request) {

        String reviewId = "rev_" + UUID.randomUUID().toString().substring(0, 8);

        log.info("Received review request: reviewId={} language={} codeLength={}",
                reviewId, request.language(), request.code().length());

        // Stub response — replace with real Claude call in Phase 2.7.
        ReviewResponse stub = new ReviewResponse(
                reviewId,
                request.language(),
                "claude-sonnet-4-5",
                "Stub response — Claude integration not yet wired. 1 example finding shown.",
                List.of(
                        new Finding(
                                "f_1",
                                Severity.MEDIUM,
                                Category.MAINTAINABILITY,
                                null, // lineNumber unknown for stub
                                "Stub finding (not real)",
                                "This is a hardcoded finding returned by the stub controller. "
                                        + "Real findings appear once the Claude API call is wired up.",
                                "Wait until Phase 2.7 ships the Claude integration.",
                                Confidence.LOW
                        )
                ),
                new Metadata(
                        request.code().length(),
                        0L,    // latencyMs — none, since we didn't actually call anything
                        0,     // promptTokens
                        0      // completionTokens
                )
        );

        return ResponseEntity.ok(stub);
    }

    /**
     * Translates Bean Validation failures (from {@code @Valid}) into a clean 400 response
     * matching {@code API_CONTRACT.md}.
     *
     * <p>Without this handler, Spring's default error response leaks internal details
     * and doesn't match our documented error shape.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request body");

        String requestId = "req_" + UUID.randomUUID().toString().substring(0, 8);

        log.warn("Validation failed: requestId={} message={}", requestId, message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("INVALID_INPUT", message, requestId));
    }
}