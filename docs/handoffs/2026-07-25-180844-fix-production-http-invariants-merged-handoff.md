# fix-production-http-invariants merged handoff

Signature: HND fix-production-http-invariants [merged] topics: backend, security, deploy, tracker → predecessor: 2026-07-25-175036-fix-production-http-invariants-ready-handoff.md

## Durable — safe to cite later

- PR #42 was squash-merged into `main` as
  `d29e7880b296592ebb293f7a3f624f69be072baa` after the user explicitly
  authorized merge on green exact-head CI.
- The merge tree exactly matched the reviewed source-branch tree. The source
  branch and canonical worktree were retained.
- Production startup now fails closed before application-context
  initialization when the runtime profile, fingerprint key, HTTP server,
  forwarding, rate-limit, error, Actuator, management-port, health-cache, or
  probe configuration violates the public boundary.
- Direct requests to `/error` are denied while genuine internal servlet error
  dispatches remain redacted. The runtime container remains non-root and uses
  the Alpine Temurin JRE.
- No production deployment occurred or was authorized.

## Snapshot at 2026-07-25T18:08:44Z — re-verify live before use

- Live `origin/main` and the PR merge commit both resolve to
  `d29e7880b296592ebb293f7a3f624f69be072baa`.
- Post-merge CI completed successfully:
  - Repository policy;
  - Maven verify;
  - Java security;
  - Dependency Submission;
  - Semgrep.
- The push-only dependency-security job skipped as designed; the corresponding
  pull-request gate had already passed on the exact reviewed head.
- Live open-alert counts were zero for Dependabot, Code Scanning, and Secret
  Scanning.
- The source branch remained available remotely and the worktree was clean
  before the local CHANGELOG and merged-state reconciliation changes.

## Follow-up integration state

- This merged-state reconciliation and the new root `CHANGELOG.md` currently
  exist only in the retained task worktree. They require a normal reviewable
  integration before they become part of `main`.
- Do not deploy as part of that documentation-only integration.
