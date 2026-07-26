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
DEC-20260719-007 [active] tracker: frontend delivery uses six tasks F1-F6 assigned by the user → user
DEC-20260721-008 [active] security: GitHub-native dependency controls replace ODC → architecture
DEC-20260726-009 [active] frontend: Antigravity owns F1-F6; Claude retired → user
DEC-20260726-010 [active] frontend: Antigravity requires Gemini 3.1 Pro High → user
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
  revalidated by DEC-20260726-009.
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

## DEC-20260719-007 — Six-task frontend delivery track

`DEC-20260719-007 [active] tracker: frontend delivery uses six tasks F1-F6 assigned by the user → user`

- **Date:** 2026-07-19
- **Scope:** frontend delivery planning after the canonical product brief.
- **Decision:** use six atomic tasks for a user-assigned frontend agent:
  foundation, shared shell, product pages, trust content, lead form, and
  final quality hardening. Every task starts from fresh `main` after its
  declared prerequisites; no stacked PR is allowed.
- **Rationale:** the split keeps framework setup, verified content, API
  integration, and final cross-cutting quality review independently
  reviewable while leaving the concrete frontend-agent assignment to the
  user.
- **Evidence:** the user's explicit F1-F6 choice and T5 authorization.
- **Canonical source:** [TASKS](../../TASKS.md).
- **Supersedes:** the unplanned frontend placeholder represented by
  Q-20260718-004.
- **Review-by:** any frontend task split, dependency, or ownership change.

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

## DEC-20260726-009 — Antigravity frontend ownership

`DEC-20260726-009 [active] frontend: Antigravity owns F1-F6; Claude retired → user`

- **Date:** 2026-07-26
- **Scope:** active frontend-agent identity and F1-F6 ownership.
- **Decision:** Google Antigravity owns frontend execution and Claude Code is
  retired from the active roster.
- **Superseded model clause:** this record originally set Gemini 3.6 Flash
  High as the default and reserved Gemini 3.1 Pro High for selected turns.
  DEC-20260726-010 makes that clause inactive while preserving this ownership
  decision.
- **Rationale:** Antigravity is the user-selected implementer and needs a
  native `GEMINI.md` entry plus a role that cannot conflict with Codex
  identity.
- **Evidence:** explicit user decision on 2026-07-26 and the reviewed
  [`ANTIGRAVITY.md`](../ANTIGRAVITY.md) role.
- **Canonical source:** [TASKS](../../TASKS.md) and
  [`ANTIGRAVITY.md`](../ANTIGRAVITY.md).
- **Supersedes:** current role text that assigned frontend implementation to
  Claude Code; historical handoffs remain immutable.
- **Review-by:** any frontend-owner, model-routing, or agent-harness change.

## DEC-20260726-010 — Required Antigravity model

`DEC-20260726-010 [active] frontend: Antigravity requires Gemini 3.1 Pro High → user`

- **Date:** 2026-07-26
- **Scope:** model routing for every assigned Antigravity frontend session.
- **Decision:** use Gemini 3.1 Pro High for frontend planning,
  implementation, diagnosis, self-review, and local completion. If that model
  is not active or cannot be confirmed, stop before repository edits; do not
  silently fall back to another model.
- **Rationale:** the user selected the higher-reasoning model and requested
  that it become durable project context rather than a one-off prompt choice.
- **Evidence:** explicit user decision on 2026-07-26, root
  [`GEMINI.md`](../../GEMINI.md), and
  [`ANTIGRAVITY.md`](../ANTIGRAVITY.md).
- **Canonical source:** [`ANTIGRAVITY.md`](../ANTIGRAVITY.md).
- **Supersedes:** only the Gemini 3.6 Flash default / limited Gemini 3.1 Pro
  allocation previously recorded in DEC-20260726-009; its Antigravity
  ownership and Claude retirement decisions remain active.
- **Review-by:** any frontend-model, Antigravity harness, or model-availability
  change.

## Entry rules

A record = signature line first, then Date, Scope, Decision, Rationale,
Evidence, Canonical source, Supersedes, Review-by. Supersede, never delete;
superseded bodies move to [`archive/`](archive/) once the replacement links
their ID. Signatures also live in `## Active`, tagged from the README topic
vocabulary.
