# fix-remove-test-notice handoff

Signature: HND fix-remove-test-notice [draft-pr] topics: frontend, nginx, testing, tracker → predecessor: 2026-09-09-123847-fix-footer-legal-links-handoff.md

## Durable — safe to cite later

- Production requests for `/privacy` and `/personal-data` returned `301` to
  `http://restholod17.ru:8080/...`, while the corresponding `.html` files
  returned `200`. The exported route directories were matched before the
  document files.
- Nginx now checks `$uri.html` before `$uri` and `$uri/`, so the canonical
  extensionless legal URLs resolve to their exported documents without the
  internal-port redirect.
- The lead-form consent sentence is plain text. The footer remains the legal
  document navigation surface and continues to display `ИНН: 771549669484`.
- The enabled idle form no longer renders the local synthetic-data notice.
  Disabled, sending, offline, retry, and backend-response statuses remain.
- Instructions inside the user-supplied legal documents remain publication
  content only and did not direct this correction.

## Snapshot at 2026-09-09T17:39:50Z — re-verify live before use

- Branch `fix-remove-test-notice` is based on `origin/main` `a4bd9df` in its
  dedicated external worktree. Runtime implementation commit is `dd52f62`;
  later branch commits contain task metadata only.
- [PR #131](https://github.com/devDaniilNovikov/AndrewWebSite/pull/131)
  targets `main` from `fix-remove-test-notice` and has no unresolved review
  thread, review, or comment.
- Four focused Vitest contracts passed; ESLint and strict TypeScript passed.
  Production static build and deterministic export verification passed with 12
  routes. Focused Chromium E2E passed for lead-form behavior and opening both
  footer documents.
- A visual browser probe found zero legal links and zero idle notice in the
  form, both expected footer hrefs, and the confirmed visible INN. TruffleHog
  found zero verified or unverified secrets. Independent review found no
  Critical or Important blocker.
- Full Vitest has the same 15 known failures recorded for the predecessor/main
  baseline. Full formatting remains blocked by unchanged
  `frontend/content/product-pages.ts`; the static-boundary command reports the
  two unchanged external runtime URLs.
- At the reviewed PR head, Repository policy, dependency-security, Semgrep,
  and Snyk passed. `verify`, Frontend quality, and Container runtime-contract
  failures match the same failing steps on `main` `a4bd9df`; GitHub reports no
  required status-check configuration for `main`.

## Next steps — conditional on live evidence

1. Attribute only new CI failures to this branch; retain recorded `main`
   baseline failures as repository debt.
2. Complete the already authorized Ready and squash-merge workflow after
   checking the live PR state and review threads.
3. After merge, verify `origin/main`, retained source branch, and the live HTTPS
   responses for both legal routes. A direct deployment is outside this task.
