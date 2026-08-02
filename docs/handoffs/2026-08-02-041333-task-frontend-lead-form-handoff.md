# task-frontend-lead-form startup handoff

Signature: HND task-frontend-lead-form [in_progress] topics: frontend, api, accessibility, privacy, testing, tracker → predecessor task: `2026-08-02-033637-task-frontend-landing-page-ready-handoff.md`

## Durable — safe to cite later

- The user authorized F5 implementation, push, and one Draft PR. Ready,
  squash-merge, deployment, production mutation, branch deletion, and PR #30
  changes remain outside the current authorization.
- F5 owns `frontend/**` plus its tracker and handoff metadata. Backend,
  `docs/backend/openapi.yaml`, `.github/**`, credentials, production content,
  and deployment remain unchanged.
- F5A is planned as `task-frontend-openapi-ci-path` after F5 merges. It must
  use a fresh owner-authored sanitized Issue and only the guarded
  `jules-action` route; the native F1A exception does not carry forward.
- Preview submission fails closed. A hosted preview cannot collect leads,
  production remains blocked by the readiness manifest, and no unverified
  phone, legal text, price, proof, or photograph may be introduced.

## Verified snapshot at 2026-08-02T04:13:33Z — re-verify live before use

- Fresh `origin/main` and the new `task-frontend-lead-form` worktree both
  pointed to `6477af195c87598e0283cd53a0b09135b8b20c21`. No existing F5 branch,
  PR, Issue, or parallel worktree owner was found.
- PR #63 was squash-merged into `main` as `6477af1`; its retained source
  branch pointed to `8b24a7f`. Post-merge CI, Dependency Submission, and
  Semgrep completed successfully, and automatic branch deletion remained
  disabled.
- PR #30 remained open Draft at
  `00f55eea452884dc4c286ae17fcb6e1d4ebc25d8` and outside F5.
- The canonical backend OpenAPI contract is already merged. F5 will generate
  TypeScript types from it without editing the schema or duplicating its
  request model manually.

## Conditional continuation — re-verify live

1. Add deterministic OpenAPI type generation and RED tests for validation,
   endpoint policy, immutable attempts, status mapping, timeout, privacy, and
   the static network boundary.
2. Implement the smallest accessible form and single allowlisted transport
   that make the tests green; run unit, component, axe, Playwright, export,
   audit, Maven, secret/PII, and independent review gates.
3. Record fresh evidence on the final exact head, publish one Draft PR, wait
   for exact-head CI, and stop before Ready or merge.
