# fix-ci-timeouts-annotations ready handoff
Signature: HND T4 [ready] topics: ci → predecessor: 2026-07-19-194032-fix-ci-timeouts-annotations-handoff.md

## Durable — safe to cite later

- T4 adds `timeout-minutes: 20` to `verify` and `timeout-minutes: 30` to
  `java-security`.
- Existing immutable action SHAs are unchanged and now carry verified
  version comments for checkout v4.3.1, setup-java v4.8.0, and CodeQL
  v3.37.1.
- Workflow permissions, triggers, conditions, required checks, and security
  behavior remain unchanged. Node.js deprecation cleanup and action upgrades
  remain out of scope.
- Q-20260718-002 is resolved by the user's direct Codex ownership decision.
- Independent review found no workflow issue. Its only finding was a stale
  PR-head claim in the predecessor handoff; that wording was corrected.

## Snapshot at 2026-07-19T19:51Z — re-verify live before use

- `origin/main` remains T2 merge
  `37d02cf0ce8961cb205d9d5598474cd949bd0152`.
- Draft PR #26 is mergeable at reviewed pre-ready head `ef0ff32`.
- Repository policy, verify, dependency-security, Java security/CodeQL,
  Semgrep, and Snyk passed on the reviewed pre-ready head.
- Local actionlint v1.7.12 passed after its official archive checksum was
  verified. `./mvnw -B verify` passed with 32 tests and the JaCoCo gate.
- Exact-scope, immutable-SHA, tag-to-SHA, memory-signature, link,
  credential-pattern, and `git diff --check` checks passed.

## Next steps — conditional, each requires the stated live check

1. Verify fresh checks on the final metadata head, then remove Draft only if
   the base, mergeability, conversations, and branch protection remain valid.
2. Merge only after a separate explicit user command.
3. After merge, reconcile T4 to `merged` in the separately authorized T6
   branch, close this handoff chain, and retain the T4 source branch.
4. Do not start T6 before the T4 squash merge reaches `origin/main`.
