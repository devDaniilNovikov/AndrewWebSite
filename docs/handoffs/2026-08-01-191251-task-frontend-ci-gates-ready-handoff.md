# task-frontend-ci-gates ready handoff

Signature: HND task-frontend-ci-gates [ready] topics: frontend, ci, testing, security, tracker, incident → predecessor: `2026-08-01-182056-task-frontend-ci-gates-handoff.md`

## Durable — safe to cite later

- The user explicitly authorized the native Jules route, the single combined
  PR, branch-protection update, Ready transition, and exact-head squash merge.
  Production deployment, branch deletion, and any PR #30 change remain
  excluded.
- PR #62 uses only `task-frontend-ci-gates-453244029589925767`. Normal merge
  commit `16f9900cbe9cb34dbab808f1712c8b32d3115c48` joined controller history;
  controller branch `task-frontend-ci-gates` remains read-only at
  `1978cc9922489777ff5bf1e2b5bcb49b875a018b` and is an ancestor of the PR
  branch. No rebase, force-push, cherry-pick, or ref deletion occurred.
- Jules owns the CI implementation. Codex restored only tracker, handoff,
  incident, task-plan, and Git Flow metadata after Jules follow-up
  `e3585b25ebe4f3081e746d73b6bbaba2fb6e7362` carried an older metadata tree.
- Protected `main` now strictly requires `Repository policy`, `verify`,
  `dependency-security`, and `Frontend quality`, all bound to GitHub Actions
  app id `15368`; the other protection settings were preserved.

## Verified implementation head — 2026-08-01T19:12:51Z

- `origin/main` remained
  `d0346b716772f2d1a3debe5692c604950c4b143f`; Draft PR #62 head was
  `e3585b25ebe4f3081e746d73b6bbaba2fb6e7362` and merge state was clean.
- Both `Frontend quality` runs succeeded: the PR path executed Node
  `24.14.0`, integrity-verified job-local Corepack `0.34.5`, manifest-pinned
  pnpm `11.18.0`, frozen install, Chromium with dependencies, and full
  `pnpm run verify`; the paired push path also returned the stable context.
- Exact-head Repository policy, both Maven `verify` runs,
  dependency-security, Java security/CodeQL, Semgrep, and Snyk succeeded.
  Repository policy included actionlint, Gitleaks, and the CI path/security
  policy test.
- Local `bash -n`, `test-ci-paths.sh`, `git diff --check`, a 660-test Maven
  verify, and a TruffleHog diff scan succeeded across the reviewed lifecycle.
- Independent specification and security reviews passed. Independent code
  review reproduced the new-branch shallow-fetch defect; Jules fixed it at
  `e3585b2`, and the follow-up review reproduced stale new-branch backend-only
  classification as `skip` and passed PR, ordinary-push, frontend, backend,
  and workflow-dispatch regression cases.
- PR #30 remained open Draft at `00f55ee`; Issue #61 remained open with only
  `jules`; auto-merge was off, automatic head deletion was off, and GitHub
  reported zero deployments for `e3585b2`.

## Conditional closure — re-verify live

1. Push this metadata-only commit and require a fresh exact-head cycle for
   all four protected contexts plus Semgrep, CodeQL, Snyk, and event-specific
   jobs. The push-side `Frontend quality` should prove the backend/docs skip
   path; the PR-side context should still exercise the full cumulative diff.
2. Confirm the final head, branch protection, clean merge state, no unresolved
   review thread, unchanged PR #30, zero deployments, and no auto-merge.
3. Mark PR #62 Ready, re-check its SHA, then squash-merge without admin
   bypass or branch deletion. Use the authorized PR title as the squash
   subject and a Codex attribution footer without the platform-added human
   co-author footers present in Jules source history.
4. Verify `origin/main`, Issue #61 closure, retained controller and generated
   refs, and post-merge CI. Do not deploy. Per tracker policy, the next task
   reconciles F1A from `ready` to `merged` using live merge evidence.
