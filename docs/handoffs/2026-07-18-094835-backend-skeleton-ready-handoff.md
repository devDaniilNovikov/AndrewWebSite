# AndrewWebSite backend skeleton Ready handoff

## Snapshot

- `snapshot_at`: `2026-07-18T09:48:35Z`.
- Repository: `devDaniilNovikov/AndrewWebSite`; protected default branch `main`.
- `base_sha`: `dbf4e184065a329c1708d65bed4bd5b5c1c8fe1b` (`origin/main`).
- `head_sha_at_snapshot`: `dd63326e56394b61ca573a7d091df24c3d106dfc`.
- Task: `task-backend-skeleton`; branch: `task-backend-skeleton`.
- Worktree: `/Users/daniilnovikov/.codex/worktrees/backend-skeleton/AndrewWorkWebSite`.
- Pull request: [PR #18](https://github.com/devDaniilNovikov/AndrewWebSite/pull/18), open and Ready, reported `MERGEABLE`, with auto-merge disabled.
- Previous episodic snapshot: [backend skeleton execution handoff](2026-07-18-backend-skeleton-handoff.md).

The SHA values describe the verified branch state immediately before the dedicated handoff commit. Historical handoffs remain unchanged.

## Completed task scope

- Added one root Maven module using Spring Boot 4.1.0, Java 25, Maven Wrapper 3.3.4, and Maven 3.9.16 with pinned wrapper and distribution checksums.
- Added the `ru.andrew.website` application foundation, a UTC `Clock`, and an exactly-one `test|local|prod` profile guard without a default profile.
- Added Actuator and Micrometer foundations with dependency-free liveness and readiness endpoints, minimal health bodies, and exact `Cache-Control: no-store` behavior.
- Closed every unapproved `/actuator/**` path, including the Actuator discovery and aggregate health paths, while preserving context-path handling.
- Added Spring Web MVC tests and a JaCoCo line-coverage gate of at least 80%.
- Kept leads, PostgreSQL, Flyway, Telegram, Docker, frontend, deployment, and Jules CI outside this task.
- Recorded the task as `ready` in [`TASKS.md`](../../TASKS.md). No application files changed after the final implementation verification; the Semgrep trigger commit was empty.

## Verification and review evidence

- The implementer completed RED, GREEN, REFACTOR and self-review.
- Independent specification review approved the implementation.
- Independent quality/security review found the over-broad Actuator surface; the regression-first fix was applied and the repeated review found no remaining Critical or Important issue.
- Independent whole-branch review found no Critical or Important issue.
- Controller verification used Maven 3.9.16 on Java 25: all 15 tests passed, `test` and `verify` passed, and the dependency tree remained within the foundation scope.
- JaCoCo line coverage was 94.74% (36 of 38 lines).
- Missing, unknown, and multiple profiles failed closed; liveness and readiness returned the exact minimal bodies and no-store header; unapproved Actuator routes returned 404.
- Official Maven SHA-512 and the pinned wrapper/distribution SHA-256 values were verified.
- Diff, scope, attribution, changed-text secret, and required CI checks passed on the implementation head.
- A local Semgrep 1.170.0 scan reported no findings, and the remote `Semgrep policy scan` completed successfully on `dd63326e56394b61ca573a7d091df24c3d106dfc`.

## Known non-required check state

The `Send signed PR event` check failed on the snapshot head because the pull-request relay was enabled before its public backend receiver existed. This check is not required and is unrelated to the application or Semgrep results. The user has now set `PR_WEBHOOK_ENABLED` to `false`; the next push must confirm that the relay no longer fails. Do not enable it again until the receiver, signature verification, replay protection, and rate limiting are implemented.

## Boundaries and authorization

- `task-backend-skeleton` is Ready, but Ready never grants merge permission.
- Merge authorization for PR #18 has not been given. Do not merge or enable auto-merge.
- Do not create the `task-ci-backend-gates` Jules Issue before an explicitly authorized skeleton merge reaches `main`.
- Do not create `.agents/memory` in this branch. The proposed `task-agent-memory` is a separate future task after an authorized skeleton merge.
- Do not reuse, clean, reset, or remove unrelated or dirty worktrees.
- Do not record secrets, credentials, lead PII, or raw tool output in handoffs.

## Immediate next step

Validate this documentation change, obtain an independent handoff review, commit it atomically with Codex attribution, push it to PR #18, and wait for fresh CI. Confirm the required checks and Semgrep are green and the disabled relay no longer fails, keep PR #18 Ready, then stop without merging or creating a Jules Issue.
