# HND fix-frontend-track-preview-replacement [in_progress] topics: frontend, product, tracker, memory → predecessor: none

## State

- **Task:** `fix-frontend-track-preview-replacement`
- **Branch:** `fix-frontend-track-preview-replacement`
- **Worktree:** `/Users/daniilnovikov/.codex/worktrees/frontend-track-preview-replacement/AndrewWorkWebSite`
- **Base:** fresh `origin/main` at `90698e18d6ce602a0e09cbc870acc4fd80f16f83`
- **External stages:** none authorized; no push, Draft PR, Ready transition,
  merge, deployment, or PR #30 closure happened.

## Durable decisions

- The first frontend delivery is a Codex-owned single-page preview exported
  at `/`, plus a real static 404.
- The supplied screenshot is preserved as
  `docs/frontend/reference/landing-ui-2026-08-01.jpg` with SHA-256
  `d79cae4ea8647b6a4f81975debd3ce96b229b468b6f9a37f1a4a6184d1e8af35`.
- The screenshot is a visual reference only. Its visible phone, prices,
  cases, review, hours, logo, and wording are not production facts.
- Multi-page frontend routes and verified trust-content population are
  deferred follow-up work, not prerequisites for the preview.
- Hosted preview builds cannot collect leads. Production builds must use only
  same-origin `/api/leads` and fail while required real content or legal text
  is missing.
- Draft PR #30 remains open and untouched. It may be closed as `superseded`
  only after this replacement delivers the outcome and the user separately
  authorizes closure.

## Current local change set

- Role routing now assigns the approved preview track to Codex and marks
  Antigravity entries inactive compatibility surfaces.
- `docs/frontend/landing-preview.md` records visual, responsive,
  accessibility, placeholder, API-safety, and production-gate requirements.
- Frontend task briefs were reshaped into F0 replacement, F1 foundation, F1A
  Jules CI gates, F2 landing page, deferred F3/F4, F5 lead form, and F6
  quality hardening.
- `TASKS.md` reconciles PR #58 as merged, PR #30 as stale Draft, and this task
  as local-only `in_progress`.
- Memory decisions archive the superseded Antigravity ownership records and
  add DEC-20260801-011 for Codex-owned single-page preview delivery.
- No `frontend/**`, backend code, OpenAPI, CI workflow, dependency, secret, or
  production resource changed.

## Checks

- Design-reference hash: passed, SHA-256
  `d79cae4ea8647b6a4f81975debd3ce96b229b468b6f9a37f1a4a6184d1e8af35`.
- Scope: passed; `frontend/` is absent, and no backend, OpenAPI, workflow,
  dependency, secret, or production resource changed.
- Markdown local links: passed for changed Markdown files.
- Whitespace diff check: passed.
- TruffleHog changed-file scan: passed, 0 findings.
- Maven verify: passed, 660 tests, coverage gate met.
- Specification review: completed; findings were addressed in the current
  diff. A separate read-only quality review did not complete before local
  commit, so it is not recorded as a passed gate.

## Next safe steps

1. Finish local checks and commit this replacement metadata atomically.
2. Ask the user for explicit authorization before pushing or opening a Draft
   PR.
3. After the replacement is merged with explicit authorization, close PR #30
   as `superseded` only with a separate explicit authorization.
4. Start `task-frontend-foundation` from a new external worktree based on the
   then-current `origin/main`.
