# fix-sec-03-pii-anonymization-uuidv4 verified local handoff

Signature: HND fix-sec-03-pii-anonymization-uuidv4 [in_progress] topics: backend, privacy, security, tracker

## Durable — safe to cite later

- The public JSON boundary accepts only canonical RFC-variant UUIDv4
  `requestId` values, including the typed honeypot path. Version 1, nil, and
  non-RFC variant UUIDs fail before lead acceptance.
- Retention replaces the client-controlled `source_path` with `/` in the same
  transaction that clears lead PII and blocks undelivered Telegram work.
- Forward-only Flyway migration
  `V2__privacy_identity_hardening.sql` backfills already anonymized rows,
  strengthens `ck_leads_privacy`, and enforces UUIDv4 for new or changed
  active rows without preventing a legacy non-v4 row from reaching the
  anonymized state.
- The OpenAPI, architecture, operations, migration, API, persistence, and
  retention tests now describe the same contract.
- No dependency, workflow, container, frontend, production setting, external
  configuration, or deployment resource changed.
- The user authorized a separate PR, CI/CD verification, and merge to `main`
  for every SEC finding. Production deployment remains outside this task.

## Snapshot at 2026-07-27T05:21:26Z — re-verify live before use

- Branch `fix-sec-03-pii-anonymization-uuidv4` is based directly on fresh
  `origin/main` `9d33ff6ee5b7d3893409c85c671d59d57df4e819`.
- Implementation commit is
  `3530f0f406ead95116ef858726f465352acb8b26`.
- The `code-debugging` workflow classified the reproduced failures as one
  logic defect. The initial focused RED produced 14 expected failures across
  the API, retention SQL, migration, and database constraints. Focused GREEN
  passed 107/107 tests, including a real PostgreSQL 18.4 V1-to-V2 upgrade.
- The first full verification exposed 19 invalid legacy test fixtures rather
  than a production regression. Replacing their nil/version-0 identifiers
  with deterministic RFC-variant UUIDv4 fixtures made the affected 19/19
  tests pass without weakening the new constraint.
- Fresh `./mvnw -B clean verify` passed 648/648 tests with PostgreSQL 18.4,
  Testcontainers, both Flyway migrations, and enforced 100% line and branch
  coverage.
- Fresh `./mvnw -B -DexcludedGroups=database clean verify` passed 603/603
  tests and matches the Docker builder verification command.
- Runtime dependency inspection succeeded with no manifest change. OpenAPI
  YAML parsing and `git diff --check` passed.
- Semgrep ran 88 Java/security rules over 68 production Java files with zero
  findings. Exact changed-file TruffleHog scans found zero verified,
  unverified, or unknown secrets.
- Final specification, migration-safety, correctness, privacy, and security
  review found no unresolved Critical or Important defect against the
  mandatory SEC-03 remediation.
- UUIDv4 shape alone is not proof of client entropy. A keyed identity
  tombstone or shorter identifier retention would be a separate
  architectural hardening decision; it is not silently claimed by this
  branch.

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
