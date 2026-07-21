# task-backend-deploy-stub handoff

Signature: HND task-backend-deploy-stub [draft_pr] topics: backend, deploy, tracker → predecessor: 2026-07-20-065910-task-backend-deploy-stub-handoff.md

## Durable — safe to cite later

- The branch merged `origin/main` at `3ce978c` through normal merge commit
  `7a249ff`; its only conflicts were the tracker and handoff index. The
  dependency-security replacement is now reconciled as merged, while the
  earlier cache task is terminal `superseded` with its closed source PRs
  retained.
- Docker build initially exposed a real build-stage defect: the executable
  contract reads `.dockerignore`, but that file was absent inside the Maven
  build stage. A focused RED contract test preceded the smallest fix in
  `a51b3be`, which copies the contract file with the Dockerfile.
- The focused contract test, host Maven verify, Maven verify inside the exact
  Docker build, image inspection/history checks, and local liveness smoke all
  passed. The local image was not pushed, and the named smoke container was
  removed after verification.
- PR #29 must remain Draft. No Ready transition, squash merge, production
  action, branch-protection change, PR #30 change, or secret mutation is
  authorized by this handoff.

## Snapshot at 2026-07-21T09:49Z — re-verify live before use

- Before this metadata checkpoint, local HEAD is `a51b3be` and the remote PR
  head remains `d65ea51`; a normal push is the next authorized remote action.
- The exact local image uses the configured non-root runtime user, exposes the
  liveness healthcheck, has no sensitive environment names or history markers,
  and returned the minimal successful liveness response with no-store caching.
- Docker Desktop daemon is available. No unrelated Docker resource was
  removed; only `andrew-website-smoke` was stopped and removed.
- The desktop-root worktree and its user-owned untracked directories remain
  untouched.

## Next steps — conditional, each requires the stated live check

1. Run final diff, link, memory-budget, scope, and changed-text secret checks
   on this metadata head, then rerun Maven verify.
2. Push normally, confirm PR #29's exact new head and current base, and wait
   for fresh `Repository policy`, `verify`, `dependency-security`, and every
   applicable security result.
3. Keep the PR Draft even after green checks. Mark Ready only after a separate
   explicit user command; merge and the next backend task require their own
   separate authorization.
