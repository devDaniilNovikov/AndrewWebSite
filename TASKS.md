# AndrewWebSite task tracker

This is the living Git Flow tracker. Reconcile it with live Git/GitHub state at the start of every task branch and update it again before that pull request becomes Ready.

Status transitions are strictly `blocked -> planned -> in_progress -> draft_pr -> ready -> merged`. `ready` means reviewed and green; it never grants merge authorization. Every task starts from the latest `origin/main` in its own external worktree and uses one non-stacked pull request.

| Order | Task | Phase | Owner | Prerequisite | Status | Branch | Worktree | PR / Issue | Checks | Merge authorization |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `task-session-handoff` | 0 | Codex | `origin/main` | `in_progress` | `task-session-handoff` | `/Users/daniilnovikov/.codex/worktrees/session-handoff/AndrewWorkWebSite` | Draft PR pending | Markdown, relative links, secrets, diff, CI, reviews | Authorized for squash merge after green gates |
| 2 | `task-backend-contract-plans` | 0 | Codex | `task-session-handoff` merged | `ready` | `task-backend-contract-plans` | `/Users/daniilnovikov/.codex/worktrees/backend-contract-plans/AndrewWorkWebSite` | [PR #15](https://github.com/devDaniilNovikov/AndrewWebSite/pull/15) | Current-head CI green; specification, quality/security, whole-branch review, OpenAPI, Markdown, fences, and secrets must be rerun after required fixes | Authorized for squash merge after fixes and green gates |
| 3 | `task-backend-skeleton` | 1 | Codex | `task-backend-contract-plans` merged | `blocked` | `task-backend-skeleton` | Create fresh after prerequisite merge | PR pending | Wrapper/version, RED, test, verify, dependency tree, JaCoCo >= 0.80, diff, secrets, reviews, CI | Not authorized; stop at Ready |
| 4 | `task-ci-backend-gates` | 1 | Jules | `task-backend-skeleton` merged | `blocked` | `task-ci-backend-gates` | Jules creates fresh after dispatch | Issue and PR pending | actionlint, Maven verify, dependency/security gates, CI, review | Not authorized |
| 5 | `task-backend-deploy-stub` | 1 | Codex | `task-ci-backend-gates` merged | `blocked` | pending | pending | pending | Maven, Docker build/smoke, secrets, CI, reviews | Not authorized |
| 6 | `task-backend-http-security` | 2 | Codex | previous task merged | `blocked` | pending | pending | pending | TDD, Maven verify, coverage, security, CI, reviews | Not authorized |
| 7 | `task-db-flyway-baseline` | 2 | Codex | previous task merged | `blocked` | pending | pending | pending | TDD, PostgreSQL 17 migrations/constraints, Maven verify, CI, reviews | Not authorized |
| 8 | `task-leads-api` | 2 | Codex | previous task merged | `blocked` | pending | pending | pending | TDD, API/idempotency/transaction/race tests, Maven verify, security, CI, reviews | Not authorized |
| 9 | `task-telegram-client` | 3 | Codex | previous task merged | `blocked` | pending | pending | pending | TDD, fake Telegram failures, Maven verify, secrets, CI, reviews | Not authorized |
| 10 | `task-telegram-worker` | 3 | Codex | previous task merged | `blocked` | pending | pending | pending | TDD, two-worker/lease/retry tests, Maven verify, telemetry, CI, reviews | Not authorized |
| 11 | `task-lead-retention` | 4 | Codex | previous task merged | `blocked` | pending | pending | pending | TDD, 29/30-day boundaries, replay after anonymization, Maven verify, CI, reviews | Not authorized |
| 12 | `task-backend-observability` | 4 | Codex | previous task merged | `blocked` | pending | pending | pending | TDD, degraded dependencies, PII-free telemetry, Maven verify, CI, reviews | Not authorized |
| 13 | `task-static-jar-integration` | 5 | Codex | `task-backend-observability` and Claude-owned frontend static-export scaffold merged | `blocked` | pending | pending | pending | frontend build/tests, Maven verify, container smoke, routing/cache/404/API/health checks, CI, reviews | Not authorized |

## Dispatch boundary for Jules

Do not create the `task-ci-backend-gates` Issue until `task-backend-skeleton` is merged. Then the allowed repository owner must create a new sanitized Issue and apply exactly one label, `jules-action`. Never apply `jules`, never combine both labels, and never label an untrusted source Issue.

## Tracker policy

- At task start, reconcile all predecessor rows with the commits actually merged into `origin/main`, then mark only the current task `in_progress`.
- Before opening a Draft PR, record the branch, worktree, PR/Issue, and checks actually run; set `draft_pr`.
- Before Ready, refresh checks and reviews and set `ready`. Do not infer or carry forward old results.
- After an authorized squash merge, the next task changes the row to `merged` using live GitHub and `origin/main` evidence.
