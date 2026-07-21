# task-dependency-security-github-native handoff
Signature: HND task-dependency-security-github-native [ready] topics: ci, security, tracker → predecessor: 2026-07-21-090947-task-dependency-security-github-native-handoff

## Durable — safe to cite later

- The repository owner explicitly authorized merging PR #34. Project policy requires a final metadata commit, complete latest-head green, GitHub Ready transition, and squash merge while retaining the source branch.
- Head `7559e712c86b6dd3f270b370794a516f29cf8127` passed Repository policy, Maven verify, required dependency-security, Java security/CodeQL, Semgrep, and informational Snyk before this ready checkpoint.
- PR #32 and PR #33 remain closed unmerged. Issue #31 remains open and is linked by `Resolves #31` in PR #34.

## Snapshot at 2026-07-21T09:15Z — re-verify live before use

- PR #34: open Draft, mergeable and clean, base `8940abe2dd92db6450f1934649581d4e00b6d1a1`, head `7559e712c86b6dd3f270b370794a516f29cf8127`, full check suite green.
- Local and remote source branch matched and the worktree was clean before this metadata commit.
- Branch protection remained strict with required contexts `Repository policy`, `verify`, and `dependency-security`.

## Next steps — conditional, each requires the stated live check

1. Push this metadata-only ready checkpoint and require the complete check suite to return green on its new exact HEAD.
2. Mark PR #34 Ready, re-check mergeability and base/head identity, then perform the authorized squash merge without deleting the source branch.
3. Verify the merge commit on `main`, automatic Issue #31 closure, retained source branch, post-merge CI, and trusted-main Dependency Submission.
