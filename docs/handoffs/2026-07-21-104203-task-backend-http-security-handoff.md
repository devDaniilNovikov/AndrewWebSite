# task-backend-http-security handoff

Signature: HND task-backend-http-security [in_progress] topics: backend, security, web → predecessor: none

## Durable — safe to cite later

- The task owns the backend HTTP security boundary, bounded request handling,
  rate limiting, local-only CORS configuration, and their tests. It does not
  own frontend paths, CI workflows, persistence, Telegram, production
  resources, or secrets.
- The public contract remains the existing OpenAPI document. This task adds no
  runtime lead controller and no database behavior.
- Implementation must preserve strict RED → GREEN → REFACTOR and leave the
  pull request Draft until a separate explicit Ready command.

## Snapshot at 2026-07-21T10:42:03Z — re-verify live before use

- `origin/main` and the new branch start at squash merge `459d493`; PR #29 is
  merged, its post-merge checks passed, and its source branch remains retained.
- The dedicated branch is `task-backend-http-security` in
  `/Users/daniilnovikov/.codex/worktrees/backend-http-security/AndrewWorkWebSite`.
- Baseline `./mvnw -B verify` passed before task edits. The desktop-root
  worktree and its user-owned untracked directories remain untouched.

## Next steps — conditional, each requires the stated live check

1. Add failing limiter and HTTP-boundary contract tests against the canonical
   architecture and OpenAPI behavior.
2. Implement the smallest stateless security, bounded body, problem response,
   local CORS, and limiter change that makes those tests pass.
3. Run focused and full verification, complete whole-diff correctness and
   security review, write a successor handoff, then open a Draft PR.
