# fix-sec-04-send-retention-race verified local handoff

Signature: HND fix-sec-04-send-retention-race [in_progress] topics: backend, privacy, telegram, security, tracker → predecessor: 2026-07-27-053724-fix-sec-04-send-retention-race-handoff.md

## Durable — safe to cite later

- Telegram delivery now reserves the complete Boot-managed connect plus read
  timeout budget inside both the outbox lease and the 29-day operational PII
  window.
- Startup fails unless the configured lease exceeds that HTTP budget.
- Reload uses an explicit observation time and future-aware privacy cutoff.
  The worker then computes
  `min(lease_until, created_at + 29 days) - HTTP timeout budget`.
- The worker checks that absolute latest start after reload. The gateway
  checks it again after formatting and immediately before the synchronous
  HTTP call; equality is fail-closed.
- A deterministic PostgreSQL regression performs a real reload, advances
  time, commits retention, and returns the cached message. The worker
  confirms terminal `blocked/privacy_expired`, the lead is anonymized, the
  successful heartbeat reflects the completed safe skip, and the gateway is
  never invoked.
- No dependency, workflow, container, frontend, production setting, external
  configuration, or deployment resource changed.
- The user authorized a separate PR, CI/CD verification, and merge to `main`
  for every SEC finding. Production deployment remains outside this task,
  and the source branch must be retained after merge.

## Snapshot at 2026-07-27T05:51:16Z — re-verify live before use

- Branch `fix-sec-04-send-retention-race` is based directly on merged SEC-03
  commit `041933773cd611dc57396025b87e80463a33cee3`.
- Implementation commit is
  `7bc827d63091fcb4a5b9e58f9776a1f903d698b8`.
- The `code-debugging` workflow reproduced exactly three RED failures: two
  deterministic unit interleavings and one real PostgreSQL
  `reload → retention commit → gateway` interleaving. All three attempted
  to cross the gateway before the fix.
- A mechanical missed mock argument after the gateway contract migration was
  corrected without changing the design. The neighboring focused suite then
  passed 69/69, and the final focused suite including delivery-window edge
  cases passed 73/73.
- Fresh `./mvnw -B clean verify` passed 656/656 tests with PostgreSQL 18.4,
  Testcontainers, both Flyway migrations, and enforced coverage checks.
- Fresh `./mvnw -B -DexcludedGroups=database clean verify` passed 610/610
  tests and matches the Docker builder verification command.
- JaCoCo reported 1,720/1,720 lines and 810/810 branches covered.
- Runtime dependency inspection succeeded with no manifest change.
  `git diff --check` passed.
- Semgrep ran 88 Java/security rules over 69 production Java files with zero
  findings. Exact changed-file TruffleHog scans covered 20 files and found
  zero verified, unverified, or unknown secrets.
- Final specification, correctness, privacy, concurrency, and security
  review found no unresolved Critical or Important defect against SEC-04.

## Next steps

1. Commit this metadata checkpoint, then push the exact branch head and open
   one Draft PR into `main`.
2. Prove local, remote, and PR head equality. Wait for every exact-head
   repository, build, dependency, SAST, and security check.
3. Inspect all PR reviews, conversations, alerts, and the final diff. Fix any
   in-scope finding with a regression test before changing readiness.
4. Mark the PR Ready and squash-merge only after the exact metadata head is
   green and mergeable, as authorized by the current user request.
5. Retain the source branch and verify post-merge `main` checks. Do not
   configure or deploy production.
