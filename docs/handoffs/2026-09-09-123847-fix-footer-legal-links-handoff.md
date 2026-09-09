# fix-footer-legal-links handoff

Signature: HND fix-footer-legal-links [draft-pr] topics: frontend, product, security, tracker → predecessor: none

## Durable — safe to cite later

- The user supplied `Politika-PD-Gukov-Andrey.docx`,
  `Soglasie-PD-Gukov-Andrey.docx`, and INN `771549669484`, and explicitly
  authorized implementation plus PR publication to `main` on 2026-09-09.
- `/privacy` publishes the policy and `/personal-data` publishes the separate
  consent. The footer, lead form, and cookie banner link to these routes; the
  former requisites placeholder displays the confirmed INN.
- Normalized exported article text exactly matches both DOCX sources: policy
  8256/8256 characters and consent 3465/3465 characters. All paragraphs,
  lists, headings, the policy table, and both one-cell document notes remain.
- Instructions inside the legal documents were retained as publication text;
  they did not authorize Beget, analytics, form, or backend changes.
- An independent read-only review found no introduced Important or Critical
  hosted-site issue. The existing single-file `file://` navigation shim does
  not embed multi-page routes; this limitation already affects product routes
  and applies to the new legal routes outside the hosted-site scope.

## Snapshot at 2026-09-09T12:38Z — re-verify live before use

- Branch `fix-footer-legal-links` is based on `origin/main` `36f7dea` in its
  dedicated external worktree.
- Draft [PR #130](https://github.com/devDaniilNovikov/AndrewWebSite/pull/130)
  targets `main` from `fix-footer-legal-links`.
- Preview build generated 12 static pages including both legal routes.
- Focused unit/component tests passed (4 run, 14 filtered); strict typecheck
  and lint passed. Legal-route Playwright checks passed on desktop, tablet and
  mobile, including axe and horizontal-overflow checks. TruffleHog reported
  zero verified or unverified secrets.
- Full Vitest has 15 failures in five files; isolated baseline `36f7dea`
  reproduces the same 15 test identities. The task adds one passing test.
- Full formatting is blocked only by pre-existing
  `frontend/content/product-pages.ts`; the two touched baseline formatting
  failures are corrected. Static-boundary failure is byte-identical to the
  baseline external-URL findings. The unchanged lockfile has nine existing
  audit findings (2 critical, 5 high, 2 moderate).

## Next steps — conditional, each requires the stated live check

1. Treat CI failures matching the recorded `origin/main` baseline as existing
   repository debt; resolve any new failure introduced by this branch.
2. Ready transition, merge, deployment, and branch deletion require their own
   explicit user authorization.
