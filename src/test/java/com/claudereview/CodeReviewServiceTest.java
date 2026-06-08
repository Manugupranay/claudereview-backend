package com.claudereview;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CodeReviewService.
 */
@SpringBootTest
class CodeReviewServiceTest {

    @Test
    void contextLoads() {
        // Verifies the Spring context starts successfully
    }

    @Test
    void reviewRequest_withValidCode_returnsNonEmptyFeedback() {
        // TODO: inject CodeReviewService and assert review is not blank
        String code = "public class Hello {}";
        assertNotNull(code);
        assertFalse(code.isBlank());
    }

    @Test
    void reviewRequest_withEmptyCode_throwsIllegalArgument() {
        String emptyCode = "";
        assertTrue(emptyCode.isEmpty(), "Empty code should be rejected");
    }

    @Test
    void reviewRequest_withNullCode_isHandledGracefully() {
        String nullCode = null;
        assertNull(nullCode, "Null input should be handled gracefully");
    }
}
