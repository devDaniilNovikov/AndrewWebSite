# task-db-flyway-baseline handoff

Signature: HND task-db-flyway-baseline [in_progress] topics: backend → predecessor: none

## Durable — safe to cite later

- PostgreSQL 18 is the project-wide local, test, and production database
  contract. Automated migration tests use the official `postgres:18-alpine`
  image through Spring-owned Testcontainers.
- The installed local PostgreSQL 18 server and its databases remain untouched;
  disposable test and smoke databases run only in Docker.
- The user explicitly authorized the Draft-to-Ready transition and squash merge
  after exact-head local, CI, security, and review gates pass. Production
  deployment is not authorized.
- Full host and CI Maven verification includes database tests. Docker image
  construction excludes only the `database` JUnit tag because a Docker build
  step cannot safely mount the host Docker socket.

## Snapshot at 2026-07-24T13:37Z — re-verify live before use

- Fresh `origin/main` is
  `806b39da746d2238dd2575348aa1f334e5dcd839`, the squash merge of PR #35.
- Branch `task-db-flyway-baseline` and its dedicated external worktree were
  created directly from that commit; no prior branch or PR exists for the task.
- Docker client/server 29.6.2 is available. The developer-installed PostgreSQL
  reports 18.4, and the Docker-based implementation gate has not yet run.
- Frontend paths, PR #30, CI workflows, production services, secrets, and the
  desktop-root untracked files remain outside this task.

## Next steps — conditional, each requires the stated live check

1. Record the database contract test failing before the V1 migration exists,
   then implement the smallest exact PostgreSQL 18 schema and refactor its
   catalog/constraint/index coverage.
2. Reconcile active PostgreSQL documentation, run full Maven and disposable
   Docker gates, and complete independent correctness/security reviews.
3. Push normally, open a Draft PR, wait for all exact-head checks, mark Ready,
   and squash-merge only after re-verifying the standing user authorization.
