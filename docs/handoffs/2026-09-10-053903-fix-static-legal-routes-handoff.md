# fix-static-legal-routes handoff

Signature: HND fix-static-legal-routes [draft_pr] topics: frontend, nginx, routing, testing, tracker → predecessor: 2026-09-09-173950-fix-remove-test-notice-handoff.md

## Scope

- User reproduced that footer legal links can land on
  `restholod17.ru:8080/personal-data/` after the latest Timeweb build.
- Branch `fix-static-legal-routes` starts from fresh `origin/main`
  `4dd990d7abdf898cd420365d0ba364a49efa747c` in
  `/Users/daniilnovikov/.codex/worktrees/fix-static-legal-routes/AndrewWorkWebSite`.
- Draft PR #132 targets `main` from `fix-static-legal-routes`.
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

1. Wait for GitHub checks on PR #132 and resolve only task-scope failures.
2. Continue the previously authorized Ready and merge workflow if checks allow.
3. After merge, Timeweb must build from the new `main` commit; direct
   production deployment remains outside this repository change.

## Verification

- `pnpm exec vitest run test/legal-documents.test.tsx --reporter=default`
  with Node 24.14.0 and pnpm 11.18.0: passed.
- `pnpm run lint` with Node 24.14.0 and pnpm 11.18.0: passed.
- `pnpm run typecheck` with Node 24.14.0 and pnpm 11.18.0: passed.
- `pnpm run build:production` with Node 24.14.0 and pnpm 11.18.0: passed.
- `pnpm run verify:export` with Node 24.14.0 and pnpm 11.18.0: passed;
  deterministic export verified.
- Artifact probe confirmed `out/privacy.html`, `out/personal-data.html`, and
  relative homepage hrefs `/privacy` and `/personal-data`; no checked HTML
  contained `restholod17.ru:8080`, `Реквизиты ИП`, or
  `Локальная тестовая отправка`.
- `git diff --check`: passed.
- Full `pnpm test` still has unrelated main-equivalent placeholder and
  production-gate failures. Docker daemon was unavailable, so local Docker
  build was not run.
