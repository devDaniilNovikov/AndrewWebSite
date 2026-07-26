# fix-revert-code-review-hardening handoff

Signature: HND fix-revert-code-review-hardening [in_progress] topics: backend, deploy, tracker, process, incident → predecessor: 2026-07-25-205559-fix-code-review-hardening-handoff.md

## Durable — safe to cite later

- The user authorized a non-history-rewriting rollback of merge `0b0a62a`,
  followed by a corrected replacement PR and an updated squash merge into
  `main`. Production deployment was not authorized.
- The code rollback is limited to restoring the prior Docker dependency
  prefetch command and its prior contract assertion. Every later Telegram
  change from PR #47 is preserved.
- The post-merge review found two process defects: tracker/handoff publication
  state was stale, and squash merge `0b0a62a` omitted the required Codex
  attribution footer. Published history is not rewritten; the rollback and
  every replacement merge must carry the footer explicitly.
- A literal six-file revert received an independent 10.0/10.0 review, but a
  separate canonical-process pass found that it deleted append-only handoff
  and lesson records. The final rollback preserves those records, adds this
  successor, and records the incident as LES-20260726-014.

## Snapshot at 2026-07-26T05:54:52Z — re-verify live before use

- Base: `3f35c1d1cad8f007899c8e1064016db440212760`, the fetched
  `origin/main` containing Telegram PR #47 after `0b0a62a`.
- Branch and worktree: `fix-revert-code-review-hardening` at
  `/Users/daniilnovikov/.codex/worktrees/revert-code-review-hardening/AndrewWorkWebSite`.
- The focused container contract test passed 2/2. Full `./mvnw -B verify`
  passed 447/447 tests with PostgreSQL 18.4, Flyway, and JaCoCo.
- TruffleHog found zero secrets, Semgrep found zero findings across 171 rules,
  and `git diff --check` passed for the pre-correction commit.
- Runtime/revert head `bbd23640ab13b3f5a9af6636c691edae6d745ef7`
  was pushed to the retained branch and opened as Draft
  [PR #48](https://github.com/devDaniilNovikov/AndrewWorkWebSite/pull/48).
- The first full canonical-process review scored 8.0/10.0 after finding that
  merged Telegram PR #47 still appeared as `ready` in the tracker and Active
  handoffs. The accepted finding is reconciled by the merged Telegram
  successor handoff; another exact-head review is required.
- The next review scored 8.5/10.0 after finding that the new successor and a
  focused correction to its predecessor were committed together. The accepted
  finding restores the predecessor byte-for-byte and keeps all correction
  facts in this successor, the tracker, and LES-20260726-014.
- The final exact-head reviewer scored `bbd2364` 10.0/10.0 with no actionable
  findings. GitHub CI for the Draft PR is pending.

## Next steps — conditional on live evidence

1. Verify the metadata head on Draft PR #48 and wait for every required
   GitHub check to succeed.
2. Mark Ready only with green required checks and no unresolved review
   findings, then squash-merge under the user's current explicit
   authorization. Preserve the branch and do not deploy.
3. Start the corrected replacement only from the resulting fresh
   `origin/main`.
