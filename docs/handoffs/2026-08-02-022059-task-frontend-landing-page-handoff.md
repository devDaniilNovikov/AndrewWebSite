# task-frontend-landing-page startup handoff

Signature: HND task-frontend-landing-page [in_progress] topics: frontend, design, accessibility, testing, tracker → predecessor: `2026-08-01-191251-task-frontend-ci-gates-ready-handoff.md`

## Durable — safe to cite later

- F1 merged as `d0346b716772f2d1a3debe5692c604950c4b143f` and F1A
  merged as `b4a5e1ca360748d25087267771a700353b54bf14`; F2 is now the
  next eligible preview task.
- The user rejected the earlier 439 by 1280 mockup and supplied the corrected
  724 by 2172 visual reference. Its canonical SHA-256 is
  `6ee924016b146c528c1f72118aab7f5a0260d15369af07e0da9892308b76ecbc`.
- The user's later decision removes all reference photography from the Git
  tree. The supplied JPEG remains an external local design reference only;
  generated comparison screenshots also stay outside the repository.
- F2 follows the corrected reference's full composition while treating its
  phone, prices, logo, photos, cases, reviews, staff identities, hours, legal
  copy, and other unverified facts as visibly labelled placeholders.
- The form remains a non-submitting shell until F5. F2 adds no API request,
  analytics, external font/media request, browser persistence, or production
  content and preserves the static-export and production-readiness gates.
- The user authorized implementation, push, and one Draft PR only. Ready,
  merge, deployment, branch deletion, and any PR #30 change remain excluded.

## Snapshot at 2026-08-02T02:20:59Z — re-verify live before use

- Fresh `origin/main` and branch `task-frontend-landing-page` both pointed to
  `b4a5e1ca360748d25087267771a700353b54bf14`. Dedicated worktree:
  `/Users/daniilnovikov/.codex/worktrees/frontend-landing-page/AndrewWorkWebSite`.
- No F2 branch, Issue, PR, or parallel worktree existed before startup.
- PR #62 was merged, Issue #61 was closed, both F1A source refs remained, and
  post-merge CI, Dependency Submission, and Semgrep were green.
- PR #30 remained open Draft at `00f55eea452884dc4c286ae17fcb6e1d4ebc25d8`
  and is outside F2.

## Conditional continuation — re-verify live

1. Commit this reconciliation and the external-reference contract without
   publishing any source photography.
2. Use RED → GREEN → REFACTOR for the landing behavior, then prove WCAG A/AA,
   keyboard drawer, anchors, reduced motion, responsive layout, placeholders,
   deterministic static export, and network isolation.
3. Run the full frontend and backend gates plus independent specification,
   code, accessibility, and security reviews. Publish only a Draft PR after
   the final exact local head is clean; stop before Ready or merge.
