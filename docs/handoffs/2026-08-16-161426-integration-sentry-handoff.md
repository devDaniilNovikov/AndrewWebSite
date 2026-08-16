# integration-sentry implementation handoff

Signature: HND integration-sentry [in_progress] topics: backend, integration, observability, security, ci -> predecessor: none

## Scope and authorization

- The user explicitly requested Sentry integration using branch `integration-sentry` and authorized PR, CI/CD, and merge after successful integration.
- This task owns Sentry backend integration and the minimal exact-branch CI policy exception needed for `integration-sentry` to receive green required checks.
- Production deployment and any secret value entry remain out of scope. Only `SENTRY_DSN` may come from the approved secret store; every other Sentry option is fixed in versioned configuration.

## Live startup evidence

- Repository: `devDaniilNovikov/AndrewWebSite`.
- Base: `origin/main` `c41538aa650b91afb39f5e2b5f0a771cfd2fc0ae`, the squash merge for PR #70.
- Branch/worktree: `integration-sentry` at `/Users/daniilnovikov/.codex/worktrees/integration-sentry/AndrewWorkWebSite`.
- No remote `integration-sentry` branch or PR existed at startup.

## Implementation evidence so far

- Added `io.sentry:sentry-bom` `8.53.0`, `sentry-spring-boot-4`, and runtime `sentry-async-profiler`; automatic Logback forwarding is disabled.
- Sentry is disabled and destination-free outside `prod`. Production requires a hosted-format HTTPS `SENTRY_DSN`, fixes `prod`/`andrew-website`, and enables sanitized errors, logs, metrics, 10% canonical-route tracing, and trace-lifecycle continuous profiling.
- The environment post-processor rejects every external Sentry binding except `SENTRY_DSN`, every unknown versioned `sentry.*` option, and every privacy/sampling override with one generic failure message.
- A pre-start sampler returns zero for every request except the exact lead and health method/path pairs, even with inherited sampling. Unsafe transactions finish under a fixed `untracked` name and are dropped; capture tests assert no transaction, span, profile, or profile-chunk envelope for the unsafe route.
- Error/transaction callbacks use an outbound allowlist: structured exception type and sanitized frames, rebuilt trace/span/profiler identity, one startup INFO log, and one startup counter. Request/user/message/breadcrumb/extras/tags/modules/unknown data, values, source context, baggage, thread metadata, spans, and measurements are removed.
- Telegram clears every preconfigured `RestClient` interceptor so Sentry cannot observe its credential-bearing URI. Tests use in-memory Sentry and OTLP transports and fictional values only.
- The final runtime moved from musl Alpine to glibc Noble for async-profiler and enables native access; the process exits after a sanitized production startup failure so profiler threads cannot keep a failed process alive.
- Added exact, one-time CI/Git Flow support for `integration-sentry`; the normal `main|task-*|fix-*` allowlist remains unchanged.

## Verification evidence

- Clean host lifecycle: `./mvnw -B clean verify` — 753 tests, zero failures/errors, 100% bundle line and branch coverage.
- Clean container-equivalent lifecycle: `./mvnw -B clean -DexcludedGroups=database verify` — 705 tests, zero failures/errors, 100% bundle line and branch coverage.
- Exact `andrew-website:integration-sentry` image build — 705 tests and coverage gate passed inside the Linux builder. The final image is non-root UID/GID `10001`, Ubuntu Noble with glibc 2.39, and uses `--enable-native-access=ALL-UNNAMED`.
- The final runtime resolved every dependency of the packaged Linux ARM64 `libasyncProfiler.so`; the builder integration test also rejected a no-op profiler implementation.
- Production-like exact-image smoke used PostgreSQL 18 and fictional bindings inside an egress-disabled Docker network with the Sentry ingest host pinned to loopback; liveness and readiness both returned `UP`. The temporary containers and network were removed afterward.
- CI policy script, official actionlint 1.7.12, `git diff --check`, the Sentry dependency tree, and targeted Semgrep scans passed. TruffleHog found zero verified secrets; its only two unverified matches were explicit fictional JDBC test fixtures.
- Independent security review reported zero merge blockers. It confirmed the unsafe-route profiler boundary, configuration-source allowlist, payload sanitizer, DSN source policy, OTLP test isolation, glibc runtime, and trace metadata hardening.
- Repository tests cannot authenticate to the private Sentry tenant. Production release remains blocked until an authorized operator verifies that the injected DSN resolves to `rogaandkopyta-pz/java-spring-boot-q1` and runs the documented non-PII canary.
- The supported runtime uses only the standard enumerable Spring configuration sources; JNDI or a custom non-enumerable `PropertySource` is prohibited until the fail-closed guard is extended and reviewed.

## Next checks

1. Commit with Codex attribution, push, and open the Draft PR.
2. Reconcile the tracker/handoff at each publication state and require exact-head GitHub CI/security success.
3. Mark Ready only after the exact head is green, then perform the explicitly authorized squash merge while retaining the source branch.
