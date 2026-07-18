# AndrewWebSite shared agent memory handoff

## Snapshot

- `snapshot_at`: `2026-07-18T10:49:09Z`.
- `task`: `task-agents-memory`.
- `base_sha`: `288fdd5b8fdba3cfbb772babed466c5c6bcd934a`.
- `base_evidence`: live local Git showed `origin/main` and the task branch at `288fdd5` before this task's documentation changes.
- `predecessor`: [merged-branch retention handoff](2026-07-18-101804-branch-retention-handoff.md).
- `branch`: `task-agents-memory` in `/Users/daniilnovikov/.codex/worktrees/agents-memory/AndrewWorkWebSite`.
- `pull_request`: [PR #20](https://github.com/devDaniilNovikov/AndrewWebSite/pull/20), Draft; base `main`, head `task-agents-memory`.
- `pr_checks`: pre-push local documentation, link, scope, and credential checks plus independent SPEC PASS and QUALITY PASS are complete; fresh GitHub PR checks will be required after this metadata commit is pushed.

This handoff is the current episodic snapshot. It may receive focused corrections only until a successor links to it. It is not a replacement for live Git/GitHub state, reconciled `TASKS.md`, canonical docs, or the assigned plan.

## Completed scope

- Added file-first shared memory for Codex, Claude Code, and Jules under `.agents/memory/`.
- Defined semantic, episodic, and procedural memory, including the authority map, startup routing, one-controller rule, conflict metadata, handoff lifecycle, archive policy, and security boundary.
- Indexed all existing committed handoffs and set this snapshot as current.
- Added concise read/write hooks to Codex, Claude Code, Git Flow, and Jules guidance; added the repository entry link.
- Reconciled `TASKS.md` for the PR #19 merge and this task's in-progress state.
- Kept application code, backend contracts and plans, frontend, workflows, and CI/CD design outside this task.

## Current evidence and boundary

- PR #19 is represented by merge commit `288fdd5b8fdba3cfbb772babed466c5c6bcd934a` on `origin/main`.
- PR #20 is the actual Draft PR for this task; its current state and checks must still be reverified live.
- The memory merge is only the first Jules prerequisite. Jules and `task-ci-backend-gates` remain blocked through the separately owned fix PR's explicit merge, follow-on `TASKS.md` reconciliation, and separate authorized dispatch.
- `codex-mem` remains deferred, inactive, and advisory; it is neither a startup dependency nor a source of truth.
- No secrets, credentials, lead PII, raw issue text, raw transcripts, or raw tool output are included.

## Coordination after PR #20

After PR #20 merges, the separately owned `fix-backend-skeleton-review` task
may rebase and open a Draft PR. The memory flow neither starts nor merges that
fix PR; it has no automatic merge authorization. Jules and
`task-ci-backend-gates` remain blocked until the fix PR explicitly merges and
the follow-on owner reconciles `TASKS.md` from live evidence before a separate
authorized dispatch.

## Conditional next steps

1. Before marking PR #20 Ready or taking merge action, the controlling agent must recheck live `origin/main`, the task branch, PR #20, required checks, and `TASKS.md`.
2. If the task remains scoped and the fresh PR checks become green, complete the required review path under the canonical Git Flow; do not infer readiness from this snapshot.
3. Merge only after fresh green gates, Codex review, and explicit user merge authorization. A later completion or transfer must create a unique successor handoff and update `HANDOFFS.md` in the same branch.
