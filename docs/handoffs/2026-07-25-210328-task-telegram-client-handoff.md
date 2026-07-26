# task-telegram-client handoff

Signature: HND task-telegram-client [in_progress] topics: backend, security → predecessor: 2026-07-25-202007-task-telegram-client-handoff.md

## Durable — safe to cite later

- The formatter security follow-up neutralizes every Java line-break sequence
  in textual message fields before assembly. CR, LF, CRLF, vertical tab,
  form feed, NEL, Unicode line separator, and Unicode paragraph separator
  become spaces, while only formatter-owned newlines define the message
  structure.
- The message remains deterministic plain text without Telegram markup.
  Literal markup-like characters remain unchanged, `parse_mode` remains
  absent, and no lead content is logged.
- The credential-in-path redaction tests now use a runtime-assembled,
  visibly fictional Telegram-like token and verify that neither its raw nor
  percent-encoded representation reaches logs, observations, or metric tags.
- Production credentials, the private destination, Telegram, frontend,
  migrations, CI implementation, and production services were not touched.

## Snapshot at 2026-07-25T21:03:28Z — re-verify live before use

- A fresh fetch confirmed `origin/main` at
  `50fe44b281a29bfbf18b1677b5e929365149a2fd`. The predecessor local head was
  `5af7e70d5bc87a2d6569baf7039ad78bd201cb2c`; the successor commit containing
  this handoff must be resolved live before publication.
- Branch `task-telegram-client` remains local-only in
  `/Users/daniilnovikov/.codex/worktrees/telegram-client/AndrewWorkWebSite`.
  No remote branch or PR existed at the final live check.
- TDD RED produced eight failing formatter cases covering CR, LF, CRLF,
  vertical tab, form feed, NEL, Unicode line separator, and Unicode paragraph
  separator. The minimal formatter-only change made all cases green across
  every textual field.
- Focused Telegram verification passed 61 tests. `./mvnw -B verify` passed
  327 tests with no failures or errors; JaCoCo reported 880/938 covered lines
  (93.82%) and 484/569 covered branches (85.06%).
- The dependency tree contains `spring-boot-starter-restclient` and no
  WebFlux/Reactor dependency. A Spring TRACE scan found none of eight raw or
  encoded credential and PII fixture classes.
- Independent final code and security rereviews reported no Critical,
  Important, Minor, or other actionable finding. Whole-diff whitespace,
  ownership, credential-pattern, marker, and direct Telegram logging scans
  passed, and the mandatory pre-commit credential hook was not bypassed.
- The exact-head container/PostgreSQL 18.4/Flyway smoke remains historical
  evidence for implementation commit `bee16b1`; it was not repeated after
  this formatter-only follow-up. Real Telegram and CI were not run.
- Production secret-store encryption, access control, audit, and destination
  auto-delete remain release gates, not facts verified by this local task.

## Next steps — conditional, each requires the stated authorization

1. Re-fetch `origin`, confirm a clean worktree, resolve the exact local head,
   and re-check that no remote branch or PR appeared.
2. With explicit user authorization, push `task-telegram-client` and verify
   the remote head exactly matches the local branch.
3. Request separate authorization before opening a Draft PR, then record its
   exact URL and head and wait for every applicable check.
4. Ready, squash merge, and production release remain separate
   user-authorized transitions.
5. Start `task-telegram-worker` only after this task is confirmed merged from
   then-current `origin/main`.
