# fix-tracker-canonical-links handoff
Signature: HND T2 [in_progress] topics: tracker → predecessor: 2026-07-19-182536-task-context-refactor-handoff.md

## Durable — safe to cite later

- The user authorized T2 through Ready after PR #24 merged, followed by T4,
  T6, and T5 as separate non-stacked tasks with separately authorized merges.
- The unpublished T2 commit `307d9d4` was based on pre-PR #24 state. Its
  branch provenance was verified, then the obsolete commit was dropped while
  rebasing the branch onto merge `66e2afa9d34bf96623ad1e09beb3661341c428cf`.
- New tracker and handoff links use the canonical repository slug
  `devDaniilNovikov/AndrewWebSite`.
- The redirect URL in the closed CI handoff is preserved because historical
  handoffs are immutable after a successor exists. This handoff records the
  canonical replacement: [PR #23](https://github.com/devDaniilNovikov/AndrewWebSite/pull/23).
- T2 owns tracker and handoff reconciliation only. T4, T6, T5, deployment,
  and backend implementation remain outside this branch.

## Snapshot at 2026-07-19T19:21Z — re-verify live before use

- `origin/main` is merge `66e2afa9d34bf96623ad1e09beb3661341c428cf`.
- PR #24 is merged and its source branch remains retained at `4fc6699`.
- Post-merge Repository policy, verify, Java security/CodeQL workflow, and
  Semgrep completed successfully on `main`.
- `TASKS.md` records T2 as `in_progress`; no PR exists for T2 yet.
- User-owned untracked `receipts/` and `review-receipts/` remain outside the
  task diff.

## Next steps — conditional, each requires the stated live check

1. Verify tracker links, memory signatures, hot-layer budget, diff scope, and
   Maven tests against the unchanged base.
2. Commit and push T2, open a Draft PR, and wait for fresh required CI and
   security checks.
3. After review passes, add a successor Ready handoff and mark the PR Ready.
4. Merge only after a separate explicit user command; do not start T4 first.
