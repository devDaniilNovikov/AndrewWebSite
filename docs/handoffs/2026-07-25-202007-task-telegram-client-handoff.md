# task-telegram-client handoff

Signature: HND task-telegram-client [in_progress] topics: backend, security → predecessor: 2026-07-25-193657-task-telegram-client-handoff.md

## Durable — safe to cite later

- Local implementation commit
  `bee16b18118223fbc906df1d5f104ef1e1654e3f` adds the Boot-managed
  synchronous `RestClient` Telegram gateway, immutable contracts,
  deterministic plain-text formatting, bounded `retry_after` parsing,
  fixed-profile endpoint policy, and complete HTTP outcome classification.
- Production bot token and chat ID remain runtime bindings only. The
  operational contract requires encryption at rest, access control, audit,
  and protected delivery by the platform secret store. Telegram HTTPS still
  requires the usable token in process memory and in its protocol-defined
  request path, so application-managed ciphertext cannot replace the token.
- Spring HTTP observations are disabled only for this credential-in-path
  client because Spring network exceptions retain the expanded URI. The
  replacement `andrew.telegram.client` observation contains only a static
  token-free route, method, and bounded outcome and never receives an
  exception, response body, token, destination, or lead data.
- The public owner-account `t.me` hyperlink is a separate frontend/product
  contract. This backend task neither changes it nor derives the private bot
  destination from it.

## Snapshot at 2026-07-25T20:20:07Z — re-verify live before use

- Branch `task-telegram-client` is local-only in
  `/Users/daniilnovikov/.codex/worktrees/telegram-client/AndrewWorkWebSite`,
  based on `origin/main`
  `50fe44b281a29bfbf18b1677b5e929365149a2fd`. No push or PR exists.
- TDD first reproduced the missing implementation. A later security RED test
  proved that Spring exposed a fictional token-bearing
  `ResourceAccessException` to an observation handler; the dedicated
  no-error telemetry design made that regression green.
- Exact commit verification: `./mvnw -B verify` passed 319 tests with JaCoCo
  879/937 lines (93.81%) and 484/569 branches (85.06%).
- The dependency tree contains
  `spring-boot-starter-restclient` and no WebFlux/Reactor dependency.
- The exact-commit container build passed 303 non-database tests. An
  isolated PostgreSQL 18.4 smoke applied the one Flyway migration, exposed
  both `leads` and `telegram_outbox`, ran the application as UID/GID 10001,
  and returned minimal `UP` liveness/readiness with `Cache-Control:
  no-store`. Fictional credential values were absent from container logs.
- Independent code and security re-reviews reported no actionable finding.
  Whole-diff whitespace, ownership, credential-pattern, marker, and direct
  Telegram logging scans passed. The pre-commit credential detector was not
  bypassed; a false-positive fixture identifier was renamed and the hook
  then passed.
- Both isolated smoke containers, their temporary PostgreSQL data, and the
  dedicated Docker network were removed. Production, real Telegram, CI,
  `frontend/`, migrations, and Jules-owned workflow files were untouched.

## Next steps — conditional, each requires the stated authorization

1. With explicit user authorization, push `task-telegram-client` and verify
   the remote head matches the local branch.
2. Request separate authorization before opening a Draft PR, then record its
   exact URL/head and wait for every applicable check.
3. Ready and squash-merge remain separate user-authorized transitions.
4. Start `task-telegram-worker` only after this task is confirmed merged,
   from a new worktree based on then-current `origin/main`.
5. Real Telegram credentials and the private destination are needed only for
   an explicitly authorized production configuration/release check; never
   place them in this task, a PR, logs, or tracked files.
