# task-db-flyway-baseline handoff

Signature: HND task-db-flyway-baseline [draft_pr] topics: backend, deploy, security → predecessor: 2026-07-24-151614-task-db-flyway-baseline-handoff.md

## Durable — safe to cite later

- Direct Flyway, PostgreSQL JDBC, and Testcontainers dependency declarations
  remain versionless under Spring Boot dependency management.
- Spring Boot property overrides pin Jackson `3.1.5` and PostgreSQL JDBC
  `42.7.13` because the current Boot 4.1.0 defaults are affected by
  CVE-2026-59889 and CVE-2026-54291 respectively. Remove an override only
  after the managed version reaches the same or a later fixed release and
  the complete gate remains green.
- The user authorized the Ready transition and squash merge after every
  required and applicable check passes on the exact pushed head. Production
  deployment remains explicitly out of scope.

## Snapshot at 2026-07-24T15:39Z — re-verify live before use

- Draft [PR #36](https://github.com/devDaniilNovikov/AndrewWebSite/pull/36)
  remained open, mergeable, and current with `main` at pushed head
  `82e863338c90f22e23616c5d2012b90f5007ab24`.
- Repository policy, Maven verify, dependency-security, Java security,
  Semgrep, and CodeQL passed on that pushed head. The external Snyk context
  failed because the new graph resolved vulnerable PostgreSQL JDBC
  `42.7.11` and Jackson Databind `3.1.4`.
- Security fix `5d8bc1f` applies the narrow patch-level overrides. The
  executable JAR resolves and contains PostgreSQL JDBC `42.7.13` and Jackson
  Databind `3.1.5`; focused dependency and secret checks passed.
- Refreshed `./mvnw -B verify` passed 77 tests, including 8 database tests
  against PostgreSQL 18.4, with the JaCoCo gate green.
- Rebuilt image `andrew-website:local` passed its 69-test non-database
  builder gate. A disposable PostgreSQL 18 smoke reached liveness `UP` and
  Docker health `healthy`, applied exactly Flyway V1, and ran the application
  as `10001:10001`; only the named smoke containers and network were removed.
- Focused independent Java/Maven and security reviews reported no Critical,
  Important, or Minor finding. Frontend, PR #30, CI workflows, the installed
  local PostgreSQL server/data, production services, and secrets remain
  untouched.

## Next steps — conditional, each requires the stated live check

1. Commit this tracker/handoff checkpoint, rerun the final metadata-head
   Maven and repository checks, and normally push without rewriting history.
2. Wait for every required and applicable GitHub check, including Snyk, on
   the exact pushed SHA.
3. If all checks remain green and the head/base guard still matches, mark
   PR #36 Ready and squash-merge it under the standing user authorization.
4. Verify the live merge on `origin/main`, the retained source branch, and
   post-merge checks. Do not deploy production.
