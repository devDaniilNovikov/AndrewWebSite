# task-frontend-ci-gates native dispatch handoff

Signature: HND task-frontend-ci-gates [in_progress] topics: frontend, ci, testing, tracker, incident → predecessor: `2026-08-01-170547-task-frontend-ci-gates-blocked-handoff.md`

## Durable — safe to cite later

- The user explicitly selected the documented native Jules GitHub App route
  for F1A and authorized Draft publication, the Ready transition, adding the
  proven `Frontend quality` context to branch protection, and exact-head
  squash merge. Production deployment and PR #30 remain excluded.
- Issue #61 must never carry `jules-action` and `jules` simultaneously. Remove
  the failed custom label first, then apply only `jules`; do not retry run
  `30709431088`.
- Native Jules may publish one generated implementation branch. Preserve the
  existing `task-frontend-ci-gates` controller branch as incident history,
  merge it into the generated branch without history rewrite, and use only
  the combined generated branch for the single F1A pull request.
- Jules owns the frontend CI implementation. Codex owns the sequential branch
  consolidation, shared tracker and handoff reconciliation, final review,
  branch-protection mutation, Ready transition, and merge lifecycle.

## Snapshot at 2026-08-01T17:41:20Z — re-verify live before use

- `origin/main` was `d0346b716772f2d1a3debe5692c604950c4b143f`.
- The clean local and remote controller head was
  `805718a93a10ccdc1796fbbaf6625ab5d9c11dc7`, directly based on that main.
- Owner-authored Issue #61 was open with exactly `jules-action`, no comments,
  no Jules session, and no F1A branch or pull request from Jules.
- Main protection required `Repository policy`, `verify`, and
  `dependency-security` with strict mode; `Frontend quality` was not required.
- PR #30 remained open, Draft, and unchanged at `00f55ee`.

## Conditional continuation

1. Publish this reconciliation, update the sanitized Issue body, remove
   `jules-action`, and apply only `jules`.
2. Accept exactly one native Jules session and implementation branch. If the
   App does not respond, requests manual setup, or creates duplicates, stop
   without fallback or a second label.
3. Review the Jules implementation, merge this controller history into its
   branch, add final Codex metadata, and prove full and skip paths.
4. Require `Frontend quality` only after a successful exact implementation
   head, then obtain a fresh protected final head before Ready and squash
   merge. Retain both source refs and do not deploy.
