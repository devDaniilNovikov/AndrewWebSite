# fix-frontend-track-preview-replacement handoff

Signature: HND fix-frontend-track-preview-replacement [active] topics: frontend, tracker, memory → predecessor: none

## Durable — safe to cite later

- The first frontend delivery is a Codex-owned single-page preview exported
  at `/`, plus a real static 404. Multi-page routes and verified trust-content
  population are deferred follow-up work.
- The supplied screenshot is preserved as
  `docs/frontend/reference/landing-ui-2026-08-01.jpg` with SHA-256
  `d79cae4ea8647b6a4f81975debd3ce96b229b468b6f9a37f1a4a6184d1e8af35`.
  It is a visual reference only; its phone, prices, cases, review, hours,
  logo, and wording are not production facts.
- Hosted preview builds cannot collect leads. Local preview submission is
  limited to loopback origins with explicit local CORS. Production uses only
  same-origin `/api/leads` and remains build-blocked until every required
  business, legal, price, proof, and licensed-media input is verified.
- Draft PR #30 remains open and untouched. It may be closed as `superseded`
  only after this replacement delivers the outcome and the user separately
  authorizes closure.
- No `frontend/**`, backend code, OpenAPI, CI workflow, dependency, secret,
  or production resource changed in this replacement task.

## Snapshot at 2026-08-01T10:46Z — re-verify live before use

- Branch `fix-frontend-track-preview-replacement` is based directly on fresh
  `origin/main` commit `90698e18d6ce602a0e09cbc870acc4fd80f16f83` in
  `/Users/daniilnovikov/.codex/worktrees/frontend-track-preview-replacement/AndrewWorkWebSite`.
- Local implementation commit is
  `5c2545084e2e2e10b4c310b62f11ea038a06e1a5`. No push, Draft PR, Ready
  transition, merge, deployment, or PR #30 closure has occurred.
- The role router assigns the approved preview track to Codex and keeps the
  Antigravity entries as inactive compatibility surfaces. The active task
  graph is F0 replacement, F1 foundation, F1A Jules CI gates, F2 landing page,
  deferred F3/F4, F5 lead form, and F6 quality hardening.
- The reference hash, changed-Markdown local links, scoped diff, whitespace,
  TruffleHog changed-file scan, and Maven verification passed. Maven ran 660
  tests with zero failures or errors and met the coverage gate.
- The first independent specification review found five documentation gaps;
  all were corrected. A second read-only review of exact commit `5c254508`
  returned PASS with no Critical or Important privacy, security, process, or
  specification finding.
- Live state at task start showed Draft PR #30 open, stale, and dirty against
  `main`; re-verify it immediately before any later GitHub action.

## Next steps — conditional, each requires the stated live check

1. Publish this branch and open a Draft replacement PR only after the user
   explicitly authorizes both external actions and live Git/GitHub state is
   re-verified.
2. Mark the replacement Ready and merge it only under their own later explicit
   authorizations and after exact-head CI and review pass.
3. Close PR #30 as `superseded` only after the replacement merge delivers its
   outcome and the user separately authorizes that closure.
4. Start `task-frontend-foundation` in a new external worktree from the then
   current `origin/main`; never stack it on this branch.
