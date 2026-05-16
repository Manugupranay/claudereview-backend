\# CLAUDE.md — ClaudeReview Backend



You are assisting on the ClaudeReview backend: a Spring Boot service that uses the Anthropic Claude API to perform structured code reviews on Java and React snippets.



\## Purpose of this file

This file is read on every AI-assisted edit in this repo. It encodes the engineering standards, security requirements, and design conventions that any generated code MUST follow. You are not generating code in a vacuum — you are generating code for a production-style review service that must be defensible to senior reviewers.



\---



\## Tech stack (do not deviate without explicit confirmation)

\- Java 17 (Temurin)

\- Spring Boot 3.x

\- Maven (not Gradle)

\- Jackson for JSON

\- Spring WebClient or `java.net.http.HttpClient` for outbound HTTP — NOT RestTemplate (deprecated for new code)

\- JUnit 5 + Mockito for tests

\- SLF4J for logging — NEVER `System.out.println`



\## Architecture principles

1\. \*\*Controllers are thin.\*\* They validate input and delegate to services. No business logic in controllers.

2\. \*\*Services are pure.\*\* They do not know about HTTP. They take typed inputs, return typed outputs, and throw typed exceptions.

3\. \*\*DTOs are immutable.\*\* Use Java `record` types for request/response shapes.

4\. \*\*No leaking implementation details.\*\* External callers should never see stack traces or Claude's raw response. All errors are translated to clean, documented error responses.



\---



\## Security and responsible AI usage (NON-NEGOTIABLE)

This service handles user-submitted source code. Treat every input as untrusted.



\- \*\*API keys\*\* are read from environment variables ONLY. Never hardcoded, never committed, never logged. Use `${ANTHROPIC\_API\_KEY}` via Spring's `@Value`.

\- \*\*Input size limits.\*\* Reject code submissions over 50,000 characters with HTTP 413. We will not silently truncate user code.

\- \*\*No logging of submitted code.\*\* We log metadata (length, language, request id) but never the code itself, in case it contains secrets the user accidentally pasted.

\- \*\*No logging of Claude's full response body\*\* at INFO level. Only at DEBUG, and never in production profile.

\- \*\*Outbound calls to Claude API\*\* must have explicit timeouts (connect: 5s, read: 60s).

\- \*\*Retries\*\* are bounded (max 2 retries, exponential backoff) and only on retryable errors (429, 503, network failures). Never retry on 400-class errors.

\- \*\*No PII collected.\*\* No user accounts, no email storage, no IP logging beyond standard access logs.



\## What Claude is allowed to do

Claude (the API the backend calls, not you Cursor) is given user code and asked to return findings. The system prompt instructs Claude to:

\- Return ONLY valid JSON matching our schema (enforced by JSON parsing on receipt).

\- Never execute or evaluate code.

\- Never invent line numbers — if uncertain, use null.

\- Provide reasoning for every finding so a human reviewer can accept/reject it.



If Claude's response fails to parse as valid JSON, we return HTTP 502 with a "Claude returned malformed output" error. We do NOT attempt to "fix" the JSON heuristically.



\---



\## Code quality standards

\- \*\*Every public method\*\* has Javadoc explaining what it does, what it throws, and any non-obvious behavior.

\- \*\*No magic numbers.\*\* Constants go in a `Constants` class or as `@ConfigurationProperties`.

\- \*\*Exceptions are typed.\*\* Create custom exceptions: `CodeTooLargeException`, `ClaudeApiException`, `MalformedClaudeResponseException`. Don't throw `RuntimeException`.

\- \*\*Null safety.\*\* Use `Optional` for return values that may be absent. Annotate parameters that must not be null with `@NonNull`.

\- \*\*Logging discipline.\*\* Every external call (Claude API) is logged with: request id, latency, status. Failures are logged at WARN or ERROR with context, never silently swallowed.

\- \*\*Tests.\*\* Every service method has at least one unit test. Controllers have at least one integration test using `MockMvc`. Aim for meaningful tests, not coverage theater.



\---



\## What to ask before generating code

If a request is ambiguous, STOP and ask. Examples:

\- "Should this be cached?" — ask about cache invalidation strategy

\- "Should this be async?" — ask about expected throughput

\- "What's the error response shape?" — confirm before designing



Do not invent requirements. Do not add features that weren't asked for. Do not "improve" the design by introducing patterns the author didn't request.



\## Style

\- Use Java 17 features where idiomatic: records, switch expressions, pattern matching, text blocks.

\- Prefer immutability. Final by default.

\- Prefer composition over inheritance.

\- Names are nouns for classes, verbs for methods, lowercase-with-hyphens for package names is forbidden — use lowercase concatenation: `com.claudereview.api`, not `com.claude-review.api`.



\## Out of scope for this project

\- No database. Reviews are stateless.

\- No authentication. The deployed service will be rate-limited at the edge.

\- No user accounts.

\- No frontend code in this repo. The React app lives in `claudereview-frontend`.

