# task-backend-observability verified local implementation handoff

Signature: HND task-backend-observability [in_progress] topics: backend, security, telemetry, tracker → predecessor: 2026-07-26-133209-task-backend-observability-handoff.md

## Durable authorization and status

- The local implementation is complete and verified in the isolated
  `task-backend-observability` worktree. Runtime commits are `1e6c2af`
  (readiness), `9d426f2` (lead telemetry), and `f86f791` (OTLP, bounded
  telemetry, and ECS startup/logging privacy).
- Task state remains `in_progress` because push, Draft PR publication, Ready,
  merge, production configuration, and deployment are separate unauthorized
  gates. No remote mutation was performed.
- The branch remains based directly on merged `origin/main`
  `ceefd7a05ed8c2a5d5c9fb3a19b104a156e59447`. The stale root workspace and
  retained task worktrees were not modified.

## Implementation

- Readiness contains only Spring readiness state, bounded PostgreSQL
  `dbReadiness`, and startup-grace/freshness-aware
  `telegramWorkerReadiness`. Liveness remains dependency-free, retention and
  OTLP are excluded, generic database health is disabled, and public health
  responses remain status-only with exact `Cache-Control: no-store`.
- Lead acceptance and every canonical rejection path now record only bounded
  outcome/reason values. A final central meter filter denies every
  non-canonical meter name, tag key, and tag value, including arbitrary
  framework and user-controlled telemetry.
- OTLP is disabled outside production. Production startup fails generically
  unless the exporter, HTTPS URL, single safe Authorization header, interval,
  resource attributes, and logging boundary exactly match the contract.
  Runtime export failures cannot affect health, lead acceptance, delivery, or
  retention.
- Production console output is ECS and permits only fixed generic failure
  messages. Both pre-context and post-context startup failures suppress
  configuration, URL, credential, exception, stack, request, and domain
  details. Repeated profile arguments fail closed.

## Verified evidence at 2026-07-26T15:41:14Z

- Focused RED reproduced the repeated-profile startup leak; the focused GREEN
  unit/subprocess gate passed 7 tests after the minimal fix.
- Fresh `./mvnw -B clean verify` passed 631 tests with PostgreSQL 18.4 and
  Flyway. Fresh
  `./mvnw -B -DexcludedGroups=database clean verify` passed 587 tests and
  exercises the exact container-build verification command.
- JaCoCo covered 1682/1682 lines and 822/822 branches across 80 classes.
- The freshly packaged executable JAR was probed with repeated `test`, `prod`
  arguments and an early credential-shaped config import failure: it exited
  nonzero with exactly one valid generic ECS event and no private or exception
  marker.
- In-memory OTLP protobuf capture proved that only canonical meters and tags
  are exported and that authorization remains in the request header. Logging
  capture covered fictional lead, request, Telegram, database, and OTLP data.
- Dependency inspection confirmed Boot-managed
  `micrometer-registry-otlp` 1.17.0,
  `spring-boot-opentelemetry` 4.1.0, and the expected OpenTelemetry 1.62.0
  runtime graph.
- Semgrep Java/security policies found zero issues in all 17 changed
  production Java files. TruffleHog found zero results in all 25 changed
  production files; its four broad changed-file signals were confined to
  intentional credential-shaped negative-test fixtures.
- `git diff --check` passed. Independent specification, correctness, and
  security reviews found no remaining Critical or Important issue after the
  repeated-profile fix.
- The full branch diff changes no OpenAPI, Flyway migration, frontend, CI
  workflow, Dockerfile, production infrastructure, or deployment file.

## Next steps

1. Re-fetch and reconcile `origin/main` before any publication decision.
2. Obtain explicit authorization before push or Draft PR creation.
3. If publication is authorized, prove local, remote, and PR head equality,
   wait for exact-head CI/security checks, inspect the final diff and reviews,
   and fix every in-scope finding before requesting a separate Ready gate.
4. Stop before merge, production configuration, deployment, or worktree
   deletion; each remains independently authorized.
