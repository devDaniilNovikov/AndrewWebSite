# fix-code-review-hardening-replacement handoff

Signature: HND fix-code-review-hardening-replacement [in_progress] topics: backend, deploy, tracker, process → predecessor: 2026-07-26-072227-fix-revert-code-review-hardening-merged-handoff.md

## Durable — safe to cite later

- The user authorized a corrected replacement for the rolled-back PR #45,
  including push, Draft/Ready PR, CI, and squash merge into `main`.
  Production deployment was not authorized.
- The replacement owns only `Dockerfile`, its container contract test,
  tracker, and append-only handoff metadata. Public APIs, database schema,
  and application runtime behavior remain unchanged.
- Current defects remain distinct from future roadmap contracts under
  LES-20260725-013. Dependency-aware readiness belongs to the blocked
  `task-backend-observability` and is outside this remediation.

## Snapshot at 2026-07-26T07:22:27Z — re-verify live before use

- Branch `fix-code-review-hardening-replacement` and worktree
  `/Users/daniilnovikov/.codex/worktrees/code-review-hardening-replacement/AndrewWorkWebSite`
  were created from fresh
  `848e94f90ee179b95671ae2eaed8a04cb59bb4e5`.
- The rolled-back Dockerfile contains `-DskipTests` only in
  `dependency:go-offline`; the exact final build command remains
  `./mvnw -B -DexcludedGroups=database verify`.
- `ContainerContractTest` currently rejects `maven.test.skip` but does not
  reject `-DskipTests`.
- The replacement branch is not pushed and no replacement PR exists.

## Next steps — conditional on live evidence

1. Add a regression assertion rejecting both Maven test-skip mechanisms and
   confirm focused RED against the rolled-back Dockerfile.
2. Remove only `-DskipTests` from dependency prefetch and confirm focused
   GREEN plus the full Maven gate.
3. Build the exact image, run the PostgreSQL/Flyway container smoke test,
   scan the whole diff, and iterate independent review to 10.0/10.0.
4. Push, open Draft, wait for CI, mark Ready, and squash-merge only under the
   user's explicit authorization. Retain the branch and do not deploy.
