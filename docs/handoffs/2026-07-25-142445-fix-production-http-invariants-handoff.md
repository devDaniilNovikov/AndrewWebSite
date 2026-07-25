# fix-production-http-invariants handoff

Signature: HND fix-production-http-invariants [in_progress] topics: backend, security, deploy, tracker → predecessor: 2026-07-25-100022-fix-production-http-invariants-handoff.md

## Durable — safe to cite later

- Scope remains backend, security, and DevOps only. Do not read or modify
  `frontend/**`, `docs/frontend/**`, Draft PR #30, `prompts/**`,
  `receipts/**`, `review-receipts/**`, or `README_V2.md`.
- Preserve the current dirty feature diff. Do not discard, stash, reset, or
  rewrite it. No feature commit, push, PR, Ready transition, merge, or deploy
  exists for this task.
- The current remediation uses Spring Boot 4.1
  `org.springframework.boot.EnvironmentPostProcessor` hooks registered in
  `META-INF/spring.factories`. The intended order is runtime profile, lead
  fingerprint key, then production HTTP invariants, all after config data and
  before application-context initialization.
- The active production HTTP policy rejects a non-servlet application, a
  negative server port, unsafe forwarding/CORS/rate-limit/error/Actuator
  configuration, and relaxed aliases that resolve to the health endpoint.
  A blank health path mapping remains accepted as equivalent to no override,
  while health access remains pinned to `READ_ONLY`.
- Direct `/error` requests are intended to be denied, while a genuine servlet
  `ERROR` dispatch to `/error` remains permitted and redacted. The real-server
  regression test exists but has not yet executed in the current restricted
  sandbox because socket binding is prohibited.
- One review subagent violated its read-only assignment and wrote an
  incomplete backend patch. It was stopped. Its changes were treated as
  untrusted, inspected, and partially corrected; independent security and bug
  reviews still must be re-run against the final exact diff.

## Snapshot at 2026-07-25T14:24:45Z — re-verify live before use

- Branch: `fix-production-http-invariants`.
- HEAD: `e03a7f03691cc7f78d979c999c60a3d231afff5e`.
- Base and live `origin/main` at the last fetch:
  `4da6448495348246198996d2814de275ce7b5de8`.
- Canonical worktree path is
  `/Users/daniilnovikov/.codex/worktrees/production-http-invariants/AndrewWorkWebSite`.
  During this turn it was temporarily moved to
  `/private/tmp/AndrewWorkWebSite-production-http-invariants` because the
  sandbox made the canonical path read-only; it must be returned to the
  canonical path before ending the pause.
- Current tracked modifications:
  `RuntimeProfileGuard`, `LeadFingerprintKeyProfileGuard`, `LeadProperties`,
  `PublicBoundaryDenyFilter`, `SecurityConfiguration`, `application.yml`,
  `RuntimeProfileGuardTest`, and `LeadPropertiesTest`.
- Current untracked feature files:
  `ProductionHttpInvariantGuard.java`, `META-INF/spring.factories`,
  `ForwardHeadersServerContractTest.java`, and
  `ProductionHttpInvariantGuardTest.java`.
- `git diff --check` passed on this snapshot.
- `./mvnw -B -DskipTests compile` passed after correcting the interrupted
  patch's invalid `ManagementPortType` import.
- Focused exact-code evidence:
  - `RuntimeProfileGuardTest`: 8/8 passed.
  - `ProductionHttpInvariantGuardTest`: 51/51 passed, including relaxed
    Actuator IDs, blank mapping, non-web mode, negative port, and early
    `spring.factories` lifecycle.
  - `LeadPropertiesTest`: 5/5 passed after the final fixture/assertion fix.
- `ForwardHeadersServerContractTest` did not execute its four assertions.
  The application reached embedded Tomcat startup and then failed with
  `java.net.SocketException: Operation not permitted`. Classify this run as
  environment-blocked, not as an application defect. The earlier Mockito
  self-attach problem was removed by replacing `@MockitoBean` with a
  deterministic `@TestBean`.
- No current full `./mvnw -B verify`, Docker/Testcontainers, Semgrep,
  TruffleHog, or independent final-review evidence exists after the latest
  changes. The older 249/249 full gate belongs only to the pre-remediation
  diff and is not proof for this snapshot.

## Next steps — resume exactly here

1. Re-fetch and verify `origin/main`, HEAD, worktree location, and the complete
   dirty diff. Confirm that the canonical worktree contains this exact state.
2. Run `ForwardHeadersServerContractTest` outside the restricted socket
   sandbox, or with explicit permission to bind loopback. It must prove:
   health endpoints stay minimal/no-store; direct GET/HEAD/POST `/error`
   return 403; genuine servlet `ERROR` dispatch returns a redacted 500; and
   forwarded headers cannot split the physical-client rate bucket.
3. If that focused test is green, run all focused guard/error/security tests,
   then `./mvnw -B verify` with Docker/Testcontainers. Do not weaken or remove
   any test to accommodate the environment.
4. Inspect and refactor the exact diff before full gates. In particular,
   verify fail-closed handling of invalid endpoint IDs, secret-free startup
   failures, `spring.factories` formatting/registration, filter dispatch
   order, and whether the early fingerprint-key remediation belongs in this
   atomic task.
5. Run `git diff --check`, JaCoCo, Semgrep, TruffleHog, dependency/security
   scans, container build/smoke checks, and the required independent
   security/correctness/operations reviews. Fix every confirmed
   Critical/Important finding, then repeat the exact-head gates.
6. Only after fresh green evidence should the feature be atomically committed
   and proceed through the already authorized Draft-PR/CI/Ready lifecycle.
   Never merge or deploy without a new explicit user command.
