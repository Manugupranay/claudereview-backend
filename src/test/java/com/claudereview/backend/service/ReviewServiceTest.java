package com.claudereview.backend.service;

import com.claudereview.backend.claude.ClaudeApiException;
import com.claudereview.backend.claude.ClaudeClient;
import com.claudereview.backend.claude.PromptLoader;
import com.claudereview.backend.dto.Category;
import com.claudereview.backend.dto.Confidence;
import com.claudereview.backend.dto.Finding;
import com.claudereview.backend.dto.Language;
import com.claudereview.backend.dto.ReviewRequest;
import com.claudereview.backend.dto.ReviewResponse;
import com.claudereview.backend.dto.Severity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ReviewService}.
 *
 * <p>Both collaborators are mocked, so nothing here starts a Spring context,
 * reads configuration, or calls the Anthropic API. The subject under test is
 * the part that is genuinely ours: how Claude's raw text is turned into a
 * {@link ReviewResponse}, and what we send it in the first place.
 */
class ReviewServiceTest {

    private static final String SYSTEM_PROMPT = "You are a code reviewer.";
    private static final String MODEL = "claude-sonnet-4-5";
    private static final String NL = System.lineSeparator();
    private static final String FENCE = "```";

    private static final String VALID_PAYLOAD = """
            {
              "summary": "Two issues worth fixing before merge.",
              "findings": [
                {
                  "id": "f1",
                  "severity": "HIGH",
                  "category": "SECURITY",
                  "lineNumber": 42,
                  "title": "SQL built by string concatenation",
                  "description": "User input reaches the query unescaped.",
                  "suggestion": "Use a PreparedStatement.",
                  "confidence": "HIGH"
                },
                {
                  "id": "f2",
                  "severity": "LOW",
                  "category": "STYLE",
                  "lineNumber": null,
                  "title": "Inconsistent brace placement",
                  "description": "Mixed styles across the file.",
                  "suggestion": "Run the formatter.",
                  "confidence": "MEDIUM"
                }
              ]
            }
            """;

    private ClaudeClient claudeClient;
    private PromptLoader promptLoader;
    private ReviewService service;

    @BeforeEach
    void setUp() {
        claudeClient = mock(ClaudeClient.class);
        promptLoader = mock(PromptLoader.class);
        when(promptLoader.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        service = new ReviewService(claudeClient, promptLoader, new ObjectMapper());
    }

    private void claudeReturns(String text) {
        when(claudeClient.call(anyString(), anyString()))
                .thenReturn(new ClaudeClient.ClaudeCallResult(text, MODEL, 1234L, 900, 350));
    }

    private static ReviewRequest request(String code, String context) {
        return new ReviewRequest(Language.JAVA, code, context);
    }

    @Nested
    @DisplayName("parsing Claude's response")
    class Parsing {

        @Test
        void mapsFindingsOntoTheResponse() {
            claudeReturns(VALID_PAYLOAD);

            ReviewResponse response = service.review(request("class A {}", null));

            assertEquals("Two issues worth fixing before merge.", response.summary());
            assertEquals(2, response.findings().size());

            Finding first = response.findings().get(0);
            assertEquals("f1", first.id());
            assertEquals(Severity.HIGH, first.severity());
            assertEquals(Category.SECURITY, first.category());
            assertEquals(Integer.valueOf(42), first.lineNumber());
            assertEquals(Confidence.HIGH, first.confidence());
        }

        @Test
        void keepsANullLineNumberRatherThanInventingOne() {
            claudeReturns(VALID_PAYLOAD);

            ReviewResponse response = service.review(request("class A {}", null));

            assertNull(response.findings().get(1).lineNumber());
        }

        @Test
        void stripsAJsonLabelledMarkdownFence() {
            claudeReturns(FENCE + "json" + NL + VALID_PAYLOAD + FENCE);

            ReviewResponse response = service.review(request("class A {}", null));

            assertEquals(2, response.findings().size());
        }

        @Test
        void stripsABareMarkdownFence() {
            claudeReturns(FENCE + NL + VALID_PAYLOAD + FENCE);

            ReviewResponse response = service.review(request("class A {}", null));

            assertEquals(2, response.findings().size());
        }

        @Test
        void turnsAbsentFindingsIntoAnEmptyListNotNull() {
            claudeReturns("""
                    {"summary": "Nothing to flag."}
                    """);

            ReviewResponse response = service.review(request("class A {}", null));

            assertNotNull(response.findings());
            assertTrue(response.findings().isEmpty());
        }

        @Test
        void rejectsUnparseableOutputInsteadOfGuessing() {
            claudeReturns("I'd be happy to review that code for you!");

            ClaudeApiException thrown = assertThrows(
                    ClaudeApiException.class,
                    () -> service.review(request("class A {}", null))
            );
            assertTrue(thrown.getMessage().contains("did not match the expected review schema"));
        }

        @Test
        void rejectsTruncatedJson() {
            claudeReturns("""
                    {"summary": "Cut off mid-
                    """);

            assertThrows(
                    ClaudeApiException.class,
                    () -> service.review(request("class A {}", null))
            );
        }
    }

    @Nested
    @DisplayName("response envelope")
    class Envelope {

        @Test
        void carriesTheRequestLanguageAndTheModelClaudeReported() {
            claudeReturns(VALID_PAYLOAD);

            ReviewResponse response = service.review(request("class A {}", null));

            assertEquals(Language.JAVA, response.language());
            assertEquals(MODEL, response.model());
        }

        @Test
        void reportsCodeLengthLatencyAndTokenUsage() {
            claudeReturns(VALID_PAYLOAD);
            String code = "class A {}";

            ReviewResponse response = service.review(request(code, null));

            assertEquals(code.length(), response.metadata().codeLengthChars());
            assertEquals(1234L, response.metadata().latencyMs());
            assertEquals(900, response.metadata().promptTokens());
            assertEquals(350, response.metadata().completionTokens());
        }

        @Test
        void generatesAPrefixedReviewId() {
            claudeReturns(VALID_PAYLOAD);

            String reviewId = service.review(request("class A {}", null)).reviewId();

            assertTrue(reviewId.startsWith("rev_"), reviewId);
            assertEquals(12, reviewId.length(), reviewId);
        }

        @Test
        void givesEveryReviewItsOwnId() {
            claudeReturns(VALID_PAYLOAD);

            String first = service.review(request("class A {}", null)).reviewId();
            String second = service.review(request("class A {}", null)).reviewId();

            assertFalse(first.equals(second));
        }
    }

    @Nested
    @DisplayName("the message sent to Claude")
    class OutboundMessage {

        private String captureUserMessage(ReviewRequest request) {
            claudeReturns(VALID_PAYLOAD);
            service.review(request);

            ArgumentCaptor<String> system = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> user = ArgumentCaptor.forClass(String.class);
            verify(claudeClient).call(system.capture(), user.capture());

            assertEquals(SYSTEM_PROMPT, system.getValue());
            return user.getValue();
        }

        @Test
        void namesTheLanguageUsingItsPromptLabel() {
            String message = captureUserMessage(request("class A {}", null));

            assertTrue(message.contains(Language.JAVA.promptLabel()), message);
        }

        @Test
        void includesTheCodeUnderReview() {
            String code = "class Widget { void spin() {} }";

            String message = captureUserMessage(request(code, null));

            assertTrue(message.contains(code), message);
        }

        @Test
        void includesTheEngineersContextWhenGiven() {
            String message = captureUserMessage(request("class A {}", "This runs on every request."));

            assertTrue(message.contains("Context provided by the engineer"), message);
            assertTrue(message.contains("This runs on every request."), message);
        }

        @Test
        void omitsTheContextSectionWhenBlank() {
            String message = captureUserMessage(request("class A {}", "   "));

            assertFalse(message.contains("Context provided by the engineer"), message);
        }

        @Test
        void omitsTheContextSectionWhenNull() {
            String message = captureUserMessage(request("class A {}", null));

            assertFalse(message.contains("Context provided by the engineer"), message);
        }
    }
}
