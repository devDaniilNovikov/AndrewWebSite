# task-backend-observability start handoff

Signature: HND task-backend-observability [in_progress] topics: backend, security, tracker

## Durable authorization

- The user explicitly authorized task start, this isolated local branch and
  worktree, reconciliation, implementation, tests, reviews, and local atomic
  commits for the canonical observability plan.
- Push, Draft PR publication, transition to Ready, merge, production
  configuration, and deployment remain separate unauthorized gates.
- Scope is dependency-aware readiness, bounded PII-free telemetry,
  production-only OTLP export, and production ECS console logging.
- OpenAPI, Flyway schema, frontend, CI workflow, public metrics exposure,
  production infrastructure, and deployment are outside this task.

## Live snapshot at 2026-07-26T13:32:09Z

- The isolated worktree is
  `/Users/daniilnovikov/.codex/worktrees/backend-observability/AndrewWorkWebSite`
  on branch `task-backend-observability`.
- Local HEAD and fresh `origin/main` matched at
  `ceefd7a05ed8c2a5d5c9fb3a19b104a156e59447`; the new worktree was clean.
- The predecessor retention task is live on `main` through merged PR #53,
  and its closed chain is recorded by
  `2026-07-26-133209-task-lead-retention-merged-handoff.md`.
- The stale root workspace and every retained task worktree remain
  untouched.

## Next steps

1. Implement readiness composition with focused RED/GREEN/REFACTOR evidence.
2. Implement the telemetry allowlist, lead metrics, production OTLP guard,
   and ECS logging with focused RED/GREEN/REFACTOR evidence.
3. Run both clean verification paths, dependency, container-equivalent,
   secret, static-analysis, specification, quality, and security checks.
4. Record the verified final local handoff and stop before push.
