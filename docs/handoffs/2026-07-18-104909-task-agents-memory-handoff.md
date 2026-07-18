# AndrewWebSite shared agent memory handoff

## Snapshot

- `snapshot_at`: `2026-07-18T10:49:09Z`.
- `task`: `task-agents-memory`.
- `base_sha`: `288fdd5b8fdba3cfbb772babed466c5c6bcd934a`.
- `base_evidence`: live local Git showed `origin/main` and the task branch at `288fdd5` before this task's documentation changes.
- `predecessor`: [merged-branch retention handoff](2026-07-18-101804-branch-retention-handoff.md).
- `branch`: `task-agents-memory` in `/Users/daniilnovikov/.codex/worktrees/agents-memory/AndrewWorkWebSite`.
- `planned_pull_request`: PR #20; its existence, head, checks, and merge state require live Git/GitHub verification before any action.

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
- The next Jules CI-gates task remains blocked until this memory task is merged and is separately dispatched by the authorized flow.
- `codex-mem` remains deferred, inactive, and advisory; it is neither a startup dependency nor a source of truth.
- No secrets, credentials, lead PII, raw issue text, raw transcripts, or raw tool output are included.

## Conditional next steps

1. Before any push or PR action, the controlling agent must recheck live `origin/main`, the task branch, PR #20, required checks, and `TASKS.md`.
2. If the task branch remains scoped and all documentation checks are green, create or update only the planned PR #20 according to the canonical Git Flow; do not infer its state from this snapshot.
3. Merge only after fresh green gates, Codex review, and explicit user merge authorization. A later completion or transfer must create a unique successor handoff and update `HANDOFFS.md` in the same branch.
