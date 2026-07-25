# fix-leads-unicode-boundary handoff

Signature: HND fix-leads-unicode-boundary [in_progress] topics: backend, security, tracker → predecessor: 2026-07-25-045958-fix-leads-api-json-boundary-replacement-ready-handoff.md

## Durable — safe to cite later

- The task owns the lead request Unicode boundary, focused regressions, and
  its tracker and handoff metadata. It does not own Telegram product
  reconciliation, Timeweb configuration, CI workflow implementation, or
  production deployment.
- Runtime reproduction on the reviewed application confirmed two input
  integrity defects before this branch was created: an isolated UTF-16
  surrogate was accepted and persisted by PostgreSQL as a different value
  from the fingerprinted input, while a NUL in a DB-bound name produced a
  generic availability response instead of an invalid-request response.
- The required behavior is to reject malformed UTF-16 and NUL before HMAC or
  transaction access while preserving valid supplementary Unicode characters.
- The user explicitly authorized the normal branch, Draft PR, CI, review, and
  squash-merge lifecycle for confirmed non-Telegram fixes on 2026-07-25.
  Production deployment and Codex Security remain deferred.

## Snapshot at 2026-07-25T07:18Z — re-verify live before use

- The branch and dedicated worktree were created from live `origin/main`
  `2f565ddb736431cfe862bb247ae8a1fab8f67bca`, the squash merge of PR #40.
- The worktree is
  `/Users/daniilnovikov/.codex/worktrees/leads-unicode-boundary/AndrewWorkWebSite`.
- No production source or test change exists yet. RED regressions, the
  minimal implementation, focused checks, full Maven verification, security
  scans, reviews, commit, push, and Draft PR are pending.

## Next steps — conditional, each requires the stated live check

1. Add focused regressions for malformed surrogate code units and NUL in
   every persisted free-text field, plus positive supplementary-Unicode
   controls; prove RED without weakening existing strict JSON tests.
2. Add the smallest shared validation before fingerprinting and transaction
   access, then run the focused suite and full repository gates.
3. Review the exact diff, commit with Codex attribution, push the task branch,
   open a Draft PR, and use only exact-head CI and review evidence for Ready
   and the already authorized squash merge.
