# task-frontend-openapi-ci-path Draft handoff

Signature: HND task-frontend-openapi-ci-path [draft_pr] topics: frontend, ci, openapi, testing, incident, tracker → predecessor: `2026-08-02-073618-task-frontend-lead-form-ready-handoff.md`

## Durable — safe to cite later

- F5 was squash-merged as
  `781ff1ce272f939f04913c12564a10852e8881c2`. Its source branch and worktree
  remain retained, post-merge CI and security checks passed, GitHub recorded
  no deployment, and PR #30 was not changed.
- The user explicitly authorized F5A, owner-authored Issue #65, and its Jules
  dispatch. Guarded run
  [30738727842](https://github.com/devDaniilNovikov/AndrewWebSite/actions/runs/30738727842)
  failed during local `jq` payload assembly with `Argument list too long`,
  before any Jules API request, session, generated branch, or pull request.
  The user then selected a mutually exclusive native recovery: `jules-action`
  was removed, absence of both Jules labels was verified, and only `jules` was
  applied to the same Issue. Native Jules task `17993467742706470746` produced
  exactly one generated branch and one PR.
- The functional F5A implementation changes only
  `.github/scripts/verify-ci-paths.sh` and
  `.github/scripts/test-ci-paths.sh`: the exact canonical path
  `docs/backend/openapi.yaml` is frontend-relevant, while other backend/docs
  paths remain skipped. Codex separately updates only the required controller
  tracker, handoff, and incident metadata. No workflow, frontend file, OpenAPI
  content, manifest, lockfile, backend API, branch protection, secret, or
  deployment setting is changed.
- PR #66 remains Draft. Ready, merge, F6, branch deletion, deployment, and any
  change to PR #30 require separate authorization and are outside this
  checkpoint.

## Verified snapshot at 2026-08-02T08:17:23Z — re-verify live before use

- `origin/main` was
  `781ff1ce272f939f04913c12564a10852e8881c2`. Generated branch
  `task-frontend-openapi-ci-path-17993467742706470746` and the dedicated
  worktree `/Users/daniilnovikov/.codex/worktrees/frontend-openapi-ci-path/AndrewWorkWebSite`
  were clean and matched implementation head
  `14da15d24dba5da8109dc32a6b5738408c11a7c1` before this controller metadata
  commit.
- Owner-authored [Issue #65](https://github.com/devDaniilNovikov/AndrewWebSite/issues/65)
  was open with only the native `jules` label. Jules reported the task ready
  for review. Draft [PR #66](https://github.com/devDaniilNovikov/AndrewWebSite/pull/66)
  targeted exact base `781ff1c`, had exact implementation head `14da15d`, and
  GitHub reported `MERGEABLE` and `CLEAN`. No duplicate Jules branch or PR was
  present.
- RED was reproduced against the pre-change policy:
  `docs/backend/openapi.yaml` returned `skip`. GREEN passed through
  `.github/scripts/test-ci-paths.sh` and direct NUL-safe fixtures for OpenAPI
  `relevant`, mixed OpenAPI `relevant`, and unrelated backend documentation
  `skip`.
- Official actionlint 1.7.12 for Darwin arm64 was verified against its
  published SHA-256 and passed `.github/workflows/ci.yml`. Local
  `./mvnw -B verify` passed 660 tests with zero failures, errors, or skips and
  all configured coverage checks.
- Exact implementation-head Frontend quality, Repository policy, `verify`,
  dependency-security, Java security/CodeQL, Semgrep, and Snyk checks passed.
  GitHub reported zero deployments for the implementation head. PR #30
  remained open Draft at `00f55eea452884dc4c286ae17fcb6e1d4ebc25d8`.
- Independent specification, code/correctness, and security/workflow reviews
  found no blocking issue. Specification and code reviews recorded one P3
  process deviation: Jules's generated implementation commit included a
  preliminary `TASKS.md` edit outside its two-script ownership. Rewriting the
  generated history is prohibited, so that preliminary content was treated
  only as evidence and replaced by the complete Codex controller metadata in
  this checkpoint; the deviation does not alter functional scope.

## Conditional continuation — re-verify live

1. Commit and push this controller metadata, then wait for all required and
   security checks on the new exact head without weakening any gate.
2. Recheck the full exact-head diff and PR conversations after CI; resolve any
   new in-scope Critical or Important finding before stopping.
3. Keep PR #66 Draft. Ready requires separate explicit authorization, merge
   requires another separate authorization, and F6 remains blocked until F5A
   is merged.
