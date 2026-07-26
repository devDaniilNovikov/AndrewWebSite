# fix-http-security-framework-native-deny start handoff

Signature: HND fix-http-security-framework-native-deny [in_progress] topics: backend, security, tracker

## Durable authorization and ownership

- The user explicitly authorized task start, this isolated local branch and
  worktree, reconciliation, implementation, tests, reviews, and local atomic
  commits for the approved framework-native deny plan.
- Push, Draft PR publication, transition to Ready, merge, production
  configuration, and deployment remain separate unauthorized gates.
- Codex owns only `ru.andrew.website.web` HTTP-security implementation and
  tests plus `TASKS.md`, the handoff index, and append-only task handoffs.
- OpenAPI, dependency manifests, actuator exposure, frontend, CI workflows,
  Flyway, Docker, infrastructure, and production are outside this task.

## Live snapshot at 2026-07-26T17:32:24Z

- The isolated worktree is
  `/Users/daniilnovikov/.codex/worktrees/http-security-framework-native-deny/AndrewWorkWebSite`
  on branch `fix-http-security-framework-native-deny`.
- Local HEAD and fresh `origin/main` matched at
  `27e6bb4f6991e0bef8ef9ae2bec48feb92c4aaec`; the new worktree was clean.
- No local or remote task branch and no task PR existed before creation.
- Direct Spring `FilterChainProxy` investigation reproduced the root gap:
  closed API and actuator namespaces were denied, but a non-local lead
  preflight returned `200` because null CORS configuration short-circuited
  authorization. The existing focused baseline passed 52 tests.
- The stale root workspace and every retained task worktree remain untouched.

## Three-step execution plan

1. Capture the direct-chain preflight RED without weakening the existing
   external boundary matrix.
2. Replace the dedicated filter and custom path lambdas with immutable Spring
   Security matchers, make non-local CORS reach authorization, and run focused
   GREEN/REFACTOR tests.
3. Run both clean verification paths and all scope, secret, static-analysis,
   specification, correctness, and security reviews; record a verified local
   handoff and stop before push.
