# fix-tracker-canonical-links ready handoff
Signature: HND T2 [ready] topics: tracker → predecessor: 2026-07-19-192146-fix-tracker-canonical-links-handoff.md

## Durable — safe to cite later

- T2 reconciles the living tracker with the authorized squash merge of PR
  #24 and records the approved T2 → T4 → T6 → T5 non-stacked sequence.
- New tracker links use `devDaniilNovikov/AndrewWebSite`. The former redirect
  in the closed CI handoff remains immutable; its canonical replacement is
  recorded in the predecessor handoff.
- `task-backend-deploy-stub` and all backend implementation remain blocked.
- Specification, quality, and security review found no Critical or Important
  findings. The complete diff contains only tracker and handoff files.

## Snapshot at 2026-07-19T19:31Z — re-verify live before use

- `origin/main` remains `66e2afa9d34bf96623ad1e09beb3661341c428cf`.
- Draft PR #25 is mergeable and clean at reviewed head `f5c2747`.
- Repository policy, verify, dependency-security, Java security/CodeQL,
  Semgrep, and Snyk passed on the reviewed head.
- Local `./mvnw -B verify` passed with 32 tests and the JaCoCo gate.
- Link, memory count/signature, symlink, conflict-marker, canonical slug,
  credential-pattern, scope, attribution, and `git diff --check` passed.
- User-owned untracked `receipts/` and `review-receipts/` remain outside the
  task diff.

## Next steps — conditional, each requires the stated live check

1. Verify fresh checks on the final metadata head, then remove Draft if the
   base, mergeability, conversations, and branch protection remain valid.
2. Merge only after a separate explicit user command.
3. After merge, reconcile T2 to `merged` in the separately authorized T4
   branch, close this handoff chain, and retain the T2 source branch.
4. Do not start T4 before T2 reaches `origin/main`.
