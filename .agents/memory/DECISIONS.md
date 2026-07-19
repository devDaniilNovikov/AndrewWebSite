# Decisions

Settled, reviewed choices. Startup reads `## Active` only; bodies are cold.
Signature format and status vocabulary: [`README.md`](README.md).

## Active

```text
DEC-20260718-001 [active] memory: all active agents share one file-first memory surface → user brief
DEC-20260718-002 [active] memory: docs/handoffs is the episodic base indexed by HANDOFFS.md → history
DEC-20260718-003 [active] process: live Git/GitHub, then reconciled TASKS.md, outrank memory snapshots → user routing
DEC-20260718-004 [active] memory: file-first implementation; codex-mem deferred, inactive, advisory → user brief
DEC-20260718-005 [active] process: merged branches stay retained read-only; worktree removal never deletes → 288fdd5b
DEC-20260719-006 [active] memory: product is an allowed topic tag for product-source records → user decision
```

## Records

## DEC-20260718-001 — Project-wide shared memory

`DEC-20260718-001 [active] memory: all active agents share one file-first memory surface → user brief`

- **Date:** 2026-07-18
- **Scope:** Codex, Claude Code, and Jules
- **Decision:** all active project agents share the same file-first memory
  surface.
- **Rationale:** shared, versioned context reduces repeated discovery without
  changing role ownership or canonical sources.
- **Evidence:** user-confirmed shared-memory task brief.
- **Canonical source:** [README](README.md)
- **Supersedes:** none
- **Review-by:** any change to memory ownership or cross-owner write rules.

## DEC-20260718-002 — Committed handoff base and index

`DEC-20260718-002 [active] memory: docs/handoffs is the episodic base indexed by HANDOFFS.md → history`

- **Date:** 2026-07-18
- **Scope:** episodic task context
- **Decision:** committed files in
  [`../../docs/handoffs/`](../../docs/handoffs/) are the episodic memory
  base, indexed by `HANDOFFS.md`.
- **Rationale:** handoffs preserve transfer evidence and task boundaries in
  the repository without claiming live authority.
- **Evidence:** user-confirmed handoff lifecycle and existing committed
  history.
- **Canonical source:** [HANDOFFS](HANDOFFS.md)
- **Supersedes:** informal unindexed handoff discovery
- **Review-by:** any change to the handoff lifecycle.

## DEC-20260718-003 — Live authority and anti-duplication

`DEC-20260718-003 [active] process: live Git/GitHub, then reconciled TASKS.md, outrank memory snapshots → user routing`

- **Date:** 2026-07-18
- **Scope:** startup context and conflict handling
- **Decision:** live Git/GitHub is authoritative for mutable state, followed
  by reconciled `TASKS.md`; memory records are snapshots and do not
  duplicate the living task queue.
- **Rationale:** a retained snapshot cannot safely answer current branch,
  PR, check, or queue status.
- **Evidence:** user-confirmed startup routing and task-tracker
  clarification.
- **Canonical source:** [README](README.md), [`../../TASKS.md`](../../TASKS.md)
- **Supersedes:** any inference that a handoff is current truth
- **Review-by:** any change to startup routing in role files; on source
  conflict, preserve the conflict with status, supersession, and evidence
  metadata rather than silently overwriting.

## DEC-20260718-004 — File-first memory; `codex-mem` deferred

`DEC-20260718-004 [active] memory: file-first implementation; codex-mem deferred, inactive, advisory → user brief`

- **Date:** 2026-07-18
- **Scope:** memory implementation
- **Decision:** the first implementation uses committed files; `codex-mem`
  is deferred, inactive, and advisory.
- **Rationale:** the repository already offers reviewable, durable storage
  and no separate memory runtime is authorized.
- **Evidence:** user-confirmed task brief.
- **Canonical source:** [README](README.md)
- **Supersedes:** none
- **Review-by:** an explicit user decision to pilot a memory runtime, as a
  scoped and reviewed task.

## DEC-20260718-005 — Retained execution-branch reference

`DEC-20260718-005 [active] process: merged branches stay retained read-only; worktree removal never deletes → 288fdd5b`

- **Date:** 2026-07-18
- **Scope:** branch and worktree history
- **Decision:** completed `task-*` and `fix-*` branches remain retained,
  read-only execution history; worktree removal never authorizes branch
  deletion, and GitHub "Automatically delete head branches" stays disabled
  and verified.
- **Rationale:** retention preserves auditability and recovery paths; a
  platform setting silently violated the documented policy once already.
- **Evidence:**
  [branch-retention handoff](../../docs/handoffs/2026-07-18-101804-branch-retention-handoff.md),
  merge commit `288fdd5b8fdba3cfbb772babed466c5c6bcd934a`.
- **Canonical source:** [`../workflows/GIT_FLOW.md`](../workflows/GIT_FLOW.md)
- **Supersedes:** prior delete-after-merge behavior
- **Review-by:** any exception requires an explicit user decision and a
  merged Git Flow update; re-verify on any repository-settings change.

## DEC-20260719-006 — Product topic in the closed vocabulary

`DEC-20260719-006 [active] memory: product is an allowed topic tag for product-source records → user decision`

- **Date:** 2026-07-19
- **Scope:** semantic-memory topic vocabulary
- **Decision:** `product` is an allowed topic tag for records about product
  requirements and their canonical sources.
- **Rationale:** Q-20260718-003 concerns the sanitized product brief and
  cannot be classified accurately under the previously listed tags.
- **Evidence:** explicit user authorization during `task-context-refactor`.
- **Canonical source:** [README](README.md)
- **Supersedes:** the prior closed vocabulary without `product`.
- **Review-by:** any further change to the closed topic vocabulary.

## Entry rules

A record = signature line first, then Date, Scope, Decision, Rationale,
Evidence, Canonical source, Supersedes, Review-by. Supersede, never delete;
superseded bodies move to [`archive/`](archive/) once the replacement links
their ID. Signatures also live in `## Active`, tagged from the README topic
vocabulary.
