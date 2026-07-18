# AndrewWebSite task tracker

This is the living Git Flow tracker. Reconcile it with live Git/GitHub state at the start of every task branch and update it again before that pull request becomes Ready.

Status transitions are strictly `blocked -> planned -> in_progress -> draft_pr -> ready -> merged`. `ready` means reviewed and green; it never grants merge authorization. Every task starts from the latest `origin/main` in its own external worktree and uses one non-stacked pull request. Under the current policy, completed local and remote branches remain retained as read-only execution history.

| Order | Task | Phase | Owner | Prerequisite | Status | Branch | Worktree | PR / Issue | Checks | Merge authorization |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `task-session-handoff` | 0 | Codex | `origin/main` | `merged` | `task-session-handoff` | Clean worktree removed | [PR #17](https://github.com/devDaniilNovikov/AndrewWebSite/pull/17) | Squash merge `487780a705768caedd6099b5e240b8a200185d1f`; required CI and final review green | Merged under explicit authorization |
| 2 | `task-backend-contract-plans` | 0 | Codex | `task-session-handoff` merged | `merged` | `task-backend-contract-plans` | Clean worktree removed | [PR #15](https://github.com/devDaniilNovikov/AndrewWebSite/pull/15) | Squash merge `dbf4e184065a329c1708d65bed4bd5b5c1c8fe1b`; all required checks and reviews green | Merged under explicit authorization |
| 3 | `task-backend-skeleton` | 1 | Codex | `task-backend-contract-plans` merged | `merged` | `task-backend-skeleton` (retained remotely at `626e25c`) | Preserved: `/Users/daniilnovikov/.codex/worktrees/backend-skeleton/AndrewWorkWebSite` | [PR #18](https://github.com/devDaniilNovikov/AndrewWebSite/pull/18) | Squash merge `1fb0eb14076b6f42c776cf9b0b7fa8118980ee17`; PR and `main` CI/Semgrep green; 15 tests and JaCoCo 94.74% verified before merge | Merged under explicit authorization |
| 4 | `fix-preserve-merged-branches` | 1 | Codex | `task-backend-skeleton` merged | `in_progress` | `fix-preserve-merged-branches` | `/Users/daniilnovikov/.codex/worktrees/preserve-merged-branches/AndrewWorkWebSite` | PR pending | GitHub auto-delete disabled; skeleton branch restored; docs, links, secrets, CI, review pending | Not authorized |
| 5 | `task-ci-backend-gates` | 1 | Jules | `fix-preserve-merged-branches` merged | `blocked` | `task-ci-backend-gates` | Jules creates fresh after dispatch | Issue and PR pending | actionlint, Maven verify, dependency/security gates, CI, review | Not authorized |
| 6 | `task-backend-deploy-stub` | 1 | Codex | `task-ci-backend-gates` merged | `blocked` | pending | pending | pending | Maven, Docker build/smoke, secrets, CI, reviews | Not authorized |
| 7 | `task-backend-http-security` | 2 | Codex | previous task merged | `blocked` | pending | pending | pending | TDD, Maven verify, coverage, security, CI, reviews | Not authorized |
| 8 | `task-db-flyway-baseline` | 2 | Codex | previous task merged | `blocked` | pending | pending | pending | TDD, PostgreSQL 17 migrations/constraints, Maven verify, CI, reviews | Not authorized |
| 9 | `task-leads-api` | 2 | Codex | previous task merged | `blocked` | pending | pending | pending | TDD, API/idempotency/transaction/race tests, Maven verify, security, CI, reviews | Not authorized |
| 10 | `task-telegram-client` | 3 | Codex | previous task merged | `blocked` | pending | pending | pending | TDD, fake Telegram failures, Maven verify, secrets, CI, reviews | Not authorized |
| 11 | `task-telegram-worker` | 3 | Codex | previous task merged | `blocked` | pending | pending | pending | TDD, two-worker/lease/retry tests, Maven verify, telemetry, CI, reviews | Not authorized |
| 12 | `task-lead-retention` | 4 | Codex | previous task merged | `blocked` | pending | pending | pending | TDD, 29/30-day boundaries, replay after anonymization, Maven verify, CI, reviews | Not authorized |
| 13 | `task-backend-observability` | 4 | Codex | previous task merged | `blocked` | pending | pending | pending | TDD, degraded dependencies, PII-free telemetry, Maven verify, CI, reviews | Not authorized |
| 14 | `task-static-jar-integration` | 5 | Codex | `task-backend-observability` and Claude-owned frontend static-export scaffold merged | `blocked` | pending | pending | pending | frontend build/tests, Maven verify, container smoke, routing/cache/404/API/health checks, CI, reviews | Not authorized |

## Dispatch boundary for Jules

The skeleton merge prerequisite is satisfied, but no stacked work is allowed: do not create the `task-ci-backend-gates` Issue until `fix-preserve-merged-branches` is merged and the user separately authorizes dispatch. Then the allowed repository owner must create a new sanitized Issue and apply exactly one label, `jules-action`. Never apply `jules`, never combine both labels, and never label an untrusted source Issue.

## Tracker policy

- At task start, reconcile all predecessor rows with the commits actually merged into `origin/main`, then mark only the current task `in_progress`.
- Before opening a Draft PR, record the branch, worktree, PR/Issue, and checks actually run; set `draft_pr`.
- Before Ready, refresh checks and reviews and set `ready`. Do not infer or carry forward old results.
- After an authorized squash merge, the next task changes the row to `merged` using live GitHub and `origin/main` evidence.
