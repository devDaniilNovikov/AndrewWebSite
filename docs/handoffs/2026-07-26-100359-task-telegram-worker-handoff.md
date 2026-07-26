# task-telegram-worker handoff

Signature: HND task-telegram-worker [in_progress] topics: backend, security, tracker → predecessor: none

## Durable — safe to cite later

- The worker implementation is complete locally in the dedicated
  `task-telegram-worker` worktree. The branch starts from the published
  Telegram client on `main`; it is not stacked on the retained client branch.
- The repository performs bounded expired-lease recovery and deterministic
  PostgreSQL `FOR UPDATE SKIP LOCKED` claims in one committed transaction,
  issues a distinct UUID lease per row, and uses state/token/unexpired-lease
  compare-and-set transitions. Telegram HTTP runs only after that transaction
  returns.
- The worker rechecks the lease, anonymization, and the strict 29-day privacy
  boundary immediately before send. Retry delay is overflow-safe and capped;
  scheduling, heartbeat, and Micrometer telemetry use the approved bounded
  defaults and tags, including `telegram_unexpected`.
- Public HTTP behavior, Flyway schema, readiness/OTLP, production
  configuration, HTTPS deployment, retention, and frontend scope are
  unchanged. The accepted crash-after-send window remains at-least-once with
  the same request ID.
- No push, pull request, Ready transition, merge, production deploy, or other
  remote mutation is authorized or has been performed for this branch.

## Snapshot at 2026-07-26T10:03:59Z — re-verify live before use

- Fresh base after the explicitly authorized rebase:
  `5056e6ea9b681c30393488a7015244badd8fec73` (`origin/main`).
- Repository/state-machine commit:
  `e72fd6d4d3308ff10d708505ab5475ba4717820b`.
- Scheduler/telemetry and reviewed implementation head:
  `7a156aecdc69a76828fb8de0e77cf8eb5a4ed468`.
- The focused Telegram suite passed 153/153. Fresh post-rebase
  `./mvnw -B clean verify` passed 500/500 tests against PostgreSQL 18.4 with
  Flyway; fresh `./mvnw -B -DexcludedGroups=database clean verify` passed
  465/465. Both reports covered 1238/1238 lines and 648/648 branches.
- Integration coverage proves deterministic claim order and limit, disjoint
  concurrent workers, restart recovery, stale-token rejection, strict privacy
  cutoff, no active transaction during the gateway call, all approved gateway
  outcomes, failed state writes, empty polls, and the accepted duplicate
  window.
- Runtime dependency inspection showed only the intended existing Spring Boot
  web/RestClient/actuator/security/JDBC/Flyway/PostgreSQL graph. TRACE capture
  exposed only `content=<redacted>`. Semgrep ran 60 Java rules over all 54
  production files with zero findings; TruffleHog found zero verified or
  unverified secrets; the whole diff and `git diff --check` were clean.
- Independent specification and quality/security reviews found no remaining
  Critical or Important issue. Two documented Minor residual risks remain:
  `reloadDeliverable` derives current time from its approved privacy-cutoff
  argument, and privacy-aged rows recovered from an expired lease remain
  unclaimable `retry` rows until the separately scoped retention task
  terminalizes them.
- This handoff, its index entry, and the tracker row are metadata changes after
  the reviewed implementation head. They require fresh exact-head verification
  before any publication decision.

## Next steps — conditional on live evidence

1. Commit only `TASKS.md`, this handoff, and its index with the required Codex
   attribution footer.
2. Re-run the full Maven gate, exact coverage totals, diff, secret, static
   analysis, and metadata review on the resulting exact local head.
3. Re-fetch `origin/main` before publication. Stop and obtain a separate
   explicit authorization before push, Draft PR creation, Ready transition,
   squash merge, or deployment.
