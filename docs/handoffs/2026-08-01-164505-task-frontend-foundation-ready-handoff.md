# task-frontend-foundation Ready transition handoff

Signature: HND task-frontend-foundation [ready] topics: frontend, security, testing, ci → predecessor: `2026-08-01-164024-task-frontend-foundation-draft-handoff.md`

## Durable — safe to cite later

- The current user request authorizes the remaining F1 lifecycle: Ready after
  fresh exact-head gates, squash merge to `main`, and post-merge CI/security
  verification. Auto-merge remains disabled.
- The F1 code, tested toolchain, canonical contract alignment, and Draft
  publication are complete. This handoff and tracker transition are
  controller-only metadata and do not change frontend or backend behavior.
- F1A remains a separate Jules-owned CI task from a new worktree after F1 is
  verified on `main`. Production deployment remains blocked and out of scope.

## Snapshot at 2026-08-01T16:45:05Z — re-verify live before use

- Draft PR [#60](https://github.com/devDaniilNovikov/AndrewWorkWebSite/pull/60)
  was `MERGEABLE` and `CLEAN` at exact head
  `ee8a0ba80c502ade190af727fcf73bfc14cc6233`, based on unchanged `main`
  `2e51f44dd9227f3c0c008be27597fb19728b3fc8`.
- Exact-head `Repository policy`, `verify`, and `dependency-security` passed.
  Semgrep policy scan, CodeQL, and Snyk also passed; the signed PR relay and
  event-inapplicable duplicate jobs skipped as designed.
- GitHub reported no review or unresolved review thread. Branch protection
  required an up-to-date branch, conversation resolution, linear history,
  and squash-only merge; administrators were included.
- The remote source branch retention setting remained enabled, auto-merge
  remained disabled, and PR #30 was not changed.

## Next steps — already authorized, require exact live evidence

1. Commit and push this Ready metadata. Treat every result for `ee8a0ba` as
   stale for the new head and wait for its full PR CI, Semgrep, and Snyk set.
2. If and only if the new exact head is mergeable, CLEAN, current with
   `main`, and all required/applicable checks succeed with no unresolved
   thread, mark PR #60 Ready.
3. Re-read the same head after the Ready event. If it remains fully green,
   squash-merge with the PR title and an exact-head match; never use admin
   bypass, auto-merge, rebase, merge commit, or force-push.
4. Fetch `main`, verify the squash commit, retained source branch, closed PR,
   and fresh post-merge CI/security workflows. Start F1A only afterward in a
   new worktree; do not deploy the preview or modify PR #30.
