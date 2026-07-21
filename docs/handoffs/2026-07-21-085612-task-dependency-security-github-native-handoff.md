# task-dependency-security-github-native handoff
Signature: HND task-dependency-security-github-native [in_progress] topics: ci, security, tracker → predecessor: 2026-07-21-081833-task-dependency-security-github-native-handoff

## Durable — safe to cite later

- The repository owner approved GitHub-native Dependency Review as the required pull-request gate, continuous Dependabot alerts and security updates, trusted-main Maven dependency submission, and informational Snyk.
- Dependabot vulnerability alerts and automated security fixes are enabled. The complete GitHub-native architecture and operational contract are canonical in `docs/backend/architecture.md` and `docs/backend/operations.md`.
- Local commit `5f73fd62ba701e324694432ae159a1062c4bf58c` records the architecture, tracker, decision, and initial handoff. Local commit `f09dc682ccb3eeaea86f73808498e5d084983e7c` implements the CI replacement reviewed from Jules session `6187696383454533996`.
- The required job remains `dependency-security`. It uses pinned Dependency Review, fails on newly introduced high-or-critical vulnerabilities in runtime, development, or unknown scopes, and does not weaken the required context.
- Dependency submission runs only for trusted `main` push or guarded manual `main`, scopes `contents: write` to its job, disables checkout credential persistence, pins every action, and cancels overlapping runs to prevent stale snapshots.
- Independent Codex evidence on the committed tree: checksum-verified actionlint 1.7.12 passed; `./mvnw -B verify` passed 32 tests and JaCoCo; full-SHA tag resolution, whole-diff whitespace, forbidden ODC/NVD patterns, scope, permissions, and secret scans passed.
- No branch, PR, Ready transition, merge, branch-protection change, or closure of PR #32, PR #33, or Issue #31 was authorized or performed.

## Snapshot at 2026-07-21T08:56Z — re-verify live before use

- Base and remote `main`: `8940abe2dd92db6450f1934649581d4e00b6d1a1`.
- Local branch: `task-dependency-security-github-native`; implementation commit `f09dc682ccb3eeaea86f73808498e5d084983e7c`; worktree clean after the metadata commit that adds this handoff.
- Remote branch and PR: none. Publication remains a separate owner authorization.
- PR #32 remains Draft at `afedf891ee7a468cbcf778a436ca19117696a96c`; PR #33 remains Draft at `caef13e77216198be96f9eadbadb564beade7772`.
- Branch protection remains strict and requires `Repository policy`, `verify`, and `dependency-security`.
- Dependabot vulnerability alerts return HTTP 204, automated security fixes report enabled and unpaused, and the open-alert query is empty.
- Jules returned the corrected local change set and verification report; its API lifecycle still reported `IN_PROGRESS` after the terminal agent message. Publication was disabled and no Jules remote output exists.

## Next steps — conditional, each requires the stated live check

1. Re-fetch `origin/main`, confirm the local branch is still based on `8940abe`, and verify no later Jules activity changed the reviewed tree.
2. Only after separate owner authorization, push `task-dependency-security-github-native` and open a Draft PR without changing branch protection or existing Draft PRs.
3. On the Draft PR's latest HEAD, verify Dependency Review, repository policy, Maven verify, CodeQL, Semgrep, and informational Snyk; verify trusted-main dependency submission only after merge authorization and merge.
4. Ready, merge, closure of PR #32/PR #33/Issue #31, and removal of the now-unused NVD secret each require their own explicit owner decision.
