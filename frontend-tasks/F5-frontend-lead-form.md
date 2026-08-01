# F5 — Preview lead form and API client

## Objective

Implement the accessible lead form and typed browser client against the
canonical backend contract with safe preview execution, retries, errors,
consent, and production gating.

## Ownership and prerequisites

- **Owner:** Codex.
- **Prerequisite:** F2 and `task-leads-api` merged; start from fresh
  `origin/main`.
- **Owned paths:** `frontend/**` and the task-specific handoff. Backend and
  OpenAPI changes require a separate task.
- **Sources:** [OpenAPI](../docs/backend/openapi.yaml),
  [architecture](../docs/backend/architecture.md),
  [preview design](../docs/frontend/landing-preview.md), and
  [Git Flow](../.agents/workflows/GIT_FLOW.md).

## Tasks

1. Deterministically generate TypeScript schema types from OpenAPI with
   `openapi-typescript` and build only the payload fields `requestId`, `name`,
   `phone`, optional `comment`, `sourcePath`, `intent` (`repair|maintenance`),
   `consent`, and honeypot `website`. Build `sourcePath` only from
   `window.location.pathname`.
2. Implement immutable in-flight attempts, same-payload idempotent retry,
   a `crypto.randomUUID()` identity that changes after an edit, 15-second
   timeout, `202`, `400`, `409`, `413`, `415`, `429`, `503`, network failures,
   and `Retry-After` handling, with accessible errors/status and loopback-only
   preview submission.
3. Add unit, component, accessibility, retry/idempotency, error-mapping,
   privacy, and Playwright network-boundary tests.

## Acceptance

- A hosted preview cannot submit; production uses only relative
  same-origin `/api/leads` after production-content validation succeeds.
- The form exposes visible name, phone, optional comment, explicit consent,
  and `repair|maintenance` controls; the `website` honeypot remains outside
  normal visual and keyboard flow.
- Lead PII never enters URLs, analytics, logs, storage, fixtures, or snapshots;
  no frontend Telegram integration or credential exists.
- Every documented response degrades safely and never echoes rejected values.
- Format, lint, strict typecheck, coverage, E2E, export, dependency audit,
  secret/PII review, frontend CI, and Codex review pass before Ready.
