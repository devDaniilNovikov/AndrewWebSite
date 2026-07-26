# fix-http-security-framework-native-deny Draft PR handoff

Signature: HND fix-http-security-framework-native-deny [draft_pr] topics: backend, security, tracker → predecessor: 2026-07-26-182249-fix-http-security-framework-native-deny-remote-checks-handoff.md

## Durable authorization

- The user explicitly authorized creating the PR, running its CI/CD, and
  updating `main` after green checks. Under repository Git Flow this is
  executed as Draft publication, Ready transition, and conditional squash
  merge; direct pushes to `main` remain forbidden.
- Production configuration and deployment remain unauthorized.
- Completed task branches must remain retained after merge.

## Live PR snapshot at 2026-07-26T18:30:59Z

- Draft PR
  [#55](https://github.com/devDaniilNovikov/AndrewWebSite/pull/55),
  `fix(http-security): use framework-native deny matchers`, is open and
  mergeable from `fix-http-security-framework-native-deny` into `main`.
- Base is `27e6bb4f6991e0bef8ef9ae2bec48feb92c4aaec`; pre-metadata head is
  `d8a8a1774f2ceb974621d02ba4ed0276ec79bbd5`.
- Push-triggered CI run
  [30214638560](https://github.com/devDaniilNovikov/AndrewWebSite/actions/runs/30214638560)
  passed Repository policy, Maven verify, and Java CodeQL on that head.
  Dependency security was correctly skipped because it is PR-only.
- The repository remains squash-only and automatic head-branch deletion is
  disabled.

## Next steps

1. Commit and push this PR metadata.
2. Wait for every check attached to the resulting exact PR head, including
   required PR-only dependency security.
3. Reconfirm the full diff, base, head, mergeability, conversations, and
   security results.
4. If and only if everything is green, mark PR #55 Ready and perform the
   authorized squash merge without deleting its source branch.
5. Verify the resulting `origin/main` commit and post-merge CI; do not deploy.
