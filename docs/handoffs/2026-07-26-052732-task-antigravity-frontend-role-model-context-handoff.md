# task-antigravity-frontend-role model-context handoff

Signature: HND task-antigravity-frontend-role [draft_pr] topics: frontend, tracker, memory, process → predecessor: 2026-07-26-040219-task-antigravity-frontend-role-draft-handoff.md

## Durable — safe to cite later

- Gemini 3.1 Pro High is the required model for every assigned Antigravity
  frontend planning, implementation, diagnosis, self-review, and local
  completion session. An unconfirmed or different model blocks repository
  edits; silent fallback is forbidden.
- Root `GEMINI.md` now loads the shared router and Antigravity role, then
  directs the agent to live Git/GitHub, `TASKS.md`, the current handoff, the
  assigned F1-F6 task, `docs/SPEC.md`, and only that task's canonical sources.
- `.agents/ANTIGRAVITY.md` is the canonical role and model source. The
  assigned task defines executable scope and acceptance; `docs/SPEC.md`
  routes architecture, product, API, privacy/security, and production
  contracts. Live state and canonical contracts outrank memory and aesthetic
  preference.
- Historical handoffs retain the model allocation in force when they were
  written. DEC-20260726-010 supersedes only the prior Gemini 3.6
  default / limited Gemini 3.1 Pro allocation.
- Draft PR #46 remains the publication vehicle. Ready, merge, deployment, and
  production mutation remain unauthorized.

## Snapshot at 2026-07-26T05:27:32Z — re-verify live before use

- Base: `0b0a62acfce09857807c4eb11e92795af3c20576`, equal to fetched
  `origin/main`.
- Existing Draft PR #46 remote head:
  `8fef0d3376bdae10bb553c44ed2cdfe191859a04`; all applicable checks on that
  head passed with expected event-specific skips.
- Local model-context commit:
  `1d8a80f7ed8bebc64452f5a706f24cb02c1d8039`; local metadata head:
  `0bfeed480db7afbbc3dd496b2076f2d17a7c7cd3`.
- Branch and worktree: `task-antigravity-frontend-role` at
  `/Users/daniilnovikov/.codex/worktrees/antigravity-frontend-role/AndrewWorkWebSite`.
- Role length is 96 lines; shared `AGENTS.md` remains 59 lines. Context-link,
  Gemini-route, decision-signature, and `git diff --check` validations passed
  before the core commit.

## Next steps — conditional on live evidence

1. Run fresh exact-head Maven, link/route/memory/diff/secret checks and
   independent review.
2. Update the existing Draft PR body, push the final head, and wait for its
   remote checks. Keep the PR Draft; do not mark Ready, merge, or deploy.
