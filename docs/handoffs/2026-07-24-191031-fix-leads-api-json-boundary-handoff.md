# fix-leads-api-json-boundary handoff

Signature: HND fix-leads-api-json-boundary [draft_pr] topics: backend, security, tracker → predecessor: none

## Durable — safe to cite later

- Feature commit `d5cd296f93871ae73290bb3cde05488a77d4b53b` fixes all
  three Important findings from the post-merge PR #38 review. Non-canonical
  UUID aliases and non-exact `intent` strings now fail at the raw JSON
  boundary before domain conversion.
- Duplicate JSON keys now fail during tree reading through Jackson
  `DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY`.
- Every malformed legitimate or honeypot payload uses the existing generic
  five-field `400 Problem` before validation, HMAC, or transaction access.
  Canonical upper- and lower-case UUIDs, both allowed intents, and the empty
  successful `202` response remain unchanged.
- The strict-boundary incident is distilled as `LES-20260724-011`.
- No OpenAPI, Flyway, frontend, CI workflow, production configuration,
  production service, secret, or user-owned `receipts/` path was changed.
- Ready, merge, and production deploy remain unauthorized.

## Snapshot at 2026-07-24T19:17Z — re-verify live before use

- Draft [PR #39](https://github.com/devDaniilNovikov/AndrewWebSite/pull/39)
  is open against `main`, mergeable, and was at published metadata head
  `6025d731d9bb7c1e3b8637f271d089fe9b6b30c4` before this focused handoff
  correction.
- Fresh focused verification passed 62/62 controller contract tests. Fresh
  `./mvnw -B verify` passed 172/172 tests with PostgreSQL 18.4 Testcontainers,
  Flyway V1, and the JaCoCo gate green (92.16% lines, 81.68% branches).
- Local Semgrep ran 88 Java/security rules with 0 findings. TruffleHog found
  no verified, unverified, or unknown secrets. `git diff --check` passed.
- Independent OpenAPI/HTTP, Jackson/security, and tests/regressions reviews
  reported no Critical, Important, Minor, or other actionable findings.
- Repository policy, Maven verify, dependency-security, Java security,
  CodeQL, Semgrep, and Snyk were green on `6025d73`; the final corrected
  metadata head still requires its own exact-head check verification.

## Next steps — conditional, each requires the stated live check

1. Normally push this focused handoff correction and wait for every required
   and applicable GitHub check on the exact resulting PR head.
2. Keep PR #39 Draft. Mark it Ready or merge only after a separate explicit
   user command and a fresh exact-head/base/review verification.
3. Do not deploy or modify production services/configuration.
