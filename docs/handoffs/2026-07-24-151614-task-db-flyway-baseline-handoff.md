# task-db-flyway-baseline handoff

Signature: HND task-db-flyway-baseline [draft_pr] topics: backend, deploy → predecessor: 2026-07-24-133729-task-db-flyway-baseline-handoff.md

## Durable — safe to cite later

- Flyway V1 owns the exact PostgreSQL 18 `leads` and `telegram_outbox`
  baseline, including named privacy/state constraints and four partial
  indexes. Hibernate/JPA schema generation is not present.
- Spring Boot manages the Flyway, PostgreSQL JDBC, and Testcontainers
  dependency versions. Host and CI verification includes the two
  `database`-tagged suites; the Docker builder excludes only that tag.
- The exact Java image runs as `10001:10001`, exposes the existing liveness
  healthcheck, and contains no datasource or other secret-bearing image
  environment/history entry.
- The user authorized the Ready transition and squash merge after exact-head
  checks pass. Production deployment remains explicitly out of scope.

## Snapshot at 2026-07-24T15:16Z — re-verify live before use

- Draft [PR #36](https://github.com/devDaniilNovikov/AndrewWebSite/pull/36)
  is open against `main`, mergeable, and initially pointed to
  `93d21c2e2ee5c74ce7ef2ad7b25085d614ac9ed3`.
- Implementation commit `0d225ec` adds the PostgreSQL 18 schema and tests;
  documentation commit `93d21c2` aligns active backend sources and future
  plans on PostgreSQL 18.
- Focused database verification passed 8 tests on PostgreSQL 18.4. Full
  `./mvnw -B verify` passed 77 tests with 91.62% line coverage.
- Docker builder verification passed 69 non-database tests and JaCoCo.
  Disposable `tmpfs` PostgreSQL smoke reached liveness `UP` and Docker
  health `healthy`, applied only Flyway V1, verified port `18080` and
  runtime UID/GID `10001:10001`, then removed both named containers and the
  named network.
- Diff, conflict-marker, relative-link, memory-budget, owner-scope, and
  changed-text secret checks passed. Independent Java/correctness and
  security reviews found no Critical, Important, or Minor finding.
- The installed local PostgreSQL 18 server and its data, frontend, PR #30,
  CI workflows, production services/configuration, and secrets remain
  untouched.

## Next steps — conditional, each requires the stated live check

1. Commit and normally push this tracker/handoff checkpoint.
2. Rerun the final local gate and wait for every required/applicable GitHub
   check on the exact pushed SHA.
3. With checks green, record the Ready checkpoint, mark PR #36 Ready, and
   squash-merge it under the standing user authorization with an exact-head
   guard.
4. Verify the live merge on `origin/main`, the retained source branch, and
   post-merge checks. Do not deploy production.
