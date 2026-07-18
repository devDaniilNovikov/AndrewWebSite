# Shared-memory decisions

## DEC-20260718-001 — Project-wide shared memory

- **Status:** active
- **Date:** 2026-07-18
- **Scope:** Codex, Claude Code, and Jules
- **Decision:** All active project agents share the same file-first memory surface.
- **Rationale:** Shared, versioned context reduces repeated discovery without changing role ownership or canonical sources.
- **Evidence:** User-confirmed shared-memory task brief.
- **Canonical source:** [README](README.md)
- **Supersedes:** none
- **Review:** Codex final review is required for cross-owner changes.

## DEC-20260718-002 — Committed handoff base and index

- **Status:** active
- **Date:** 2026-07-18
- **Scope:** episodic task context
- **Decision:** Committed files in [`../../docs/handoffs/`](../../docs/handoffs/) are the episodic memory base, indexed by `HANDOFFS.md`.
- **Rationale:** Handoffs preserve transfer evidence and task boundaries in the repository without claiming live authority.
- **Evidence:** User-confirmed handoff lifecycle and existing committed history.
- **Canonical source:** [HANDOFFS](HANDOFFS.md)
- **Supersedes:** informal unindexed handoff discovery
- **Review:** A successor link makes the predecessor immutable.

## DEC-20260718-003 — Live authority and anti-duplication

- **Status:** active
- **Date:** 2026-07-18
- **Scope:** startup context and conflict handling
- **Decision:** Live Git/GitHub is authoritative for mutable state, followed by reconciled `TASKS.md`; memory records are snapshots and do not duplicate the living task queue.
- **Rationale:** A retained snapshot cannot safely answer current branch, PR, check, or queue status.
- **Evidence:** User-confirmed startup routing and task-tracker clarification.
- **Canonical source:** [README](README.md), [`../../TASKS.md`](../../TASKS.md)
- **Supersedes:** any inference that a handoff is current truth
- **Review:** Preserve conflicts with status, supersedes, evidence, and review metadata rather than silently overwriting them.

## DEC-20260718-004 — File-first memory; `codex-mem` deferred

- **Status:** active
- **Date:** 2026-07-18
- **Scope:** memory implementation
- **Decision:** The first implementation uses committed files. `codex-mem` is deferred, inactive, and advisory.
- **Rationale:** The repository already offers reviewable, durable storage and no separate memory runtime is authorized.
- **Evidence:** User-confirmed task brief.
- **Canonical source:** [README](README.md)
- **Supersedes:** none
- **Review:** A future pilot requires an explicit user decision and scoped, reviewed task.

## DEC-20260718-005 — Retained execution-branch reference

- **Status:** active
- **Date:** 2026-07-18
- **Scope:** branch and worktree history
- **Decision:** Completed `task-*` and `fix-*` branches remain retained, read-only execution history; worktree removal never authorizes deletion.
- **Rationale:** Retention preserves auditability and recovery paths.
- **Evidence:** [branch-retention handoff](../../docs/handoffs/2026-07-18-101804-branch-retention-handoff.md) and merge commit `288fdd5b8fdba3cfbb772babed466c5c6bcd934a`.
- **Canonical source:** [`../workflows/GIT_FLOW.md`](../workflows/GIT_FLOW.md)
- **Supersedes:** prior delete-after-merge behavior
- **Review:** Any exception requires an explicit user decision and a merged Git Flow update.
