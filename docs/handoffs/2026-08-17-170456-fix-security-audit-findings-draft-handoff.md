# fix-security-audit-findings draft handoff

Signature: HND fix-security-audit-findings [draft_pr] topics: backend, frontend, container, ci, security -> predecessor: 2026-08-17-155333-fix-security-audit-findings-handoff.md

## Durable — safe to cite later

- All four findings from the read-only audit are remediated in one branch:
  `SEC-RATE-01`, `FE-CSP-01`, `SC-IMG-01`, and `CI-AGENT-01`.
- The public HTTP perimeter now has a coarse per-instance admission gate before
  media, body, routing, and authorization work. Production health runs on the
  distinct loopback-only management connector `127.0.0.1:8081`; the public
  listener remains exactly `0.0.0.0:8080`.
- Hosted static HTML receives deterministic per-document CSP response headers
  with exact SHA-256 hashes. The offline standalone file has a strict hash CSP
  meta policy and is download-only with a blocking CSP at the hosted boundary.
- Both Temurin stages use immutable multi-platform OCI index digests. The
  runtime has no package-manager install step or curl, and CI performs a fresh
  container build plus runtime-image inspection.
- The custom Jules issue route treats the issue as a numeric authorization
  handle only. A reviewed manifest from the event-time `main` SHA supplies the
  task, the provider source is discovered and validated, plan approval is
  mandatory, and issue title/body/comments never enter the provider request.
- The Jules v1alpha API exposes no provider-enforced per-session tool/path
  allowlist. This limitation is documented honestly; the trusted manifest,
  mandatory plan approval, least-privilege token, normal PR review, and CI are
  compensating controls rather than a claimed hard sandbox.

## Fresh local evidence at 2026-08-17T17:04:56Z

- `./mvnw -B -DexcludedGroups=database clean verify`: PASS, 717 tests, zero
  failures/errors/skips, all JaCoCo line and branch thresholds met.
- `corepack pnpm run verify` on Node `24.14.0`: PASS after the final CSP
  fixture cleanup, 210 Vitest tests; 36
  applicable Playwright tests with 30 expected viewport skips; deterministic
  export; standalone Chromium with zero CSP violations or external requests;
  Lighthouse medians 96/100/100; package audit found no known vulnerabilities.
- `bash .github/scripts/test-ci-paths.sh`: PASS, including functional malicious
  Jules fixtures and the real container-build job contract.
- actionlint `1.7.12` with the verified upstream Darwin archive digest: PASS
  for every workflow.
- TruffleHog `3.96.0` filesystem scan: 0 verified and 0 unverified secrets
  after replacing a credential-shaped negative-test URL with a username-only
  loopback fixture. No ignore or suppression was added.
- Temurin tag lookups still resolve to the two committed OCI index digests.
  The local Docker daemon is unavailable, so the actual image build is not
  reported as local evidence; the new required CI job owns that proof.
- Independent backend/security review reported no remaining finding. Its one
  intermediate concern — a management bypass based on port alone — is already
  closed by the loopback local-address condition and regression test.
- `git diff --check`, shell syntax checks, and focused RED/GREEN regression
  suites pass. No production request, deployment, secret access, or DAST ran.

## Conditional continuation — re-verify live

1. Commit the explicit task paths with Codex attribution, push
   `fix-security-audit-findings`, and open one Draft PR with no auto-merge.
2. Require every check on the exact PR head to finish successfully, including
   Repository policy, `verify`, dependency-security, Frontend quality,
   Container build, and all security integrations that run.
3. Resolve every review conversation, create a Ready successor handoff, and
   push that metadata checkpoint. Mark Ready and squash-merge only if the new
   exact head is fully green and branch protection remains intact.
4. Preserve the remote source branch, confirm zero deployments, and do not
   mutate production.
