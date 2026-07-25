# fix-leads-api-json-boundary-replacement handoff

Signature: HND fix-leads-api-json-boundary-replacement [draft_pr] topics: backend, security → predecessor: none

## Durable — safe to cite later

- Post-merge review of PR #38 found three Important strict-boundary defects:
  Jackson UUID conversion accepted non-canonical encodings, enum conversion
  accepted non-exact strings, and tree parsing silently retained one duplicate
  JSON key. The public successful response, OpenAPI, and Flyway schema did not
  need to change.
- Draft PR #39 implemented the initial fix, but two published commits lacked
  the required Codex attribution. The user explicitly selected a
  policy-compliant replacement instead of rewriting published history.
  Branch `fix-leads-api-json-boundary` and its worktree remain preserved.
- Replacement branch `fix-leads-api-json-boundary-replacement` started from
  fresh `origin/main` `731a17dbba5503b7a3ea94ac32ff9567f490d443`.
  Its task-start commit is `6a1388128ceb01ed9d03f9d03ad4deeef983800a`;
  its atomic feature commit is
  `76906b9af634cc729e29185ad3917fca8265aa76`. Both contain
  `Co-Authored-By: Codex <noreply@openai.com>`.
- The replacement rejects UUID Base64 forms, surrounding whitespace, and the
  JDK short-component alias; accepts both UUID hex cases without version or
  variant restrictions; accepts only exact `repair` or `maintenance`; and
  rejects duplicate known keys in both orderings before honeypot
  classification or transaction access.
- The no-version/variant restriction is intentional and has a positive
  all-zero UUID control. A review suggestion to add version/variant
  restrictions was rejected because it contradicted the approved task.
- Three independent final reviews — HTTP/OpenAPI, Jackson/security, and
  tests/regressions/metadata — report no unresolved Critical, Important, or
  actionable Minor finding.

## Snapshot at 2026-07-25T04:27Z — re-verify live before use

- Draft [PR #40](https://github.com/devDaniilNovikov/AndrewWebSite/pull/40)
  targets `main`; its pre-metadata head is
  `76906b9af634cc729e29185ad3917fca8265aa76` and its base is
  `731a17dbba5503b7a3ea94ac32ff9567f490d443`.
- TDD evidence: the focused suite first ran 66 tests with 16 expected
  failures, then passed 67/67 after the fix and added regression control.
  `./mvnw -B verify` passed 177/177 tests with PostgreSQL 18.4 Testcontainers,
  Flyway V1, and the JaCoCo gate. Coverage was 92.16% lines and 81.68%
  branches.
- Semgrep ran 88 rules over 33 Java files with zero findings. TruffleHog
  reported zero verified or unverified findings. `git diff --check` passed.
- Draft [PR #39](https://github.com/devDaniilNovikov/AndrewWebSite/pull/39)
  remains open at `a6dbbe5c07333fde9aaf05c7530bb58e19858918` until every
  applicable check passes on the replacement's exact metadata head. Its
  untracked `.claude/reviews/pr-39-review.md` remains untouched.
- This handoff metadata commit creates a new PR #40 head. No check result from
  `76906b9` may be reused as exact-head CI evidence.
- Frontend, Draft PR #30, CI workflows, OpenAPI, Flyway V1, production
  services/configuration, secrets, and user-owned receipt files remain
  untouched.

## Next steps — conditional, each requires the stated live check

1. Commit and normally push this metadata checkpoint, then wait for every
   required and applicable GitHub check on its exact SHA.
2. Confirm PR #40 remains Draft, targets `main`, is mergeable, has no new
   review finding, and still matches the locally reviewed tree.
3. After exact-head gates pass, comment on PR #39 that PR #40 supersedes it
   and close PR #39 unmerged. Do not delete or rewrite its branch.
4. Leave PR #40 Draft. Ready, merge, and production deployment require
   separate explicit user commands.
