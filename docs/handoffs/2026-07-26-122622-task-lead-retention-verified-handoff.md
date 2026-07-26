# task-lead-retention verified implementation handoff

Signature: HND task-lead-retention [in_progress] topics: backend, privacy, security, tracker → predecessor: none

## Scope and authorization

- PR #52 was squash-merged under explicit user authorization as `857240f`;
  its source branch remains retained and its post-merge CI/security checks
  succeeded.
- The user authorized implementation, push, Draft PR publication, review
  fixes, and transition to Ready for `task-lead-retention`.
- Squash merge, production deployment, frontend/observability work, schema
  changes, and branch/worktree deletion remain unauthorized.

## Implementation

- RED commit `342c1b1` pins the privacy boundaries, rollback, concurrency,
  stale-token rejection, cascade deletion, configuration, metrics, heartbeat,
  database-free startup, and changed-payload replay behavior.
- GREEN commit `ad6ead5` adds the retention configuration, repository,
  transactional PostgreSQL implementation, bounded batch service, UTC
  calendar cutoff, heartbeat, and tagless aggregate metrics.
- Review-fix commit `75e30ee` adds database-free SQL-contract tests for both
  successful repository paths.
- Expiry uses deterministic bounded `FOR UPDATE SKIP LOCKED` selection and
  atomically terminalizes non-delivered outbox work, clears leases and PII,
  removes the fingerprint, and records anonymization.
- Deletion uses the canonical calendar cutoff and relies on the existing
  foreign-key cascade. HTTP/OpenAPI, Flyway, production, frontend, and OTLP
  paths are unchanged.

## Verified evidence

- Focused retention, replay, service, configuration, metrics, and validation
  gate passed 32 tests against PostgreSQL 18.4 with Flyway.
- The review regression gate passed four repository tests without a database.
- Fresh PostgreSQL/Flyway `clean verify` passed 526 tests.
- Fresh database-free `clean verify` passed 486 tests.
- JaCoCo reports 1346/1346 lines and 672/672 branches.
- Runtime dependency inspection succeeded and introduced no dependency
  change.
- TruffleHog found zero verified or unverified secrets.
- Semgrep Java/security policies found zero findings.
- `git diff --check` passed and the full diff changes no migration,
  architecture, OpenAPI, frontend, CI, or production configuration.

## Review

- The required first `code-reviewer` pass found no runtime correctness,
  privacy, transaction, concurrency, performance, configuration, or
  maintainability defect.
- The independent non-database gate then exposed one Important test-coverage
  gap: successful retention SQL paths depended on database-tagged tests.
- `75e30ee` fixes that gap with regression tests that pin UTC parameters,
  bounded locking, delivered-row exclusion, and deletion SQL.
- Focused and full gates were repeated, then the required final whole-diff
  `code-reviewer` pass found no remaining Critical, Important, or Minor
  finding.

## Next steps

1. Re-fetch `origin` and confirm `origin/main` remains the implementation base
   before publication.
2. Push `task-lead-retention`, create one Draft PR, and confirm local, remote,
   and PR head equality.
3. Wait for every exact-head CI/security check and inspect comments, reviews,
   alerts, and the final diff; fix any in-scope findings with regression tests.
4. After all gates are green, update the tracker/handoff and mark the PR Ready.
5. Stop before squash merge, production deployment, or branch deletion; each
   remains a separate user-only gate.
