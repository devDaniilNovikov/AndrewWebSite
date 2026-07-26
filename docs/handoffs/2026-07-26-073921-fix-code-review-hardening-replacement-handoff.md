# fix-code-review-hardening-replacement verification handoff

Signature: HND fix-code-review-hardening-replacement [in_progress] topics: backend, deploy, tracker, process, incident → predecessor: 2026-07-26-072227-fix-code-review-hardening-replacement-handoff.md

## Durable — safe to cite later

- The corrected replacement changes only the Docker dependency-prefetch
  command and its container contract test. It does not change public APIs,
  database schema, or application runtime behavior.
- `ContainerContractTest` now rejects both `-DskipTests` and
  `maven.test.skip`; the Docker builder still executes the exact final command
  `./mvnw -B -DexcludedGroups=database verify`.
- LES-20260725-013 keeps future dependency-aware readiness in the blocked
  `task-backend-observability`, outside this remediation.
- LES-20260726-015 records the PostgreSQL 18 container data-layout surprise
  encountered in the disposable smoke harness.

## Snapshot at 2026-07-26T07:39:21Z — re-verify live before use

- Base is `848e94f90ee179b95671ae2eaed8a04cb59bb4e5`; implementation commits are
  `f80b6ad154404d15b73ee5cf807ac18ef90a63e9` and
  `3e3eb3399c95a5322fd207e4105c311c21be9871`.
- Focused RED failed only because Dockerfile contained `-DskipTests`.
  Focused GREEN passed 2/2, and `./mvnw -B clean verify` passed 447/447 with
  PostgreSQL 18.4, Flyway, and all JaCoCo checks.
- Exact image
  `andrew-website:review-hardening-replacement-3e3eb33` built without cache
  with digest
  `20c72f20c20df38e2ee0218dd6b2361684d61d82f1dc51c59c6badfa4d596688`;
  its builder ran the exact final verify command with 431/431 tests passing.
- The corrected PostgreSQL 18.4 smoke reported healthy liveness and readiness,
  `Cache-Control: no-store`, runtime UID/GID `10001:10001`, Flyway version 1,
  and the expected `leads` and `telegram_outbox` tables. Temporary containers
  and networks were cleaned.
- The first disposable smoke harness used the pre-18 tmpfs target
  `/var/lib/postgresql/data`; PostgreSQL 18 rejected it. Mounting
  `/var/lib/postgresql` fixed the harness without a repository change.
- `git diff --check`, TruffleHog 3.95.9, Semgrep, image environment/history
  review, and Docker Scout completed cleanly; Scout reported zero critical,
  high, medium, or low vulnerable packages.
- The branch remains local-only. No replacement PR exists, independent review
  is pending, and no production deployment was authorized or performed.

## Next steps — conditional on live evidence

1. Commit this verification metadata and rerun the exact-head local gates.
2. Obtain a fresh independent 10.0/10.0 whole-diff review against the base.
3. Push, open Draft, wait for all CI/security checks, and review each new head.
4. Mark Ready and squash-merge only after the exact Ready head is green and
   independently rated 10.0/10.0. Retain all source branches and do not deploy.
