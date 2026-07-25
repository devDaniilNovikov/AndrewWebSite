# fix-leads-unicode-boundary draft handoff

Signature: HND fix-leads-unicode-boundary [draft_pr] topics: backend, security, tracker → predecessor: 2026-07-25-071808-fix-leads-unicode-boundary-handoff.md

## Durable — safe to cite later

- The lead normalization boundary now rejects NUL and isolated UTF-16
  surrogate code units in every database-bound string before fingerprinting
  or transaction access.
- Valid supplementary Unicode characters remain accepted, normalized,
  fingerprinted, and persisted without replacement or truncation.
- Raw JSON contract regressions require the generic invalid-request Problem
  response and no transaction access for NUL, lone high-surrogate, and lone
  low-surrogate input in name, comment, and source path.
- PostgreSQL integration regressions require NUL and malformed UTF-16 to
  create no rows, while a valid supplementary character round-trips exactly
  and equivalent replay remains idempotent.
- This task does not change Telegram, OpenAPI, migrations, frontend, CI
  workflows, deployment, or production.

## Snapshot at 2026-07-25T07:35:43Z — re-verify live before use

- Draft PR #41 is open from `fix-leads-unicode-boundary` to `main`.
- The published feature head is
  `fe856d984bea14ba476251e9e61966c1f813adb9`; live `origin/main` is
  `2f565ddb736431cfe862bb247ae8a1fab8f67bca`.
- Focused tests passed, including PostgreSQL 18.4. Full
  `./mvnw -B verify` passed 197 tests and the JaCoCo gate.
- Local Semgrep completed with zero findings across 89 rules and 124 tracked
  targets. TruffleHog reported zero verified, unverified, or unknown results.
- Independent GPT-5.6 Sol Ultra review found zero Critical, Important, or
  Minor findings in the exact source and test diff.
- GitHub exact-head PR checks are still running. Draft status, checks,
  reviews, mergeability, and head must be refreshed live before any state
  transition.

## Next steps — conditional, each requires the stated live check

1. Publish this metadata commit and wait for every exact-head PR check to
   reach a terminal expected state.
2. Review the final whole diff and exact-head checks; fix every actionable
   finding and repeat affected gates.
3. Record Ready metadata only after exact-head checks and reviews are green,
   mark PR #41 Ready, then use the user's existing authorization for a guarded
   squash merge.
4. Verify the resulting `main` commit and post-merge checks. Do not deploy.
