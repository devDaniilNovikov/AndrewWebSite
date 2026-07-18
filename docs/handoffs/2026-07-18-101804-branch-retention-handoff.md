# AndrewWebSite merged-branch retention handoff

## Snapshot

- `snapshot_at`: `2026-07-18T10:18:04Z`.
- Repository: `devDaniilNovikov/AndrewWebSite`; protected default branch `main`.
- `origin_main`: `1fb0eb14076b6f42c776cf9b0b7fa8118980ee17`.
- Backend skeleton: [PR #18](https://github.com/devDaniilNovikov/AndrewWebSite/pull/18) squash-merged at `2026-07-18T10:10:34Z` under explicit user authorization.
- Retained skeleton branch: `task-backend-skeleton` restored remotely at `626e25c57fcfccbbc4e4da6a87afb6f7e0400825`.
- GitHub repository setting `delete_branch_on_merge`: `false`, verified live after the user changed the policy.
- Current policy fix: `fix-preserve-merged-branches`, parent `1fb0eb14076b6f42c776cf9b0b7fa8118980ee17`.
- Current worktree: `/Users/daniilnovikov/.codex/worktrees/preserve-merged-branches/AndrewWorkWebSite`.
- Pull request: [PR #19](https://github.com/devDaniilNovikov/AndrewWebSite/pull/19), Ready after PR CI and Semgrep succeed on the latest head; always reverify live state.
- Previous episodic snapshot: [backend skeleton Ready handoff](2026-07-18-094835-backend-skeleton-ready-handoff.md).

This file supersedes the previous handoff, which is now historical and immutable. Always reverify the latest branches, pull requests, repository settings, and checks live before acting.

## Completed backend skeleton state

- The one-module Spring Boot 4.1.0 and Java 25 foundation is on `main`.
- PR #18 required checks and Semgrep were successful before merge.
- The post-merge `main` CI and full Semgrep push scan completed successfully.
- `Jules - CI failure` was `SKIPPED` because the post-merge CI succeeded; Jules did not run a repair session.
- The existing `Application checks (pending scaffold)` job is still a placeholder and does not run Maven. Adding real Maven, coverage, dependency, and security gates remains the separate `task-ci-backend-gates` Jules task.

## Settled branch-retention decision

- From this policy decision onward, local and remote `task-*` and `fix-*` branches are durable execution history and must not be deleted after merge, closure, or supersession.
- GitHub's **Automatically delete head branches** setting must remain disabled.
- Completed branches are read-only history and must never be reused as the base for new work.
- Worktree lifecycle is separate from branch retention. A worktree may be removed only when it is clean and no longer needed; removing it must preserve its local and remote branch.
- If a retained branch disappears, stop, restore it from a verified local or pull request head, and report the incident.
- Any future exception requires an explicit user decision and a merged update to the canonical Git Flow before deletion.

Only `task-backend-skeleton` was explicitly restored in this focused task. Older branches deleted before this policy are not represented as live remote refs and require a separate explicit restoration decision if they are also needed remotely.

The skeleton worktree remains preserved at `/Users/daniilnovikov/.codex/worktrees/backend-skeleton/AndrewWorkWebSite`. Its generated Maven `target/` artifacts were removed with `./mvnw -B clean`; it was not force-removed and must not be deleted as part of this policy fix.

## Known metadata note

The squash commit on `main` contains the intended Codex attribution as text, but the merge command stored literal newline escape characters, so Git does not parse it as a trailer. Every source commit retained in PR #18 has a valid AI attribution footer. Do not rewrite or revert published `main` for this metadata-only issue; any different remediation requires a separate explicit user decision.

## Current task boundary

- Owned paths: `.agents/workflows/GIT_FLOW.md`, `.agents/AGENTS.md`, `TASKS.md`, and this handoff.
- No application, workflow, secret, Jules, or dependency change belongs to this task.
- The GitHub setting change and restored skeleton branch are already complete external-state corrections authorized by the user.
- `task-ci-backend-gates` remains blocked by this non-stacked policy PR and requires a separate explicit dispatch instruction after this PR merges.
- The user explicitly authorized squash-merging PR #19 only after its fresh PR gates are green, followed by fresh post-merge CI/CD verification on `main`.
- Before merge, confirm the ready metadata commit itself has fresh green PR CI/Semgrep; the PR relay may remain skipped while `PR_WEBHOOK_ENABLED=false`.
- Preserve both `fix-preserve-merged-branches` and `task-backend-skeleton`; neither the merge nor worktree cleanup authorizes branch deletion.
- Do not create the Jules Issue as part of this policy task.

## Immediate next step

Read the live `fix-preserve-merged-branches` branch and PR #19 state. After the ready metadata commit has fresh green PR checks, perform the authorized squash merge, verify the merge commit and preserved remote branches, then wait for and verify fresh post-merge CI/CD on `main`. Do not create a Jules Issue. If PR #19 is already merged, do not repeat the merge; reconcile `main` and preserve both source branches.
