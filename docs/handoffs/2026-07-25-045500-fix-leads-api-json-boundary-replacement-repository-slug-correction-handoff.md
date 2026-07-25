# fix-leads-api-json-boundary-replacement repository-slug correction handoff

Signature: HND fix-leads-api-json-boundary-replacement [draft_pr] topics: backend, security, tracker → predecessor: 2026-07-25-042750-fix-leads-api-json-boundary-replacement-handoff.md

## Correction at 2026-07-25T04:55:00Z — verified live

- The predecessor snapshot used the retired repository slug
  `devDaniilNovikov/AndrewWorkWebSite`. GitHub's live REST API returns 404
  for that slug; the canonical public repository is
  [`devDaniilNovikov/AndrewWebSite`](https://github.com/devDaniilNovikov/AndrewWebSite).
  The active worktree's `origin` uses that canonical URL.
- Draft [PR #40](https://github.com/devDaniilNovikov/AndrewWebSite/pull/40)
  is open and mergeable with base `main`
  `731a17dbba5503b7a3ea94ac32ff9567f490d443` and exact head
  `848a1905c7b47e5af8ed9aa7d3f33524ebabd084`.
- On that exact head, Repository policy, both verify paths,
  dependency-security, java-security, CodeQL, Semgrep, and the Snyk legacy
  status succeeded. Event-specific complementary jobs and the disabled PR
  relay were skipped as designed. The owner submitted a `COMMENTED` final
  review on the same head.
- The live open-alert queries for Dependabot, Code Scanning, and Secret
  Scanning each returned zero. This correction changes no product behavior,
  code, tests, configuration, OpenAPI, database migration, or deployment.

## Historical boundary

- The predecessor remains unchanged as the historical evidence snapshot that
  detected the stale slug. This successor is the authoritative navigation
  correction for future operations; it does not rewrite published history.
- `TASKS.md` records the canonical PR links and refreshed exact-head evidence
  for the active Draft task. The task remains `draft_pr`; this document does
  not authorize Ready, merge, or deployment.

## Conditional next steps

1. Before any authorized action, verify the canonical repository, `origin/main`,
   PR #40 state/head/base, checks, and reviews live again.
2. Keep PR #40 Draft until the user separately authorizes Ready. Merge and
   production deployment still require their own explicit commands.
