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

## Snapshot at 2026-07-19T19:40Z — re-verify live before use

- `origin/main` is T2 merge `37d02cf0ce8961cb205d9d5598474cd949bd0152`.
- PR #25 is merged and its source branch remains retained at `bc5e63b`.
- Post-merge Repository policy, verify, Java security/CodeQL workflow, and
  Semgrep completed successfully on `main`.
- `TASKS.md` records T4 as `in_progress`; no T4 PR exists yet.

## Next steps — conditional, each requires the stated live check

1. Run actionlint, Maven verify, immutable-SHA and exact-diff checks.
2. Complete specification, quality, and security review of the workflow.
3. Push and open a Draft PR; wait for all final-head CI and security checks.
4. Mark Ready only after review is clean; merge requires another user command.
