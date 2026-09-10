# fix-static-legal-routes handoff

Signature: HND fix-static-legal-routes [in_progress] topics: frontend, nginx, routing, testing, tracker → predecessor: 2026-09-09-173950-fix-remove-test-notice-handoff.md

## Scope

- User reproduced that footer legal links can land on
  `restholod17.ru:8080/personal-data/` after the latest Timeweb build.
- Branch `fix-static-legal-routes` starts from fresh `origin/main`
  `4dd990d7abdf898cd420365d0ba364a49efa747c` in
  `/Users/daniilnovikov/.codex/worktrees/fix-static-legal-routes/AndrewWorkWebSite`.
- Owned paths are `Dockerfile`, the legal-document unit test, this task note,
  `TASKS.md`, and `.agents/memory/HANDOFFS.md`.

## Current findings

- Live homepage links are relative `/privacy` and `/personal-data`, so the
  visible `:8080` symptom is consistent with server-side redirect behavior
  rather than bad anchor values in the footer.
- Nginx route fallback on `origin/main` checks `$uri` before a route's
  `index.html`, allowing directory handling to win before the intended static
  document fallback.

## Changes

- `Dockerfile` now disables Nginx absolute redirects and port injection in
  redirects.
- `Dockerfile` installs the frontend's declared `pnpm` version so Timeweb
  builds do not drift past `frontend/.npmrc` engine checks.
- `Dockerfile` maps `/privacy/` and `/personal-data/` directly to the exported
  HTML files, then serves `$uri.html` and `$uri/index.html` before falling back
  to `$uri` and finally `/index.html`.
- `frontend/test/legal-documents.test.tsx` pins the redirect-safe Nginx
  fallback contract.

## Next steps

1. Run focused frontend checks for the legal route contract.
2. Commit, push, open PR to `main`, and continue the previously authorized
   merge workflow.
3. After merge, Timeweb must build from the new `main` commit; direct
   production deployment remains outside this repository change.
