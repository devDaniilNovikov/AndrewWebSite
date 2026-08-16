# integration-sentry Draft published handoff

Signature: HND integration-sentry [draft-published] topics: backend, integration, observability, security, ci -> predecessor: 2026-08-16-161426-integration-sentry-handoff.md

## Durable — safe to cite later

- The user explicitly authorized implementation, PR publication, required CI,
  Ready transition, and squash merge for this Sentry integration.
- Draft [PR #71](https://github.com/devDaniilNovikov/AndrewWebSite/pull/71)
  is the only task PR and uses the exact authorized branch
  `integration-sentry`. Source-branch deletion, force push, admin bypass,
  auto-merge, production deployment, and any change to Draft PR #30 remain
  outside scope.
- No Sentry DSN is committed. Production release remains blocked until an
  authorized operator verifies the canonical secret-store DSN against
  `rogaandkopyta-pz/java-spring-boot-q1` and completes the documented non-PII
  error/log/metric/transaction/profile canary.

## Verified snapshot at 2026-08-16T18:36:25Z — re-verify live before use

- Draft PR #71 targeted `main` from `integration-sentry` at implementation
  checkpoint `dbf2825c9c11b44e93f7c716c430c4e9ba0392aa`, based directly on
  `origin/main` `c41538aa650b91afb39f5e2b5f0a771cfd2fc0ae`.
- The push Repository policy check and Snyk PR check had succeeded. The PR
  Repository policy, both `verify` paths, dependency security, Frontend
  quality, Java security, and Semgrep were running or event-inapplicable.
- Clean local host verification passed 753/753 tests; the clean
  container-equivalent path and exact Linux image builder passed 705/705.
  Both achieved 100% bundle line and branch coverage. Exact-image liveness and
  readiness returned `UP` in an egress-disabled production-like smoke.
- Policy tests, actionlint 1.7.12, diff validation, targeted Semgrep, the Sentry
  dependency tree, TruffleHog, and async-profiler ABI checks passed. Independent
  security review reported zero merge blockers.

## Conditional continuation — re-verify live

1. Push this metadata successor without force and wait for every required and
   applicable check on the resulting exact Draft head.
2. Mark PR #71 Ready only if its exact head is current, mergeable, green, and
   has no unresolved conversations or new security signal; then re-check the
   Ready event and exact head.
3. Squash-merge without admin bypass, auto-merge, or source-branch deletion;
   verify the merge SHA, post-merge checks, retained branch, zero deployments,
   and unchanged Draft PR #30.
