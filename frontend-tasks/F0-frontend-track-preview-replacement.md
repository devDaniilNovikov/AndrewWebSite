# F0 — Frontend preview track replacement

## Objective

Replace the stale frontend-track proposal with the user's approved
single-page preview scope and transfer active frontend ownership to Codex
without changing frontend, backend, CI, or production behavior.

## Ownership and prerequisites

- **Owner:** Codex.
- **Prerequisite:** fresh `origin/main`; the user selected replacement rather
  than revival of Draft PR #30.
- **Owned paths:** role routing, frontend task briefs, `TASKS.md`, frontend
  design/specification documents, the supplied design reference, decision and
  handoff metadata, and active-plan owner references.
- **Sources:** the user-approved implementation plan, [SPEC](../docs/SPEC.md),
  [Git Flow](../.agents/workflows/GIT_FLOW.md), and live Git/GitHub state.

## Tasks

1. Preserve the supplied reference and define the visual, responsive,
   placeholder, API-safety, and production-gate contract.
2. Reconcile the active roles and frontend task dependency graph around Codex,
   the one-page preview, deferred multi-page work, and a separately authorized
   Jules CI task.
3. Reconcile stale live tracker/handoff state, review the complete diff, and
   stop locally before every unapproved external GitHub stage.

## Acceptance

- No `frontend/**`, backend, workflow, branch-protection, dependency, secret,
  or production resource changes.
- Draft PR #30 remains untouched until a merged replacement and a separate
  close authorization exist.
- The next executable frontend task is F1 from fresh `origin/main`, only after
  this replacement merges.
- Link, scope, secret-pattern, specification, quality, and whole-diff checks
  pass before publication is requested.
