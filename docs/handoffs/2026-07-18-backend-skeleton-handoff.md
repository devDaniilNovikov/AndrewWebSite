# AndrewWebSite backend skeleton execution handoff

## Live state at handoff creation

- Created: 2026-07-18.
- Repository: `devDaniilNovikov/AndrewWebSite`; protected default branch `main`.
- Verified baseline: `origin/main` at `1b4fff3210c3b4cea5dd78947904acd33e631505` (`ci(security): add deferred Semgrep scanning`).
- Main checkout: `/Users/daniilnovikov/Documents/AndrewWorkWebSite`, clean at the verified baseline.
- This task: `task-session-handoff`, external worktree `/Users/daniilnovikov/.codex/worktrees/session-handoff/AndrewWorkWebSite`, created from that baseline.
- Contract task: `task-backend-contract-plans` at `5a162fd9c9ba9b4ea575d4ef307b05e9bf5e1f74`, clean worktree `/Users/daniilnovikov/.codex/worktrees/backend-contract-plans/AndrewWorkWebSite`, [PR #15](https://github.com/devDaniilNovikov/AndrewWebSite/pull/15) open and Ready. Live GitHub reported `MERGEABLE` and `BEHIND`; required checks on its current head were successful, auto-merge was off, and no GitHub approval was required by repository policy.
- Preserved dirty worktrees: `/Users/daniilnovikov/.codex/worktrees/backend-execution-handoff/AndrewWorkWebSite` has untracked `docs/.DS_Store`; `/Users/daniilnovikov/.codex/worktrees/jules-instructions/AndrewWorkWebSite` has deleted `JULES.md` plus untracked `.DS_Store`, `.planning/`, and `AGENTS.md`. Never force-remove, clean, reset, reuse, or commit their contents.
- Other verified worktrees: detached `/Users/daniilnovikov/.codex/worktrees/0589/AndrewWorkWebSite` is clean; `/Users/daniilnovikov/.gemini/antigravity/worktrees/AndrewWorkWebSite/deep-eclipse-beams-11h24` is clean.
- Local runtime: Temurin Java 25.0.3. No system Maven, Docker, or actionlint was available at the preceding handoff; reverify tools before relying on them.

## Settled product and architecture decisions

The complete approved specification remains in [`.agents/AGENTS.md`](../../.agents/AGENTS.md), the backend contracts in `docs/backend/` after PR #15 merges, and the executable plans in `docs/superpowers/plans/`. Do not reopen settled decisions.

- One root Maven module, Spring Boot 4.1.0, Java 25, package `ru.andrew.website`; frontend remains Claude-owned under `frontend/`.
- MVP has no login, accounts, CRM, admin, ecommerce, Redis, or separate broker.
- Lead intake is durable PostgreSQL 17 plus transactional outbox, then at-least-once Telegram delivery with leases and retries.
- Production is one non-root Java container; the final static Next.js export is embedded only after the frontend prerequisite merges.
- Public surface, lead validation/idempotency, keyed-HMAC fingerprinting, rate limits, queue state machine, privacy limits, and observability constraints are fixed exactly as documented in [`.agents/AGENTS.md`](../../.agents/AGENTS.md).
- Never log or expose lead PII. Credentials and production values come only from approved secret stores.

## Authorized sequence and merge boundary

Execute without stacked PRs:

1. Finish `task-session-handoff`: validate docs, commit atomically with Codex attribution, open Draft PR, obtain green CI/reviews, mark Ready, then squash-merge. This merge is explicitly authorized only after green gates.
2. Return PR #15 to Draft. After the handoff merge lands, merge current `origin/main` normally into `task-backend-contract-plans` (no rebase or force-push), with a conventional attributed merge commit. Correct Task 1 pre-flight details, repeat all reviews and checks, mark Ready, then squash-merge. This merge is explicitly authorized only after fixes and green gates.
3. Only after PR #15 is merged, create a fresh `task-backend-skeleton` branch/worktree from latest `origin/main`. Execute only Task 1 of `docs/superpowers/plans/2026-07-18-backend-foundation.md` with one fresh implementer, RED/GREEN/REFACTOR, implementer self-review, independent specification review, independent quality/security review, fix loops, whole-branch review, and fresh controller verification. Open Draft, reach Ready, then stop. Skeleton merge is not authorized.
4. Do not create the Jules Issue for `task-ci-backend-gates` until a future explicitly authorized skeleton merge is complete.

## Required corrections for PR #15

- Use test-scoped `spring-boot-starter-webmvc-test`, not redundant `spring-boot-starter-test`, for Spring Boot 4.1 `AutoConfigureMockMvc` support.
- Generate the wrapper with `org.apache.maven.plugins:maven-wrapper-plugin:3.3.4:wrapper -Dtype=bin -Dmaven=3.9.16`.
- Document temporary official Maven 3.9.16 bootstrap: download into a temporary directory, verify the published official SHA-512, do not install system Maven, generate the wrapper, and record both `distributionSha256Sum` and `wrapperSha256Sum`.
- Add the executing agent's `Co-Authored-By` footer to the skeleton commit command.

## Skeleton execution contract

Before implementation, consult current official Spring Boot 4.1.0 and Maven Wrapper 3.3.4 documentation. Bootstrap Maven 3.9.16 from the official artifact into a temporary directory with published SHA-512 verification. Pin wrapper SHA-256 checksums.

Implement only: one Maven module, Java 25, UTC `Clock`, explicit exactly-one `test|local|prod` profile guard, no `spring.profiles.default`, Actuator/Micrometer health-only surface, dependency-free liveness, readiness foundation, exact `Cache-Control: no-store`, Boot 4.1 Web MVC test starter, and JaCoCo line coverage at least 0.80. Do not implement leads, PostgreSQL, Telegram, Docker, or Jules CI.

Fresh evidence must include wrapper version, the recorded RED failure, `./mvnw -B test`, `./mvnw -B verify`, `./mvnw -B dependency:tree`, coverage at least 0.80, `git diff --check`, secrets/scope review, reviews, and required CI.

## Git, security, and stop conditions

- Follow the [canonical Git Flow](../../.agents/workflows/GIT_FLOW.md): protected `main`, PR-only, squash merges, no direct or force pushes, no auto-merge, and one clean external worktree per task.
- Every AI-authored commit, including the synchronization merge commit, carries the executing agent's own attribution footer: `Co-Authored-By: Codex GPT-5.6-Sol <codex@users.noreply.github.com>`.
- Never expose, repeat, request, save, scan into output, or forward any GitHub, Jules, Telegram, database, or production secret. Previously exposed credentials are compromised and unusable.
- Stop immediately for new secret exposure, data-loss risk, ownership conflict, architectural drift, an unresolvable required check, or three failed fixes for the same root condition.
- Ready is not permission to merge. Stop at Ready for `task-backend-skeleton` because its merge is not authorized.
- At roughly 80% context, do not begin another major unit. First update this durable handoff and [`TASKS.md`](../../TASKS.md), then create a new Codex task; never compact mid-implementation.

## Immediate next step

Complete the handoff PR and merge it under the existing authorization. Then correct and merge PR #15. Only then begin the isolated skeleton implementation.
