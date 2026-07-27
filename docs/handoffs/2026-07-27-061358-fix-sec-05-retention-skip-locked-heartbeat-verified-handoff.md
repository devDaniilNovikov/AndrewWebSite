# fix-sec-05-retention-skip-locked-heartbeat verified local handoff

Signature: HND fix-sec-05-retention-skip-locked-heartbeat [in_progress] topics: backend, privacy, security, tracker → predecessor: 2026-07-27-060528-fix-sec-05-retention-skip-locked-heartbeat-handoff.md

## Durable — safe to cite later

- Retention still drains anonymization and 12-month deletion work in bounded
  `FOR UPDATE SKIP LOCKED` batches.
- After both drains, one non-locking PostgreSQL snapshot uses the existing
  partial retention indexes and two `EXISTS` probes to prove that neither
  cutoff has an eligible row.
- MVCC exposes a cutoff-eligible row to that snapshot even when another
  transaction holds its row lock. An apparently empty batch therefore cannot
  produce a complete-pass heartbeat while skipped work remains.
- The prior heartbeat stays unchanged under either anonymization or deletion
  lock contention. Releasing the lock lets the next pass finish and record
  success.
- No migration, dependency, workflow, container, frontend, production
  setting, external configuration, or deployment resource changed.
- The user authorized a separate PR, CI/CD verification, and merge to `main`
  for every SEC finding. Production deployment remains outside this task,
  and the source branch must be retained after merge.

## Snapshot at 2026-07-27T06:13:58Z — re-verify live before use

- Branch `fix-sec-05-retention-skip-locked-heartbeat` is based directly on
  merged SEC-04 commit
  `6d800ec4189dc36011db7a806d8f12042ccf1a9e`.
- Implementation commit is
  `2d81598f359face2840b1fe6c0ad502460020002`.
- The `code-debugging` workflow used separate real PostgreSQL transactions
  to lock cutoff-eligible rows. RED first proved the PII case, then the final
  RED suite produced exactly two failures: anonymization and technical
  deletion both advanced the heartbeat while their rows remained.
- Focused GREEN passed 17/17 service, repository, boundary, and controlled
  contention tests. Both lock tests also prove recovery after rollback.
- Fresh `./mvnw -B clean verify` passed 660/660 tests with PostgreSQL 18.4,
  Testcontainers, both Flyway migrations, and enforced coverage checks.
- Fresh `./mvnw -B -DexcludedGroups=database clean verify` passed 612/612
  tests and matches the Docker builder verification command.
- JaCoCo reported 1,733/1,733 lines and 812/812 branches covered.
- Runtime dependency inspection succeeded with no manifest change.
  `git diff --check` passed.
- Semgrep ran 88 Java/security rules over 69 production Java files with zero
  findings. Exact changed-file TruffleHog scans covered 12 files and found
  zero verified, unverified, or unknown secrets.
- Final specification, SQL/index, concurrency, privacy, correctness, and
  security review found no unresolved Critical or Important defect against
  SEC-05.

## Next steps

1. Commit this metadata checkpoint, then push the exact branch head and open
   one Draft PR into `main`.
2. Prove local, remote, and PR head equality. Wait for every exact-head
   repository, build, dependency, SAST, and security check; inspect reviews,
   conversations, alerts, and the final diff.
3. If the exact head remains clean and mergeable, follow the authorized
   Ready and squash-merge lifecycle, retain the source branch, verify
   post-merge `main`, and do not deploy production.
