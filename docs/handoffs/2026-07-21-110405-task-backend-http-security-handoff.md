# task-backend-http-security handoff

Signature: HND task-backend-http-security [in_progress] topics: backend, security → predecessor: 2026-07-21-104203-task-backend-http-security-handoff.md

## Durable — safe to cite later

- Focused RED evidence was captured before the HTTP boundary implementation.
  The bounded limiter unit suite, request body limit, stable problem response,
  forwarded-header fallback, and local-only CORS contracts now pass in focused
  runs.
- The remaining contract requires unexposed `/api/**` and `/actuator/**`
  paths plus production CORS preflights to be rejected explicitly. A missing
  access-control allow-origin header alone is not the planned status contract.
- Tests must not be weakened to accept the current framework fallbacks. The
  smallest proposed next change is a dedicated path-scoped deny filter before
  MVC, ordered after the local CORS handling and before authorization.

## Snapshot at 2026-07-21T11:04:05Z — re-verify live before use

- Three matcher approaches produced the same focused outcome under Boot 4.1.0
  and Security 7.1: string matchers, explicit path-pattern matchers, and custom
  method/path request matchers. Unexposed actuator paths still returned `404`,
  and a production preflight still returned `200` without an allow-origin
  header instead of the required explicit rejection.
- The branch contains one committed task-start metadata commit and uncommitted
  TDD implementation files. No feature commit, push, PR, Ready transition, or
  merge has occurred.
- The desktop-root worktree, frontend, PR #30, CI workflows, production, and
  secrets remain untouched.

## Next steps — conditional, each requires the stated live check

1. Obtain an explicit user decision before a fourth implementation attempt for
   the deny-path root cause, as required by the repository tripwire.
2. If authorized, add one dedicated path-scoped rejection filter without
   weakening any test, then rerun the two failing contracts first.
3. Only after focused GREEN, run the complete Maven gate, review, successor
   handoff, commits, normal push, Draft PR, and exact-head CI verification.
