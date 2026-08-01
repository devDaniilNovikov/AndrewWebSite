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
DEC-20260721-008 [active] security: GitHub-native dependency controls replace ODC → architecture
DEC-20260801-011 [active] frontend: Codex owns the single-page preview; multi-page delivery is deferred → user
```

## Records

## DEC-20260718-001 — Project-wide shared memory

`DEC-20260718-001 [active] memory: all active agents share one file-first memory surface → user brief`

- **Date:** 2026-07-18
- **Scope:** all active project agents; the current roster is routed by
  [`../AGENTS.md`](../AGENTS.md).
- **Decision:** all active project agents share the same file-first memory
  surface.
- **Rationale:** shared, versioned context reduces repeated discovery without
  changing role ownership or canonical sources.
- **Evidence:** user-confirmed shared-memory task brief; participant routing
  was historically revalidated by DEC-20260726-009 and is currently routed by
  DEC-20260801-011.
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

## DEC-20260721-008 — GitHub-native dependency security

`DEC-20260721-008 [active] security: GitHub-native dependency controls replace ODC → architecture`

- **Date:** 2026-07-21
- **Scope:** repository dependency-vulnerability detection and remediation.
- **Decision:** replace the ODC/NVD required gate with the GitHub-native
  dependency controls defined by the canonical architecture; keep Snyk as an
  informational external signal.
- **Rationale:** repeated cold-data timeouts and cache-provenance risks made
  ODC unreliable as a required pull-request gate, while the approved
  GitHub-native design separates pull-request dependency review from
  continuous default-branch monitoring.
- **Evidence:** explicit user selection of option 1, live enablement of
  Dependabot alerts and security updates, and
  [`2026-07-21-081833-task-dependency-security-github-native-handoff.md`](../../docs/handoffs/2026-07-21-081833-task-dependency-security-github-native-handoff.md).
- **Canonical source:** [backend architecture](../../docs/backend/architecture.md).
- **Supersedes:** the ODC/NVD dependency-gate design in the original backend
  foundation plan; historical incident records remain valid.
- **Review-by:** any change to dependency-security semantics, required check
  names, Dependabot settings, or branch protection.

## DEC-20260801-011 — Codex-owned single-page frontend preview

`DEC-20260801-011 [active] frontend: Codex owns the single-page preview; multi-page delivery is deferred → user`

- **Date:** 2026-08-01
- **Scope:** active frontend ownership and the first frontend delivery slice.
- **Decision:** Codex owns the replacement, foundation, landing-page, lead
  form, and preview-hardening tasks. The first delivery exports only `/` with
  explicit non-production placeholders and a production gate. Multi-page
  product routes and verified trust content are deferred follow-up work, not
  prerequisites for the preview. Jules retains separately authorized CI
  ownership; Google Antigravity has no active repository ownership.
- **Rationale:** the user selected a fast, reviewable preview based on the
  supplied low-resolution landing-page reference, chose Codex as implementer,
  and explicitly rejected reviving stale Draft PR #30.
- **Evidence:** explicit user decisions on 2026-08-01 and the canonical
  [preview contract](../../docs/frontend/landing-preview.md).
- **Canonical source:** [TASKS](../../TASKS.md),
  [`../AGENTS.md`](../AGENTS.md), and
  [preview contract](../../docs/frontend/landing-preview.md).
- **Supersedes:** DEC-20260719-007 as the active frontend split,
  DEC-20260726-009 as active frontend ownership, and DEC-20260726-010 as the
  model requirement for an active frontend owner. Their historical evidence
  remains preserved in the [decision archive](archive/DECISIONS-20260719-007-20260726-009-010.md).
- **Review-by:** any frontend scope, owner, preview/production boundary, or
  agent-routing change.

## Entry rules

A record = signature line first, then Date, Scope, Decision, Rationale,
Evidence, Canonical source, Supersedes, Review-by. Supersede, never delete;
superseded bodies move to [`archive/`](archive/) once the replacement links
their ID. Signatures also live in `## Active`, tagged from the README topic
vocabulary.
