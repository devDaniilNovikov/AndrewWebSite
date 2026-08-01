# task-frontend-ci-gates dispatch handoff

Signature: HND task-frontend-ci-gates [in_progress] topics: frontend, ci, testing, tracker → predecessor: `2026-08-01-164505-task-frontend-foundation-ready-handoff.md`

## Durable — safe to cite later

- F1 merged through PR #60 as
  `d0346b716772f2d1a3debe5692c604950c4b143f`. Its retained source branch is
  `task-frontend-foundation`; no production deployment was created.
- F1A owns only the stable frontend GitHub Actions quality context and narrow
  policy tests. It does not own frontend product behavior, dependencies,
  business content, backend code, credentials, or deployment.
- Codex is the task controller and final reviewer. Jules is the implementer
  through one owner-authored sanitized Issue with exactly the
  `jules-action` label. Jules may open a reviewable PR but never mark it Ready
  or merge it.
- The stable job name is `Frontend quality`. It must run the commands already
  declared by the merged F1 manifest with Node `24.14.0`, Corepack `0.34.5`,
  pnpm `11.18.0`, and the frozen lockfile. Backend-only changes must not
  install the frontend while still producing a stable successful context.
- Branch protection may require `Frontend quality` only after the exact
  implementation head proves that context green. Existing required checks
  must not be renamed, removed, or weakened.

## Snapshot at 2026-08-01T16:58:05Z — re-verify live before use

- Fresh `origin/main` and this dedicated controller branch both pointed to
  `d0346b716772f2d1a3debe5692c604950c4b143f`. Worktree:
  `/Users/daniilnovikov/.codex/worktrees/frontend-ci-gates/AndrewWorkWebSite`.
- Post-merge `main` Repository policy, verify, Java security/CodeQL,
  Dependency Submission, and Semgrep all succeeded for `d0346b7`; the Jules
  failure responder skipped because CI succeeded.
- GitHub actor and `JULES_ALLOWED_ACTOR` both resolved to
  `devDaniilNovikov`; `JULES_AUTOMATION_ENABLED` was `true`, and the guarded
  `jules-action` label existed. No secret value was read.
- No remote `task-frontend-ci-gates` branch, F1A Issue, or F1A pull request
  existed at startup. PR #30 remained outside this task.
- The current user request authorized F1A publication, CI verification,
  Ready transition, and squash merge after exact-head green gates. It did not
  authorize production deployment or weakening the fail-closed content gate.

## Next steps — live evidence required

1. Commit and publish only this controller reconciliation, then create one
   sanitized owner Issue with exactly `jules-action`, directing Jules to
   continue `task-frontend-ci-gates` from this metadata head.
2. Verify exactly one guarded Jules workflow accepts the Issue. Wait for its
   session and Draft PR; do not create a competing implementation branch.
3. Codex reviews the complete Jules diff, reproduces the frontend commands,
   tests path filtering and the stable context, and resolves every blocking
   finding without weakening existing gates.
4. Only after the exact PR head is green may branch protection add
   `Frontend quality`; then require a fresh protected exact-head cycle before
   Ready and the authorized squash merge. Verify post-merge `main` checks and
   keep production deployment disabled.
