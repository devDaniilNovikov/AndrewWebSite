# task-telegram-client handoff

Signature: HND task-telegram-client [in_progress] topics: backend, security → predecessor: 2026-07-25-210328-task-telegram-client-handoff.md

## Durable — safe to cite later

- The formatter follow-up remains a deterministic plain-text boundary:
  user-controlled Java line-break sequences become spaces and only
  formatter-owned newlines define Telegram message fields.
- The user explicitly raised the Maven-module quality gate to 100% JaCoCo
  line and branch coverage. `pom.xml` now enforces bundle `COVEREDRATIO`
  `1.00` independently for `LINE` and `BRANCH`, without production-class
  exclusions.
- Both the complete host/CI suite and the container-equivalent suite meet the
  same 100%/100% gate. Fast mock-JDBC tests preserve full coverage when the
  Docker build excludes only the Testcontainers `database` group; the complete
  suite still runs the real PostgreSQL migration, constraint, and transaction
  tests.
- Coverage work removed two provably unreachable internal phone-normalization
  branches and made the HMAC provider boundary locally injectable for a
  deterministic failure test. Pinned digest tests preserve canonical payload
  compatibility, and Spring context tests pin the production constructor.
- Independent specification review found no Critical or Important issue and
  identified two Minor gaps. The review follow-up now pins generic non-429
  4xx classification with an explicit `404` case and makes the canonical
  architecture accurately state that only a bounded 429 body is parsed for
  `retry_after`; Telegram response bodies are never logged.
- Independent quality/security review found no actionable issue in message
  neutralization, endpoint policy, redirect/timeout behavior, credential
  redaction, PII-free telemetry, status handling, or test coverage.
- No real credential, destination, lead, Telegram call, frontend, migration,
  CI workflow, production service, push, pull request, merge, or deployment
  was used or changed.

## Snapshot at 2026-07-26T03:47:45Z — re-verify live before use

- The local unpublished branch was at
  `298e23fa2eef43c5df170bf64e90dca3ba32fe29` before this successor commit.
  Resolve the commit containing this handoff live.
- A fresh fetch confirmed `origin/main` at
  `0b0a62acfce09857807c4eb11e92795af3c20576`; the unpublished branch remained
  five commits ahead and one behind. The intervening main commit changes
  container verification and tracker/memory metadata, not Telegram runtime
  code.
- Focused Telegram verification passed 100 tests with no failure, error, or
  skip.
- `./mvnw -B clean verify` passed 447 tests; PostgreSQL 18.4 and Flyway ran
  successfully. The clean JaCoCo report covers 935/935 lines and 565/565
  branches, and the Maven coverage check reports that all rules are met.
- `./mvnw -B -DexcludedGroups=database clean verify` passed 431 tests with
  the same 935/935 lines and 565/565 branches, proving the Docker build path
  also satisfies the gate without starting sibling Testcontainers.
- Runtime dependency inspection found `spring-boot-starter-restclient` and no
  WebFlux/Reactor dependency. A Spring TRACE scan found no raw or encoded
  fictional credential, destination, message, or request-ID fixture.
- TruffleHog scanned all changed and untracked task files with zero verified
  secrets. Semgrep ran 60 Java rules over 43 tracked source targets with zero
  findings. `git diff --check` is clean.
- Final independent specification and quality/security reviews found no
  Critical or Important issue. The specification follow-up corrected the
  historical plan's stale HTML `parse_mode` wording. Quality review retained
  one documented Minor: a few provably unbindable defensive branches use
  narrow private-helper tests; public constructor, filter, MockMvc, context,
  and real-database tests cover the corresponding runtime boundaries.
- Real Telegram, CI, rebase, push, pull request, merge, and production were
  not run.

## Next steps — conditional, each requires the stated authorization

1. Re-fetch `origin` and confirm the worktree and exact local head.
2. Because current `origin/main` advanced, obtain explicit user authorization
   before rewriting the unpublished branch through a rebase. Preserve both
   active handoff rows and reconcile the tracker conflict during that rebase.
3. After an authorized rebase, rerun focused Telegram tests, both Maven
   verification profiles, coverage, dependency, TRACE redaction, whole-diff
   scans, and independent specification plus quality/security reviews against
   the new exact head.
4. Request separate authorization before push; verify the remote head exactly
   matches the local branch.
5. Request another separate authorization before opening a Draft PR. Ready,
   squash merge, and production release remain independent user-authorized
   transitions.
6. Start `task-telegram-worker` only after this task is confirmed merged from
   then-current `origin/main`.
