# task-backend-http-security handoff

Signature: HND task-backend-http-security [in_progress] topics: backend, security → predecessor: 2026-07-21-110405-task-backend-http-security-handoff.md

## Durable — safe to cite later

- The user authorized the fourth deny-path implementation attempt and asked to
  keep the framework-native replacement in the backlog. Tracker row 31 records
  `fix-http-security-framework-native-deny` as the follow-up; it is not
  authorized for implementation in this task.
- The accepted implementation keeps the dedicated `PublicBoundaryDenyFilter`
  in front of authorization/MVC. It exists to preserve the exact deny, CORS,
  and actuator/static contracts after three Spring Security matcher variants
  failed to do so.
- The public HTTP boundary is implemented without adding a runtime
  `/api/leads` controller. Contract tests use a test-only stub with a generic
  JSON body.

## Snapshot at 2026-07-21T11:42:11Z — re-verify live before use

- Branch: `task-backend-http-security` in worktree
  `/Users/daniilnovikov/.codex/worktrees/backend-http-security/AndrewWorkWebSite`.
- Base: live `origin/main` is still
  `459d493da0e5c4377f5778e90b8798a113568f8b`, matching merged PR #29.
- Implementation commit:
  `afdda37f0dc468b60e8598d3197987fec2e58929`.
- Local implementation gate passed: focused security/payload/CORS/limiter
  tests passed, then `./mvnw -B verify` passed with 69 tests and the existing
  JaCoCo gate.
- The final whole-diff correctness and security review found no unresolved
  Critical or Important finding. The framework-native matcher cleanup is a
  non-blocking follow-up with its current dedicated-filter behavior protected
  by contract tests.
- Scope/quality checks passed on implementation head: `git diff --check`,
  conflict-marker scan, relative-link scan, changed-text secret scan, and
  owner-scope review. Frontend, PR #30, CI workflows, production config, and
  secrets remain untouched.
- Final metadata commit, normal push, Draft PR creation, and exact-head
  GitHub checks are still pending.

## Next steps — conditional, each requires the stated live check

1. Commit the tracker/memory/handoff metadata without modifying the
   implementation commit.
2. Rerun `./mvnw -B verify` on the final metadata head, then run the final
   diff/scope/secret review.
3. Push `task-backend-http-security`, create a Draft PR only, update tracker
   to `draft_pr`, and wait for the exact pushed SHA checks.
4. Do not mark Ready or merge without a new explicit user command.
