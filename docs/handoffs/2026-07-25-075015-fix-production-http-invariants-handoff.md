# fix-production-http-invariants handoff

Signature: HND fix-production-http-invariants [in_progress] topics: backend, security, deploy, tracker → predecessor: none

## Durable — safe to cite later

- This task owns fail-fast production invariants for forwarded-header
  processing, the exact public Actuator web surface, and the enabled
  application rate limiter, together with focused startup and web
  regressions.
- The canonical backend contracts require forwarded headers to remain
  ignored until Timeweb proxy behavior and trusted CIDRs are verified, and
  require startup failure for unsafe public Actuator exposure.
- Spring Boot 4.1 can default forwarded-header processing to native handling
  on supported cloud platforms. Its externalized management properties can
  relocate Actuator endpoints, expose a wildcard set, or move them to a
  separate management server unless the application validates the effective
  production configuration.
- The fix must preserve the exact anonymous liveness/readiness paths, the
  closed `/actuator` namespace, static GET/HEAD behavior, and existing local
  and test flexibility. It must not add trusted-proxy behavior.
- This task does not own Timeweb configuration, CI workflows, Telegram,
  readiness dependency semantics, or production deployment.

## Snapshot at 2026-07-25T07:50:15Z — re-verify live before use

- The dedicated branch and worktree were created from live `origin/main`
  `4da6448495348246198996d2814de275ce7b5de8`, the squash merge of PR #41.
- PR #41 and its post-merge checks were verified live. The retained source
  branch remains at `25dbc14352d76f8fe30e57d379a4a3e17f9fd347`.
- Official Spring Boot 4.1 documentation confirms
  `server.forward-headers-strategy=none` ignores forwarded headers, while
  management base path, exposure, and server-port properties can change the
  web management surface.
- No production source or test change exists yet. RED regressions,
  implementation, focused and full gates, security scans, reviews, commit,
  push, and Draft PR are pending.

## Next steps — conditional, each requires the stated live check

1. Define the complete hostile-property matrix against the effective `prod`
   environment, including forwarded strategy, rate limiter, Actuator base
   path/exposure/port, and endpoint path mappings.
2. Prove RED with startup-context regressions and a real embedded-servlet
   forwarded-header test where required.
3. Add the smallest production-only fail-fast guard plus an explicit safe
   common forwarded-header default; preserve local/test overrides needed by
   focused tests.
4. Run focused and full gates, independent review, security scans, and the
   already authorized Draft PR/CI/Ready/squash-merge lifecycle. Do not
   deploy.
