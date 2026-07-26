# fix-http-security-framework-native-deny verified local handoff

Signature: HND fix-http-security-framework-native-deny [in_progress] topics: backend, security, tracker → predecessor: 2026-07-26-173224-fix-http-security-framework-native-deny-handoff.md

## Durable — safe to cite later

- `PublicBoundaryDenyFilter` is removed. The public boundary now uses only
  Spring Security 7.1-native authorization matchers: exact
  `RegexRequestMatcher` instances, namespace `PathPatternRequestMatcher`
  instances, an exact ERROR-dispatch composition, method-only static
  GET/HEAD matchers, and final deny-all.
- The exact `POST /api/leads` matcher is shared with the CSRF exclusion.
  Exact route patterns accept query strings and do not accept trailing
  slashes, descendants, or matrix-parameter variants.
- Spring CORS is enabled only when the `local` configuration source exists.
  It is disabled in non-local profiles, so closed preflights reach namespace
  authorization and return `403` without an allow-origin header.
- The existing public routes, closed API and actuator namespaces, internal
  ERROR dispatch, static GET/HEAD fallback, statelessness, body and rate
  filters, health boundary, exact status-only health bodies, and `no-store`
  handling remain intact.
- No public API, dependency, OpenAPI, configuration key, actuator exposure,
  frontend, workflow, Flyway, Docker, infrastructure, or production setting
  changed. No real credential or lead data was used.
- Push, Draft PR publication, Ready transition, merge, production
  configuration, and deployment remain unauthorized under the current plan.

## Snapshot at 2026-07-26T17:48:52Z — re-verify live before use

- Fresh `origin/main` remains
  `27e6bb4f6991e0bef8ef9ae2bec48feb92c4aaec`. The isolated local branch is
  two commits ahead before this metadata commit:
  - `3644fd8` reconciles the task start;
  - `3d5e382` implements and tests the native security boundary.
- No remote `fix-http-security-framework-native-deny` branch and no pull
  request exist. The stale root workspace and retained task worktrees were
  not modified.
- TDD evidence:
  - the direct `FilterChainProxy` RED ran 15 tests and failed only because
    non-local preflight returned the reproduced `200` instead of `403`;
  - the minimal GREEN passed 15/15;
  - the expanded security/CORS/health suite passed 59/59, and the final
    `SecurityContractTest` passed 24/24 after adding near-collision coverage.
- `./mvnw -B clean verify` passed 640/640 tests with PostgreSQL 18.4,
  Testcontainers, and Flyway.
- `./mvnw -B -DexcludedGroups=database clean verify` passed 596/596 tests and
  exactly matches the Docker builder's container-equivalent verification
  command.
- The final JaCoCo report has 1665/1665 covered lines and 782/782 covered
  branches; both enforced 100% gates passed.
- Dependency inspection retained Spring Boot Security 4.1.0 and Spring
  Security 7.1.0; no manifest changed. `git diff --check` is clean and the
  diff contains only owned implementation/tests and task metadata.
- Semgrep ran 88 Java/security rules over the three remaining changed Java
  files with zero findings. Exact changed-file TruffleHog scans found zero
  verified or unverified secrets. A broader history scan included the
  inherited `27e6bb4` commit and reported only its two pre-existing fictional
  embedded-credential URL fixtures, not any task change.
- Final specification, correctness, and security reviews found no unresolved
  Critical or Important issue. The worktree was clean at implementation head
  before this metadata-only handoff.

## Next steps — each remains a separate gate

1. Resolve the commit containing this handoff and re-check `origin/main`,
   worktree cleanliness, the exact diff, and authorization.
2. Obtain explicit current authorization before push or Draft PR
   publication.
3. After any publication, wait for exact-head CI/security checks and refresh
   the review record before separately requesting Ready and merge
   authorization.
4. Do not configure production or deploy from this task.
