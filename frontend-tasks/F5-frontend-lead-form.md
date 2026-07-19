# F5 — Lead form and API client

## Objective

Implement the accessible lead form and typed same-origin client against the
canonical backend contract, including safe retries, errors, consent, and call
fallbacks.

## Ownership and prerequisites

- **Owner:** frontend agent assigned by the user.
- **Prerequisite:** F2 and `task-leads-api` merged; start from their resulting
  fresh `origin/main`.
- **Owned paths:** `frontend/**` and the task-specific handoff. Backend and
  OpenAPI changes require a separate Codex-owned task.
- **Sources:** [OpenAPI](../docs/backend/openapi.yaml),
  [architecture](../docs/backend/architecture.md),
  [product brief](../docs/product/technical-brief.ru.md), and
  [Git Flow](../.agents/workflows/GIT_FLOW.md).

## Tasks

1. Implement the typed same-origin client and form payload directly from
   OpenAPI, including contract-required idempotency, intent, source, consent,
   and anti-automation behavior without redefining server rules.
2. Implement client validation for UX, all documented success/error states,
   in-flight and retry behavior, accessible field errors/status messages, and
   a persistent phone fallback. The server remains authoritative.
3. Add unit, component, accessibility, retry/idempotency, error-mapping, and
   browser E2E tests against a contract-faithful test boundary.

## Acceptance

- Retrying one submission reuses its contract identity; a new submission does
  not. Every documented response degrades safely.
- Lead PII never enters URLs, analytics, logs, browser persistence, fixtures,
  or snapshots; no credential or direct Telegram integration exists.
- Format, lint, strict typecheck, tests, build, dependency audit, secret/PII
  review, required CI, and Codex contract/security review pass.
- The PR stops at Ready and requires separate merge authorization.
