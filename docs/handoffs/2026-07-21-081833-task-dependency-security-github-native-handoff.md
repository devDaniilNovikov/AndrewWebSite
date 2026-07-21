# task-dependency-security-github-native handoff
Signature: HND task-dependency-security-github-native [in_progress] topics: ci, security, tracker → predecessor: none

## Durable — safe to cite later

- On 2026-07-21 the repository owner approved replacing the required ODC/NVD gate with required GitHub Dependency Review, continuous Dependabot alerts/security updates, complete trusted-main Maven dependency submission, and informational Snyk.
- The required context remains `dependency-security`; branch protection, Ready state, merge state, and existing Draft PRs are outside this task's mutation scope.
- Dependabot alerts and automated security fixes were enabled through the GitHub API. The initial open-alert query returned no alerts, and the dependency graph became available.
- PR #32 and PR #33 remain Draft, open, unmerged, and unchanged. Legacy Jules session `10325546321536741150` was instructed to stop ODC work and avoid publication; clean session `6187696383454533996` was started from `main` to prepare only the local GitHub-native CI patch.
- Path ownership is split without overlap: Jules owns `.github/workflows/ci.yml`, `.github/workflows/dependency-submission.yml`, and `pom.xml`; Codex owns the canonical docs, tracker, handoff, platform verification, and final review.

## Snapshot at 2026-07-21T08:18Z — re-verify live before use

- Base: `origin/main` `8940abe2dd92db6450f1934649581d4e00b6d1a1`.
- Branch: `task-dependency-security-github-native`.
- Worktree: `/Users/daniilnovikov/.codex/worktrees/dependency-security-github-native/AndrewWorkWebSite`.
- PR: none; publication is pending implementation and whole-diff review.
- Clean Jules session `6187696383454533996` was `IN_PROGRESS`; legacy session `10325546321536741150` remained publication-prohibited.
- Live branch protection still requires `Repository policy`, `verify`, and `dependency-security` with strict up-to-date checks.
- Snyk remains a non-required external status check.

## Next steps — conditional, each requires the stated live check

1. Re-read the latest Jules terminal change set only after the session reports completion; verify it remains based on `8940abe` and touches only the three assigned paths.
2. Apply or reproduce the reviewed CI patch in this worktree, then run actionlint, Maven verify, immutable-action checks, secret checks, documentation links, and full correctness/security review.
3. Verify Dependabot settings, complete Maven graph submission behavior, Dependency Review on the latest head, and unchanged branch protection before considering a Draft PR.
4. Do not publish, mark Ready, close old PRs, merge, or change branch protection without the required separate authorization.
