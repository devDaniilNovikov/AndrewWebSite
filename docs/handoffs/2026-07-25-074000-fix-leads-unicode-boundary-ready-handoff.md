# fix-leads-unicode-boundary ready handoff

Signature: HND fix-leads-unicode-boundary [ready] topics: backend, security, tracker → predecessor: 2026-07-25-073543-fix-leads-unicode-boundary-draft-handoff.md

## Durable — safe to cite later

- The complete task diff contains only task metadata, the shared persisted
  text guard, and its unit, HTTP contract, and PostgreSQL integration
  regressions.
- NUL and isolated UTF-16 surrogate code units are rejected before HMAC and
  JDBC. Valid supplementary Unicode remains accepted and has exact
  PostgreSQL round-trip and idempotent-replay coverage.
- Independent whole-diff review found zero unresolved Critical, Important,
  or Minor findings. No test, gate, API contract, migration, or deployment
  control was weakened.
- The user authorized the normal Ready and squash-merge lifecycle for this
  non-Telegram fix. Production deployment remains outside this task.

## Snapshot at 2026-07-25T07:40:00Z — re-verify live before use

- Draft PR #41 is open, Draft, and mergeable against live `main`
  `2f565ddb736431cfe862bb247ae8a1fab8f67bca`.
- Before this metadata-only commit, exact head
  `e49c6439f86d199f18493506785496f445ccbdec` passed Repository policy,
  both verify paths, dependency-security, java-security, CodeQL, Semgrep,
  and Snyk. Event-specific dependency-security, java-security, and signed
  relay jobs skipped as designed.
- The local and remote branch matched at `e49c643` and the worktree was
  clean. Every commit had the required Codex co-author footer.
- This Ready handoff changes the PR head. Its new exact head and all checks
  must be refreshed before the PR is marked Ready or merged.

## Next steps — conditional, each requires the stated live check

1. Push the Ready metadata commit and wait for all checks on its exact head.
2. Publish an owner review comment that identifies the exact reviewed head
   and records zero unresolved actionable findings.
3. If the PR remains open, mergeable, based on unchanged `main`, and all
   exact-head checks remain green, mark it Ready and use the existing
   authorization for a guarded squash merge.
4. Fetch and verify the resulting `main` commit and post-merge checks. Keep
   the source branch and worktree; do not deploy.
