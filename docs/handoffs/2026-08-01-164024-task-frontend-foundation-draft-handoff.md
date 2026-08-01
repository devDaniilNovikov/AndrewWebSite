# task-frontend-foundation Draft PR handoff

Signature: HND task-frontend-foundation [draft_pr] topics: frontend, security, testing, ci → predecessor: `2026-08-01-144453-task-frontend-foundation-handoff.md`

## Durable — safe to cite later

- The user explicitly authorized publication, pull-request CI/CD, and merge
  to `main` after the completed local F1 handoff disclosed the secure
  Next.js `16.2.11` deviation from the older `16.2.9` planning value.
- The canonical architecture, F1 brief, and downstream static-JAR plan now
  agree with the audited frontend manifest: Next.js `16.2.11` and Node
  `24.14.0`. This removes the only blocking pre-publication review finding.
- F1 remains the standalone static preview foundation. F1A is a separate
  post-merge Jules-owned task that adds the stable `Frontend quality` CI
  context without changing product behavior.
- Production deployment is not part of F1 or F1A. The production build gate
  remains intentionally closed until every verified business-content blocker
  is resolved, and the repository has no deployment workflow or environment.

## Snapshot at 2026-08-01T16:40:24Z — re-verify live before use

- `origin/main` remained
  `2e51f44dd9227f3c0c008be27597fb19728b3fc8`; branch
  `task-frontend-foundation` was published without force-push and retained by
  repository policy.
- Draft [PR #60](https://github.com/devDaniilNovikov/AndrewWorkWebSite/pull/60)
  targets `main`, uses the Conventional Commit title
  `feat(f1-01): add tested static frontend foundation`, and was initially
  mergeable. Auto-merge and automatic source-branch deletion remain disabled.
- The exact worktree content before this controller-only lifecycle handoff
  passed the pinned `pnpm verify`: 11/11 Vitest tests, 100% statements, lines,
  and functions, 95.55% branches, 4/4 Chromium E2E cases, deterministic
  export digest
  `56d799f9c69539abe74e6713604778c7d3abeda7f13e94c9a2034de860966cc4`,
  and no known dependency vulnerability at the high audit threshold.
- `./mvnw -B verify` passed 660 tests and every JaCoCo gate. Changed diff
  lines passed TruffleHog; the only whole-file scanner alert was an unchanged,
  credential-free internal smoke-test JDBC URL already present on `main`.
- Independent specification, correctness, and security reviews returned PASS
  after the toolchain contract was synchronized.
- PR #30 remained open, Draft, dirty, and untouched at head `00f55eea`.

## Next steps — authorized sequence, each state must be fresh

1. Push this lifecycle metadata and ignore all CI results from earlier PR
   heads. Require the exact head to pass `Repository policy`, `verify`, and
   `dependency-security`, plus applicable Semgrep and Snyk checks.
2. With conversations resolved and the exact head mergeable, record the
   Ready state in the tracker and a successor handoff, push that metadata,
   and require its own fresh exact-head checks before marking PR #60 Ready.
3. Squash-merge only the final verified head using the approved PR title,
   then fetch and verify the squash commit on `origin/main`, retained source
   branch, and post-merge CI/security workflows.
4. Reconcile F1 to `merged` only from live merge evidence when starting the
   separately authorized F1A task in a fresh worktree. Do not deploy the
   preview or change PR #30 as part of this sequence.
