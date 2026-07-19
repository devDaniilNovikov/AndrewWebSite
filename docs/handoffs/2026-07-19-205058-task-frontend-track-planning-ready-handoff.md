# task-frontend-track-planning ready handoff

Signature: HND T5 [ready] topics: frontend → predecessor: 2026-07-19-204641-task-frontend-track-planning-handoff.md

## Durable — safe to cite later

- `frontend-tasks/` contains exactly six decision-complete task files:
  F1 foundation, F2 shell, F3 product pages, F4 trust content, F5 lead form,
  and F6 quality hardening.
- F1-F6 belong to a frontend agent selected by the user, not Claude Code.
  No agent has been dispatched and no frontend implementation has started.
- Every implementation task requires a fresh non-stacked branch/PR, applicable
  quality gates, Codex review, and a separate user merge command.
- Content tasks remain blocked on verified user-owned inputs; F5 remains
  blocked on the merged backend leads API; static JAR integration waits for
  F6 and backend observability.

## Snapshot at 2026-07-19T20:50Z — re-verify live before use

- Draft PR #28 is mergeable against T6 merge
  `2be8a2ca76beef192a76894b7c6ffd5ce38fc1c6`.
- Repository policy, verify, dependency-security, CodeQL, Semgrep, and Snyk
  passed on reviewed pre-ready head `7afd6fe`.
- `./mvnw -B verify` passed with 32 tests and the JaCoCo gate.
- Task-file structure and atomicity, tracker order/ownership/dependencies,
  links, memory signatures, scope, secret-pattern, specification, quality,
  security, and whole-diff reviews passed.

## Next steps — conditional, each requires the stated live check

1. Verify all required checks on the final metadata head, then remove Draft
   only if head/base, mergeability, conversations, and protection remain
   valid.
2. Merge only after a separate explicit user command; squash only and retain
   `task-frontend-track-planning`.
3. Do not dispatch or implement F1 until the user separately selects the
   frontend agent and authorizes that task.
