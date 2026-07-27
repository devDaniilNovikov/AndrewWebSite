# fix-sec-05-retention-skip-locked-heartbeat task handoff

Signature: HND fix-sec-05-retention-skip-locked-heartbeat [in_progress] topics: backend, privacy, security, tracker

## Durable authorization and scope

- The user authorized one separate `fix-*` branch, PR, CI/CD cycle, and merge
  into `main` for each listed security finding.
- This branch owns only SEC-05: prevent the privacy retention heartbeat from
  reporting a successful complete pass when eligible rows were skipped
  because another transaction held their row locks.
- Owned paths are the privacy retention implementation and tests, canonical
  backend architecture and operations contracts, `TASKS.md`, and append-only
  handoff metadata.
- Production configuration and deployment remain outside this task. The
  source branch must be retained after merge.

## Snapshot at 2026-07-27T06:05:28Z — re-verify live before use

- Branch `fix-sec-05-retention-skip-locked-heartbeat` is a fresh non-stacked
  branch from `origin/main`
  `6d800ec4189dc36011db7a806d8f12042ccf1a9e`.
- SEC-04 was merged by PR #57 as that exact squash commit. Post-merge CI,
  Java security/CodeQL, Dependency Submission, and Semgrep succeeded; its
  remote source branch is retained at
  `bc0602c205f204b67a51d8a3ca878b6120ac4532`.
- Current root-cause hypothesis: `expireBatch` and `deleteBatch` intentionally
  use `FOR UPDATE SKIP LOCKED`, while `RetentionService` treats a short or
  empty batch as end-of-work and advances its heartbeat without proving that
  no cutoff-eligible row remains.
- The older dirty worktree
  `/private/tmp/AndrewWorkWebSite-privacy-retention-races` remains untouched
  and is read-only reference material only.

## Plan

1. Under the `code-debugging` workflow, hold an expired lead lock on a
   separate PostgreSQL transaction and prove RED: retention returns but
   advances the heartbeat while the row still contains PII.
2. Add the smallest bounded completion proof for both anonymization and
   technical deletion, then rerun the controlled contention and neighboring
   retention suites.
3. Run full local and security gates, review the complete diff, and follow
   the already authorized Draft → Ready → squash-merge lifecycle without a
   production deployment.
