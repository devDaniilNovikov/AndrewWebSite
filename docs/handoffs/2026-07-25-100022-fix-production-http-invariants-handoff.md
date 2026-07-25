# fix-production-http-invariants handoff

Signature: HND fix-production-http-invariants [in_progress] topics: backend, security, deploy, tracker → predecessor: 2026-07-25-075015-fix-production-http-invariants-handoff.md

## Durable — safe to cite later

- This task owns fail-fast production invariants for forwarded-header
  processing, the exact public Actuator web surface, the enabled application
  rate limiter, safe error handling, and early runtime-profile validation.
- Telegram is out of scope. The user clarified that Telegram is only an
  external hyperlink to the owner's profile; any correction of the obsolete
  bot/outbox assumptions belongs to a separate local-only branch with no push
  or PR until a product decision defines the lead form's destination.
- The current feature diff is deliberately uncommitted while second-pass
  review findings are remediated. Preserve the existing worktree and do not
  discard, stash, reset, or rewrite it.
- The user authorized the normal Draft PR, CI/review, Ready, and guarded
  squash-merge lifecycle for confirmed non-Telegram fixes. Production deploy
  remains unauthorized.
- `$code-reviewer` remains the governing review workflow. Every Important
  finding must be fixed and re-reviewed before the feature commit and Draft
  PR.

## Snapshot at 2026-07-25T10:00:22Z — re-verify live before use

- Worktree:
  `/Users/daniilnovikov/.codex/worktrees/production-http-invariants/AndrewWorkWebSite`.
  Branch `fix-production-http-invariants` is based on live `origin/main`
  `4da6448495348246198996d2814de275ce7b5de8`; metadata head is
  `d8c18634926a67afb4fa257c7c6a7840c9db14d7`.
- The dirty feature diff contains:
  `RuntimeProfileGuard` lazy-initialization hardening, a new
  `ProductionHttpInvariantGuard`, explicit safe Boot 4.1 HTTP/error/Actuator
  configuration, a hostile startup matrix, and real prod/Tomcat HTTP tests.
- Latest completed full local gate on that exact pre-review-fix diff:
  `./mvnw -B verify` succeeded with 249/249 tests, PostgreSQL 18.4
  Testcontainers, Flyway V1, and JaCoCo 92.78% lines / 85.35% branches.
  Semgrep ran 89 rules over 128 targets with 0 findings; TruffleHog found 0;
  `git diff --check` passed.
- First and second review iterations already resolved lazy deferral of the
  HTTP guard, obsolete Boot error-property prefixes, effective health access,
  health-group semantics, Actuator CORS/additional paths, Tomcat RemoteIpValve
  triggers, and a `Retry-After` boundary flake.
- The final second-pass reviews found four unresolved Important issues:
  Boot's relaxed `EndpointId` aliases bypass raw exposure/path-mapping checks;
  `spring.main.web-application-type=none` and `server.port=-1` can pass without
  an HTTP listener; moving `RuntimeProfileGuard` to
  `SmartInitializingSingleton` allows Flyway/singleton side effects before an
  invalid profile is rejected; and the direct `/error` test proves neither a
  real ERROR dispatch nor closure of that backend route.
- One reviewer also reported two Minor equivalence observations. A blank
  health path mapping should be accepted because Boot treats it as absent.
  `UNRESTRICTED` health access must remain rejected intentionally: pinning
  `READ_ONLY` is least privilege and prevents a future write operation from
  becoming reachable after a dependency upgrade.
- The exact-security reviewer confirmed the earlier lazy/profile and Boot 4.1
  error-property findings were fixed, then its second-pass service call ended
  with an external HTTP 503 before it could finalize one candidate. Re-run
  that review after the new fixes; do not treat the interrupted pass as green.
- No feature commit, push, PR, Ready transition, merge, or deploy exists for
  this task.

## Next steps — conditional, each requires the stated live check

1. Re-read the current dirty diff and reproduce RED for: relaxed Actuator IDs
   in `exclude` and `path-mapping`; non-web production and negative server
   port; invalid profile before any `InitializingBean` side effect; direct
   `/error` closure plus a genuine servlet ERROR dispatch with diagnostic
   redaction. Also prove a blank health mapping remains semantically safe.
2. Implement the smallest GREEN change: compare Actuator IDs via Boot
   `EndpointId`; require a servlet application and non-negative server port;
   move profile validation to a lazy-proof early bean-factory phase; close a
   direct `/error` request while preserving internal ERROR dispatch. Keep
   health access exactly `READ_ONLY`.
3. Run focused tests, full Maven verify, JaCoCo, Semgrep, TruffleHog, and
   `git diff --check`; then re-run all three independent exact-security,
   operational, and test reviews against the final exact diff. Only with zero
   unresolved Critical/Important/actionable Minor findings should the feature
   be atomically committed and proceed through the already authorized
   Draft-PR/CI/Ready/guarded-squash-merge lifecycle. Re-fetch and verify
   `origin/main`, exact PR head, checks, reviews, alerts, and branch-retention
   setting before every mutable GitHub action. Do not deploy.
