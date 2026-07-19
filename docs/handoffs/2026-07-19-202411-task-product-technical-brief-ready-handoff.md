# task-product-technical-brief ready handoff

Signature: HND T6 [ready] topics: product → predecessor: 2026-07-19-201720-task-product-technical-brief-handoff.md

## Durable — safe to cite later

- `docs/product/technical-brief.ru.md` is the normalized canonical product
  source. Confirmed requirements, recommendations, assumptions, and open
  questions retain distinct status.
- Product UX belongs to the brief; HTTP wire behavior and backend delivery
  semantics remain canonical in OpenAPI and the backend architecture.
- No actual phone, account, legal requisite, credential, or other private
  client value was introduced. Missing product facts remain explicit open
  questions and launch blockers.
- T5 remains a separate task and must start only after an explicitly
  authorized T6 merge reaches fresh `origin/main`.

## Snapshot at 2026-07-19T20:24Z — re-verify live before use

- Draft PR #27 is mergeable against base
  `326c9f42eea9e1e2d6b8ff1ee189e2b6f0faefd2`.
- Repository policy, verify, dependency-security, CodeQL, Semgrep, and Snyk
  passed on reviewed pre-ready head `4278866`.
- `./mvnw -B verify` passed with 32 tests and the JaCoCo gate.
- Source-status, export-artifact, credential-pattern, PII-pattern, link,
  heading, contract, scope, secret, and whole-diff reviews passed.

## Next steps — conditional, each requires the stated live check

1. Verify all required checks on the final metadata head, then remove Draft
   only if head/base, mergeability, conversations, and protection remain
   valid.
2. Merge only after a separate explicit user command; squash only and retain
   `task-product-technical-brief`.
3. After merge, create `task-frontend-track-planning` from fresh
   `origin/main`; do not stack it on PR #27.
