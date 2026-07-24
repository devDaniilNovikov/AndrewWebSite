# task-db-flyway-baseline handoff

Signature: HND task-db-flyway-baseline [ready] topics: backend, deploy, security → predecessor: 2026-07-24-153930-task-db-flyway-baseline-handoff.md

## Durable — safe to cite later

- The user explicitly authorized the Ready transition and squash merge for
  PR #36. Production deployment remains outside that authorization.
- The implementation, PostgreSQL 18/Flyway contract, security remediation,
  Docker evidence, and independent reviews remain unchanged from the
  predecessor handoff, with no unresolved finding.

## Snapshot at 2026-07-24T15:47Z — re-verify live before use

- Draft [PR #36](https://github.com/devDaniilNovikov/AndrewWebSite/pull/36)
  is open against `main`, mergeable, and points to exact implementation head
  `42efb1dc7977d2627704f135b8e03f8691e0c212` before this readiness metadata
  checkpoint.
- On that exact head, both Repository policy and Maven verify event runs,
  dependency-security, Java security, CodeQL, Semgrep, and Snyk completed
  successfully. Event-specific companion jobs were correctly skipped.
- The final local `./mvnw -B verify` passed 77 tests, including 8 PostgreSQL
  18.4 database tests, with the JaCoCo gate green. Diff, scope, relative-link,
  memory-budget, conflict-marker, and changed-text secret checks passed.
- Live `origin/main` remains
  `806b39da746d2238dd2575348aa1f334e5dcd839`; the PR has no comments,
  reviews, or requested changes. Frontend, PR #30, workflows, production
  services/configuration, and secrets remain untouched.

## Next steps — conditional, each requires the stated live check

1. Commit and normally push this readiness checkpoint, rerun the local Maven
   gate, and wait for every required/applicable check on its exact SHA.
2. Confirm PR #36 still targets `main`, then mark it Ready and re-check the
   exact head, mergeability, review state, and required contexts.
3. Squash-merge only PR #36 with an exact-head guard, then verify the merge on
   `origin/main`, post-merge checks, and the retained source branch.
4. Do not deploy production. The next backend task reconciles row 22 to
   `merged` and moves this handoff chain to closed chains.
