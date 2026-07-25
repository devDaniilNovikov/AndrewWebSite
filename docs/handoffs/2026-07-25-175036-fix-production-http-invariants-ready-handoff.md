# fix-production-http-invariants ready handoff

Signature: HND fix-production-http-invariants [ready] topics: backend, security, deploy, tracker → predecessor: 2026-07-25-142445-fix-production-http-invariants-handoff.md

## Durable — safe to cite later

- The task is confined to backend, security, DevOps, and its tracker
  metadata. No frontend, prompt, receipt, or production resource is part of
  the change.
- Production startup now fails before application-context initialization
  when the runtime profile, fingerprint key, server binding, forwarded-header
  handling, rate limiter, error response, Actuator exposure, management port,
  health cache, or probe contracts drift from the public boundary.
- Direct `/error` requests are denied while genuine servlet `ERROR`
  dispatches remain available and redacted. The embedded-Tomcat contract also
  proves matrix-parameter variants remain closed and cloud detection cannot
  make forwarded headers split the physical-client rate bucket.
- The runtime image uses the Alpine Temurin JRE, retains the non-root user and
  liveness healthcheck, and no longer contains the vulnerable `pebble`
  package inherited from the previous runtime image.
- Three independent read-only correctness, security, and operations reviews
  found no unresolved Critical or Important defect. No test, security
  control, review gate, or production boundary was weakened.
- The user explicitly authorized merge into `main` after green exact-head CI.
  Production deployment remains unauthorized.

## Snapshot at 2026-07-25T17:50:36Z — re-verify live before use

- Draft PR #42 is open and mergeable against `main`
  `4da6448495348246198996d2814de275ce7b5de8`.
- Exact implementation head
  `be64c2f0b193d4d47da562bbca9c549c37133f15` passed:
  - local `./mvnw -B verify`: 265 tests, PostgreSQL/Testcontainers/Flyway,
    and JaCoCo;
  - Docker build-stage verify: 249 non-database tests and JaCoCo;
  - isolated final-image PostgreSQL 18/Flyway smoke: readiness `UP` with
    `no-store`, direct `/error` GET/HEAD/POST `403`, and UID `10001`;
  - Docker Scout: zero Critical/High findings and no vulnerable packages;
  - Semgrep: 132 rules over 41 targets with zero findings;
  - TruffleHog: zero verified or unverified secrets;
  - live GitHub Dependabot, Code Scanning, and Secret Scanning alerts: zero
    open;
  - Repository policy, both verify paths, dependency-security,
    java-security, CodeQL, Semgrep, and Snyk.
- Event-specific jobs skipped only where their workflow conditions did not
  apply.
- The local branch and remote branch matched at `be64c2f`, and the worktree
  was clean before this metadata-only Ready commit.
- This handoff changes the PR head. Its new exact head and every required
  check must be refreshed before Ready transition or merge.

## Next steps — conditional on live evidence

1. Commit and push only this Ready metadata, then wait for every check on the
   new exact head.
2. Reconfirm the PR diff against the reviewed implementation plus this
   metadata, unchanged `main`, mergeability, and zero open security alerts.
3. Publish the final review record for the exact head, mark PR #42 Ready, and
   perform the user-authorized squash merge.
4. Fetch and verify the resulting `main` commit and post-merge checks. Retain
   the source branch and worktree. Do not deploy.
