# Backend skeleton review-fix Draft PR handoff

## Snapshot

- `snapshot_at`: `2026-07-18T13:11:42Z`.
- `task`: `fix-backend-skeleton-review`.
- `base_sha`: `735a5a75f35a410a85472d3e6e9202cc8d006f55`.
- `base_evidence`: PR #20 is merged and its merge commit is the fresh `origin/main` and merge base for this branch.
- `predecessor`: [shared agent memory handoff](2026-07-18-104909-task-agents-memory-handoff.md).
- `branch`: `fix-backend-skeleton-review` in `/Users/daniilnovikov/.codex/worktrees/backend-skeleton-review-fix/AndrewWorkWebSite`.
- `pull_request`: [PR #21](https://github.com/devDaniilNovikov/AndrewWebSite/pull/21), Draft; base `main`, head `fix-backend-skeleton-review`.
- `implementation_head_before_handoff`: `25d698d4aea156d853d61030e0b503fa9b218d60`.
- `pr_checks`: the current application job passed; the current repository-policy and Semgrep jobs were running when this snapshot was created. This handoff commit will start fresh checks, so live GitHub state must be reverified.

This handoff records a Draft-PR pause. It does not replace live Git/GitHub state, reconciled `TASKS.md`, canonical contracts, ownership, or an explicit current user decision.

## Completed scope

- Rebasing the three unpublished review-fix commits onto PR #20 merge commit `735a5a7` preserved both the memory task and backend review-fix intent; the only conflict was an unambiguous `TASKS.md` reconciliation.
- Hardened the exact public liveness and readiness endpoints against non-GET methods, matrix parameters, encoded path aliases, and malformed path candidates.
- Preserved dependency-free liveness, minimal readiness output, and `Cache-Control: no-store` on the two approved health responses.
- Added regression coverage for actuator aliases, methods, malformed paths, cache headers, and local/test/prod runtime profiles.
- Added Maven `target/` ignores and reconciled the task tracker with PR #20 and PR #21.
- Pushed the branch and opened PR #21 as Draft with no merge authorization.

## Fresh evidence

- `./mvnw -B verify`: passed with 32 tests and no failures or errors.
- JaCoCo: 52 of 54 lines covered (96.30%) and 31 of 34 branches covered (91.18%); the configured gate passed.
- Runtime smoke: exact GET liveness/readiness returned `200`, minimal `{"status":"UP"}`, and `Cache-Control: no-store`; HEAD, OPTIONS, POST, matrix, encoded, and out-of-context aliases were closed. Tomcat rejected malformed percent encodings with `400` before application dispatch.
- `./mvnw -B dependency:tree -Dscope=runtime`: passed and showed only the intended Spring Boot web, validation, actuator, Micrometer, and transitive runtime dependencies.
- `git diff --check`, task-table shape, GitHub link validation, and tracked credential-pattern scans passed.
- Independent post-rebase specification, runtime, and security reviews reported no actionable findings.
- PR #20 merge commit `735a5a7` is reachable from `origin/main`; its post-merge `main` CI and Semgrep runs passed.

## Scope and coordination boundary

- PR #21 is Draft. Do not mark it Ready or merge it from this snapshot alone; refresh the PR head, required checks, review state, and `TASKS.md` first.
- The user has not authorized merging PR #21. A green CI state alone does not grant merge authorization.
- Do not dispatch Jules. `task-ci-backend-gates` remains blocked until PR #21 is explicitly merged, a follow-on controller reconciles `TASKS.md` from live state, and the user separately authorizes Jules dispatch.
- No secrets, credentials, lead PII, production configuration, frontend changes, workflow changes, or dependency changes are included.

## Conditional next steps

1. Reverify that PR #21 still targets `main`, remains Draft, and points to the latest remote branch head.
2. Wait for the fresh PR CI and Semgrep runs triggered by the handoff commit; investigate any failure before changing readiness.
3. If all required checks and reviews are fresh and green, update the tracker and handoff state before marking the PR Ready.
4. Merge only after separate explicit user authorization. After an authorized merge, create a unique successor handoff, reconcile `TASKS.md`, and keep Jules blocked until separately authorized.
