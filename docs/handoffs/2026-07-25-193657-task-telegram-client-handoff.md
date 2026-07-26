# task-telegram-client handoff

Signature: HND task-telegram-client [in_progress] topics: backend, security → predecessor: none

## Scope and ownership

- Codex owns `pom.xml`, backend Telegram source/tests, runtime profile
  configuration, canonical backend operations/architecture documentation,
  `TASKS.md`, and this task's handoff chain.
- Frontend, Jules-owned CI implementation, production services, credentials,
  and the separate framework-native deny backlog are outside scope.
- The task starts from live `origin/main` commit
  `50fe44b281a29bfbf18b1677b5e929365149a2fd` in the dedicated clean worktree
  `/Users/daniilnovikov/.codex/worktrees/telegram-client/AndrewWorkWebSite`.

## Execution plan

1. Write failing gateway, formatting, configuration, and redaction tests.
2. Add the smallest Boot-managed synchronous Telegram client implementation
   that satisfies the canonical contracts.
3. Run focused and full gates, review the complete diff, and create a local
   implementation commit.

## Boundaries

- Use only `RestClient`; do not add WebFlux or Reactor.
- Use fake local/test HTTP endpoints only. Do not call Telegram or any
  production dependency.
- Push, Draft PR publication, Ready transition, merge, and production deploy
  remain separately authorized actions.
