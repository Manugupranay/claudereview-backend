package com.claudereview.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the Spring context starts with every bean wired.
 *
 * <p>Runs under the "test" profile so ClaudeConfig gets a dummy API key from
 * src/test/resources/application-test.properties. Without it the context fails
 * to start and this test can never pass on CI.
 */
@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
