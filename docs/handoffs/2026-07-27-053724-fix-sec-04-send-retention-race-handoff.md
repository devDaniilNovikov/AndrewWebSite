# fix-sec-04-send-retention-race task handoff

Signature: HND fix-sec-04-send-retention-race [in_progress] topics: backend, privacy, telegram, security, tracker

## Durable authorization and scope

- The user authorized one separate `fix-*` branch, PR, CI/CD cycle, and merge
  into `main` for each listed security finding.
- This branch owns only SEC-04: prevent a Telegram worker from sending a
  cached PII copy after retention has invalidated the database row.
- The required proof is an absolute delivery deadline, a fail-closed check
  immediately before the external HTTP call, and a deterministic regression
  test that commits retention between reload and send.
- Production configuration and deployment remain outside this task. The
  source branch must be retained after merge.

## Snapshot at 2026-07-27T05:37:24Z — re-verify live before use

- Branch `fix-sec-04-send-retention-race` is a fresh non-stacked branch from
  `origin/main` `041933773cd611dc57396025b87e80463a33cee3`.
- SEC-03 was merged by PR #56 as that exact squash commit. Post-merge CI,
  Dependency Submission, and Semgrep succeeded; its remote source branch is
  retained at `2d877bce669b2739d94337411c7526684677d835`.
- The older dirty worktree `/private/tmp/AndrewWorkWebSite-privacy-retention-races`
  remains untouched and is read-only reference material only. Its mixed
  SEC-03/04/05 prototype is not a branch base and will not be published.
- Current root cause: `TelegramWorker` reloads an eligible message, then calls
  `gateway.send` with that in-memory PII without rechecking either lease or
  privacy time. Retention can commit between those operations; the later
  outbox compare-and-set fails only after disclosure.

## Next steps

1. Add unit and PostgreSQL regression tests that reproduce the cached-send
   race before changing production code.
2. Implement the smallest absolute delivery-window contract shared by the
   worker, repository reload, and HTTP gateway.
3. Run focused RED/GREEN, neighboring gateway/repository/configuration tests,
   full database and container-equivalent gates, coverage, dependency,
   secret, static-analysis, and final security review.
4. Commit implementation and verified handoff before the already authorized
   publication path.
