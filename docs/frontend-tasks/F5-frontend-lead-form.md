# F5 — Lead form and API client

## Objective

Implement the accessible lead form and typed same-origin client against the
canonical backend contract, including safe retries, errors, consent, and call
fallbacks.

## Ownership and prerequisites

- **Owner:** Claude Code.
- **Prerequisite:** F2 and `task-leads-api` merged; start from their resulting
  fresh `origin/main`.
- **Owned paths:** `frontend/**` and the task-specific handoff. Backend and
  OpenAPI changes require a separate Codex-owned task.
- **Sources:** [OpenAPI](../backend/openapi.yaml),
  [architecture](../backend/architecture.md),
  [product brief](../product/technical-brief.ru.md), and
  [Git Flow](../../.agents/workflows/GIT_FLOW.md).

## Tasks

1. Generate the same-origin client's schema types from the canonical OpenAPI
   file as a deterministic build/CI step with `openapi-typescript` or an
   equivalent approved generator, then implement the form payload without
   redefining server rules. Hand-written schema types are prohibited; preserve
   contract-required idempotency, intent, source, consent, and anti-automation
   behavior.
2. Implement client validation for UX, all documented success/error states,
   in-flight and retry behavior, accessible field errors/status messages, and
   a persistent phone fallback. The server remains authoritative.
3. Add unit, component, accessibility, retry/idempotency, error-mapping, and
   browser E2E tests against a contract-faithful network boundary using MSW
   or Playwright route interception, not client-function mocks.

## Acceptance

- Retrying one submission reuses its contract identity; a new submission does
  not. Every documented response degrades safely.
- Lead PII never enters URLs, analytics, logs, browser persistence, fixtures,
  or snapshots; no credential or direct Telegram integration exists.
- Format, lint, strict typecheck, tests, build, dependency audit, secret/PII
  review, required CI, and Codex contract/security review pass.
- The PR stops at Ready and requires separate merge authorization.
