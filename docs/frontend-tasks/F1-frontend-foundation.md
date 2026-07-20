# F1 — Frontend foundation

## Objective

Create the production frontend workspace and a tested static-export baseline
that later frontend tasks can extend without changing backend, CI, or product
contracts.

## Ownership and prerequisites

- **Owner:** Claude Code.
- **Prerequisite:** `fix-frontend-track-gaps` merged; start from fresh
  `origin/main` in a dedicated `task-frontend-foundation` worktree and
  non-stacked PR.
- **Owned paths:** `frontend/**` and the task-specific handoff. Coordinate
  tracker and shared-memory changes with Codex.
- **Sources:** [SPEC](../SPEC.md),
  [architecture](../backend/architecture.md), and
  [Git Flow](../../.agents/workflows/GIT_FLOW.md).

## Tasks

1. Scaffold the architecture-approved frontend stack under `frontend/` with
   its manifest, lockfile, strict type checking, styling configuration, and
   static-export command. Include the Playwright runner configuration needed
   by later tasks. Do not add unapproved frameworks or dependencies.
2. Add the minimum design-token, global-style, root-layout, test, and
   accessibility-test foundations required by later tasks, plus a minimal
   Playwright smoke test with no product-content assertions. Keep all business
   content explicitly placeholder-free or clearly marked as non-production.
3. Prove a deterministic production export to the architecture-defined output
   path and document only commands that actually run.

## Acceptance

- A clean checkout can install with the committed lockfile, format, lint,
  typecheck, test, run the Playwright smoke, and produce the static export.
- No backend, API contract, CI, deployment, analytics, or production-content
  behavior changes.
- Dependency audit, secret scan, whole-diff review, required CI, and Codex
  review pass before Ready.
- The PR stops at Ready; merge requires a separate explicit user command and
  the source branch is retained.
