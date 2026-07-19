# fix-ci-timeouts-annotations handoff
Signature: HND T4 [active] topics: ci → predecessor: 2026-07-19-193125-fix-tracker-canonical-links-ready-handoff.md

## Durable — safe to cite later

- The user authorized T4 through Ready and selected Codex as the direct owner
  of the narrow CI hardening fix, resolving Q-20260718-002.
- T4 adds bounded timeouts to `verify` and `java-security` and annotates the
  existing immutable action SHAs with their verified release versions.
- Existing action SHAs, permissions, event conditions, required checks, and
  security behavior remain unchanged.
- Node.js 20 deprecation annotations and action upgrades remain out of scope.

## Snapshot at 2026-07-19T20:02Z — re-verify live before use

- `origin/main` is T2 merge `37d02cf0ce8961cb205d9d5598474cd949bd0152`.
- PR #25 is merged and its source branch remains retained at `bc5e63b`.
- Post-merge Repository policy, verify, Java security/CodeQL workflow, and
  Semgrep completed successfully on `main`.
- Draft PR #26 points to head `09be3ab`; merge remains unauthorized.
- Local actionlint, Maven verify, exact-scope, immutable-SHA,
  memory-signature, link, and secret-pattern checks passed.

## Next steps — conditional, each requires the stated live check

1. Wait for all final-head CI and security checks on PR #26.
2. Complete specification, quality, and security review of the workflow.
3. Create a successor Ready handoff and re-run checks on its final metadata head.
4. Mark Ready only after review is clean; merge requires another user command.
