# task-frontend-quality-hardening startup handoff

Signature: HND task-frontend-quality-hardening [in_progress] topics: frontend, performance, accessibility, testing, security, tracker → predecessor: `2026-08-02-081723-task-frontend-openapi-ci-path-draft-handoff.md`

## Durable — safe to cite later

- The user explicitly authorized the Ready transition and squash-merge of
  F5A, followed by F6 implementation and Draft publication. This does not
  authorize F6 Ready, F6 merge, branch deletion, deployment, JAR integration,
  production mutation, or the start of F3/F4.
- F6 owns `frontend/**` plus its task-specific controller tracker and handoff
  metadata. Backend code, OpenAPI content, CI architecture, branch protection,
  PR #30, verified business content, legal text, and production remain outside
  scope.
- F6 preserves the approved landing composition and the local-media contract.
  It may add quality tooling and remove optional reveal cost, but it may not
  add screenshot baselines or any stock, generated, extracted, or otherwise
  unverified photograph.
- The static handoff remains `pnpm run build:preview` producing
  `frontend/out/`. Production export must continue to fail while readiness
  blockers remain missing.

## Verified snapshot at 2026-08-02T08:39:27Z — re-verify live before use

- F5A [PR #66](https://github.com/devDaniilNovikov/AndrewWebSite/pull/66)
  was marked Ready and squash-merged with exact-head guard
  `489646683353363839dd4a4b57c658d14f46e1eb` as
  `1372bbd2e1c50e0dc549e4be8c42909e7359e714`. Linked Issue #65 closed and the
  native Jules source branch remained at `4896466`.
- Post-merge `main` CI run
  [30739997420](https://github.com/devDaniilNovikov/AndrewWebSite/actions/runs/30739997420),
  Dependency Submission run
  [30739997441](https://github.com/devDaniilNovikov/AndrewWebSite/actions/runs/30739997441),
  and Semgrep run
  [30739997442](https://github.com/devDaniilNovikov/AndrewWebSite/actions/runs/30739997442)
  succeeded. GitHub recorded zero deployments, and PR #30 remained open Draft
  at `00f55eea452884dc4c286ae17fcb6e1d4ebc25d8`.
- No existing local or remote `task-frontend-quality-hardening` branch, F6 PR,
  parallel owner, or target worktree was present. The dedicated worktree
  `/Users/daniilnovikov/.codex/worktrees/frontend-quality-hardening/AndrewWorkWebSite`
  was created clean from exact fresh `origin/main` at `1372bbd` on branch
  `task-frontend-quality-hardening`.
- Canonical F6 acceptance requires three cold mobile Lighthouse audits with
  median Performance, Accessibility, and Best Practices scores of at least
  90; behavior-based Playwright contracts at 390×844, 768×1024, and
  1440×900; at least 80% coverage; deterministic static export; and the
  expected production-gate failure. SEO is intentionally excluded while
  preview `noindex` remains active.

## Conditional continuation — re-verify live

1. Commit this startup reconciliation before changing frontend files.
2. Follow RED → GREEN → REFACTOR for the Lighthouse runner and behavior-based
   visual contracts. Add only exact `lighthouse@13.4.1` and
   `chrome-launcher@1.2.1`, retain the pinned Node/Corepack/pnpm toolchain, and
   write reports only below ignored `frontend/test-results/lighthouse`.
3. Run the complete local quality, performance, accessibility, privacy,
   static-export, audit, Maven, secret-scan, and independent-review gates.
   Publish only a Draft PR titled
   `test(frontend): harden preview quality gates`, then stop.
