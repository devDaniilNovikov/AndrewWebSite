# AndrewWebSite backend skeleton Ready handoff

## Snapshot

- `snapshot_at`: `2026-07-18T09:56:41Z` (the verified post-CI Ready state before this focused handoff correction).
- Repository: `devDaniilNovikov/AndrewWebSite`; protected default branch `main`.
- `base_sha`: `dbf4e184065a329c1708d65bed4bd5b5c1c8fe1b` (`origin/main`).
- `application_verified_head`: `9bd3a61bc35575ca5ace6ac849ee255fcbeba0d9` (the last commit that changed application code).
- `handoff_parent_head`: `bcbc1b19d610bb6b2f6243d88917ec8112a1bb71` (the committed branch head immediately before this focused correction).
- `merge_authorization_recorded_at`: `2026-07-18T10:05:40Z`; the user explicitly authorized the PR #18 squash merge after fresh green gates.
- Task: `task-backend-skeleton`; branch: `task-backend-skeleton`.
- Worktree: `/Users/daniilnovikov/.codex/worktrees/backend-skeleton/AndrewWorkWebSite`.
- Pull request: [PR #18](https://github.com/devDaniilNovikov/AndrewWebSite/pull/18), open and Ready after successful checks, reported `CLEAN` and `MERGEABLE`, with auto-merge disabled.
- Previous episodic snapshot: [backend skeleton execution handoff](2026-07-18-backend-skeleton-handoff.md).

The pinned SHAs provide non-self-referential evidence: `application_verified_head` identifies the reviewed application state, while `handoff_parent_head` identifies the parent of this documentation correction. They do not attempt to predict the correction commit SHA. Always re-read the latest branch, pull request, and checks from live Git/GitHub before acting; live state takes precedence over this snapshot.

This file is the current handoff and may receive focused corrections while it remains current. Once a later handoff links to it as its predecessor, it becomes historical and must not be edited.

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
- Diff, scope, attribution, changed-text secret, and required CI checks passed on the verified application state.
- Required `Repository policy` and `Application checks (pending scaffold)` checks completed successfully on `handoff_parent_head`.
- A local Semgrep 1.170.0 scan reported no findings. The remote `Semgrep policy scan` also completed successfully on `handoff_parent_head`, reporting 0 findings and 0 blocking findings.

## Non-required relay state

The historical `Send signed PR event` failure occurred because the pull-request relay was enabled before its public backend receiver existed. The user then set `PR_WEBHOOK_ENABLED` to `false`; subsequent push and Ready events confirmed the relay check as `SKIPPED`. The prior failure is resolved, was never a Semgrep or application failure, and is not a required check. Keep the relay disabled until the receiver, signature verification, replay protection, and rate limiting are implemented.

## Boundaries and authorization

- `task-backend-skeleton` is Ready, but Ready never grants merge permission.
- PR #18 now has explicit authorization for a manual squash merge only after the latest head passes fresh checks and review. Auto-merge remains forbidden.
- Do not create the `task-ci-backend-gates` Jules Issue before an explicitly authorized skeleton merge reaches `main`.
- Do not create `.agents/memory` in this branch. The proposed `task-agent-memory` is a separate future task after an authorized skeleton merge.
- Do not reuse, clean, reset, or remove unrelated or dirty worktrees.
- Do not record secrets, credentials, lead PII, or raw tool output in handoffs.

## Immediate next step

Follow only the authorized merge path and begin by rereading the latest branch and PR live. If PR #18 is still open, do not repeat implementation or handoff work: wait for the latest head's fresh checks and review, require the PR to be Ready, `CLEAN`, and `MERGEABLE`, require the required checks and Semgrep to be successful, keep the relay disabled and auto-merge off, then squash-merge using the approved Conventional Commit PR title. If PR #18 is already merged, do not attempt another merge.

After the merge, verify the resulting commit on `origin/main`, confirm remote source-branch deletion, and remove only the clean skeleton worktree according to the canonical Git Flow. The `task-ci-backend-gates` Jules Issue and the separate `task-agent-memory` remain forbidden until the authorized skeleton merge is verified on `main`; neither is automatically authorized by the merge.
