# task-leads-api handoff

Signature: HND task-leads-api [draft_pr] topics: backend, security, tracker → predecessor: none

## Durable — safe to cite later

- `POST /api/leads` now enforces the canonical legitimate/honeypot JSON
  boundary, normalizes immutable request data, computes a keyed canonical
  HMAC-SHA-256 fingerprint, and creates lead/outbox rows atomically.
- `LeadAcceptanceService.accept()` remains outside a transaction. Legitimate
  requests enter a separate explicitly `READ_COMMITTED` JDBC transaction;
  honeypots stop before Bean Validation, normalization, HMAC, or PostgreSQL.
- The unique `request_id` constraint remains the concurrency authority.
  Equal, conflicting, retained-null, unexpectedly absent, rollback, and
  deterministic equal/different racing cases are covered.
- Feature commit `41d5788370e3d13450481ed798887f02848087b0`
  leaves OpenAPI, Flyway V1, frontend, CI workflows, and production
  services/configuration unchanged.
- The user authorized implementation, normal push, and a Draft PR only.
  Ready, merge, and production deployment require separate explicit commands.

## Snapshot at 2026-07-24T17:56Z — re-verify live before use

- Fresh `origin/main` remains
  `ac7d21a023e658ef369d57037c59351b1d9c505f`, the verified squash merge of
  PR #37. Branch `task-leads-api` is based directly on that commit.
- Draft [PR #38](https://github.com/devDaniilNovikov/AndrewWebSite/pull/38)
  is open and mergeable against `main`; before this metadata checkpoint its
  exact head is `41d5788370e3d13450481ed798887f02848087b0`.
- The focused lead suite passed 80 tests. Full `./mvnw -B verify` passed
  149 tests against PostgreSQL 18.4 with Flyway V1 and the JaCoCo gate;
  line coverage is 577/625 (92.32%).
- Semgrep reported no Java finding, TruffleHog reported no verified or
  unverified secret, and the final diff/log/PII checks were clean.
  Independent specification, database/concurrency, and whole-diff code
  reviews reported no Critical, Important, or Minor finding after fixes.
- GitHub checks started on the feature head, but this metadata commit creates
  a new exact head. No earlier check result may be reused for readiness.
- Frontend, Draft PR #30, the desktop-root user files, production services,
  secrets, and PII outside fictional test fixtures remain untouched.

## Next steps — conditional, each requires the stated live check

1. Commit and normally push this tracker/handoff checkpoint, then wait for
   every required and applicable GitHub check on its exact SHA.
2. If an in-scope check fails, reproduce and fix it through the normal TDD
   and review loop, then write a successor handoff before another checkpoint.
3. If exact-head CI remains green, keep PR #38 Draft and wait for a separate
   explicit user command before marking it Ready.
4. Merge and production deployment remain separately unauthorized.
