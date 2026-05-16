# ClaudeReview API Contract — v1

This document defines the public API for the ClaudeReview backend. The implementation MUST match this contract. Changes to this contract require bumping `/api/v1` to `/api/v2`.

---

## POST /api/v1/reviews

Submits a code snippet for review by Claude. Returns structured findings.

### Request

**Headers:**
- `Content-Type: application/json` (required)

**Body:**

```json
{
  "language": "java",
  "code": "public class HelloController { ... }",
  "context": "Spring Boot REST controller for user registration"
}
```

| Field | Type | Required | Constraints |
|---|---|---|---|
| `language` | string | yes | enum: `java`, `react` |
| `code` | string | yes | 1–50,000 chars |
| `context` | string | no | max 500 chars |

### Successful response (200 OK)

```json
{
  "reviewId": "rev_8f3a2b1c",
  "language": "java",
  "model": "claude-sonnet-4-5",
  "summary": "Found 4 issues: 1 critical security, 2 medium reliability, 1 low style.",
  "findings": [
    {
      "id": "f_1",
      "severity": "critical",
      "category": "security",
      "lineNumber": 24,
      "title": "SQL injection via string concatenation",
      "description": "User input is concatenated directly into the SQL query.",
      "suggestion": "Use PreparedStatement with parameterized queries.",
      "confidence": "high"
    }
  ],
  "metadata": {
    "codeLengthChars": 1247,
    "latencyMs": 3421,
    "promptTokens": 1850,
    "completionTokens": 612
  }
}
```

### Finding object schema

| Field | Type | Notes |
|---|---|---|
| `id` | string | Stable within this review only (e.g. `f_1`, `f_2`) |
| `severity` | string | enum: `critical`, `high`, `medium`, `low`, `info` |
| `category` | string | enum: `security`, `reliability`, `performance`, `maintainability`, `style`, `testing`, `observability` |
| `lineNumber` | integer or null | Best-effort line in the submitted code. Null if not localizable. |
| `title` | string | Short headline, <= 100 chars |
| `description` | string | Why this is a finding |
| `suggestion` | string | What to do about it |
| `confidence` | string | enum: `high`, `medium`, `low` — how sure Claude is |

### Error responses

All errors return this shape:

```json
{
  "error": {
    "code": "CODE_TOO_LARGE",
    "message": "Submitted code exceeds the 50,000 character limit.",
    "requestId": "req_a8e2c1d4"
  }
}
```

| HTTP | code | Trigger |
|---|---|---|
| 400 | `INVALID_INPUT` | Missing or invalid field |
| 413 | `CODE_TOO_LARGE` | Code exceeds 50,000 chars |
| 502 | `CLAUDE_MALFORMED` | Claude returned unparseable output |
| 504 | `CLAUDE_TIMEOUT` | Claude API exceeded read timeout |
| 429 | `RATE_LIMITED` | Throttled by Claude or by us |
| 500 | `INTERNAL_ERROR` | Unhandled exception (no stack trace leaked) |

---

## GET /actuator/health

Standard Spring Boot Actuator health endpoint. Used for liveness probes.

Returns `200 OK` with `{"status":"UP"}` when service is healthy.

---

## Design notes

1. **Why structured output instead of free text?** Frontends, IDE plugins, and CI gates need to filter, sort, and act on findings programmatically. Free text forces every consumer to write a parser.
2. **Why `confidence` per finding?** AI output isn't uniformly reliable. A confidence score lets downstream consumers decide their own threshold (e.g., auto-approve high-confidence security findings, require human review for low-confidence style suggestions).
3. **Why `lineNumber` can be null?** Claude sometimes can't reliably localize a finding (e.g., "the overall architecture is wrong"). Forcing a fake line number would be worse than admitting we don't know.
4. **Why version the path (`/api/v1`)?** Future evolution. When we add streaming responses, multi-file submissions, or breaking changes, we move to `/api/v2` while keeping v1 alive.