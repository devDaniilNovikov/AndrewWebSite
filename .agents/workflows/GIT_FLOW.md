# AndrewWebSite Git Flow

## Model

- Use trunk-based development with `main` as the only long-lived branch.
- Every change reaches `main` through a pull request; direct pushes are blocked.
- Do not create `develop`, release, or agent-specific long-lived branches.
- Start every task from the latest `origin/main`; stacked pull requests are forbidden.

## Work Unit and Ownership

- One approved task or issue equals one branch, one worktree, and one pull request.
- Declare the owning agent and paths before editing.
- Parallel tasks must use separate worktrees and must not modify overlapping paths.
- A task that depends on an unmerged pull request waits until that prerequisite reaches `main`.
- Never reuse another agent's branch or worktree.

## Branch Names

```text
task-<short-kebab-case-description>
fix-<short-kebab-case-description>
```

- Use `task-*` for features, documentation, infrastructure, and planned maintenance.
- Use `fix-*` for defects, regressions, hotfixes, and revert work.
- Use lowercase ASCII letters, digits, and hyphens only.
- CI rejects unsupported branch names.

## Task Lifecycle

1. Fetch `origin`, inspect open pull requests, and verify the prerequisite state.
2. Create a dedicated external worktree and branch from fresh `origin/main`.
3. Record the task scope, owned paths, dependencies, and a plan of at most three steps.
4. Implement the smallest complete change; use RED, GREEN, REFACTOR for testable behavior.
5. Run focused checks, then every applicable repository quality and security gate.
6. Review the complete diff for secrets, unrelated edits, compatibility, and missing tests.
7. Create atomic Conventional Commits with the responsible agent's attribution footer.
8. Push the branch and open a Draft pull request with summary, test evidence, and risks.
9. Resolve CI failures and review findings only within the declared scope.
10. Mark Ready only after required CI is green and Codex review is complete.
11. Merge only after the user explicitly authorizes it.
12. Squash-merge using the Conventional Commit PR title as the final commit subject.
13. Confirm `main`, close the linked issue, prune the remote branch, and remove the clean worktree.

## Pull Request Contract

- PR titles follow `feat|fix|docs|test|refactor|perf|chore|ci(scope): description`.
- The body states what changed, why, checks actually run, risks, and related issues.
- Draft means work or verification is incomplete; Ready means it is reviewable and green.
- Required CI checks currently include `Repository policy` and
  `Application checks (pending scaffold)`.
- Renaming a required job must update GitHub branch protection in the same change.
- GitHub account approvals are not required for the sole-owner repository; Codex review
  and explicit user merge authorization remain mandatory workflow gates.
- Jules and other automation may create pull requests but never merge them.
- Auto-merge remains disabled.

## Local Verification

- Workflow changes: run `actionlint` for every changed workflow.
- Documentation changes: run `git diff --check` and verify every relative link.
- Application changes: use checks declared by committed manifests and lockfiles.
- Backend behavior requires the test and coverage gates defined in `../AGENTS.md`.
- Frontend behavior requires the gates defined in `../CLAUDE.md`.
- Never weaken a valid test, security control, or branch rule to make CI green.

## Protected `main`

- Require a pull request before merging.
- Require the configured CI checks and require the branch to be up to date.
- Require conversation resolution.
- Enforce protection for administrators.
- Require linear history; allow squash merge only.
- Block force pushes and deletion of `main`.
- Automatically delete remote source branches after merge.

## Cleanup Safety

- Remove a local worktree only after confirming it has no tracked or untracked work to preserve.
- Never force-remove a dirty worktree; report it and leave it available for recovery.
- Delete obsolete remote branches only after their PR is merged, closed as superseded, or
  explicitly abandoned by the user.
- Run `git fetch --prune` after cleanup.

## Hotfixes and Rollback

- A hotfix starts from current `origin/main` on `fix-<description>` and follows the same PR gates.
- Revert a bad merge with a new `fix-revert-<description>` pull request using `git revert`.
- Do not rewrite published history or bypass protection for an emergency.
- Production rollback or release tagging requires explicit user authorization.

## Exceptions

- Any exception requires an explicit user decision and a synchronized update to this document.
- Security, secret scanning, input validation, and review requirements cannot be waived.
