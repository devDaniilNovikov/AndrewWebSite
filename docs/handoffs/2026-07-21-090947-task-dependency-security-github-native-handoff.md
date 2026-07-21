# task-dependency-security-github-native handoff
Signature: HND task-dependency-security-github-native [draft_pr] topics: ci, security, tracker → predecessor: 2026-07-21-090424-task-dependency-security-github-native-handoff

## Durable — safe to cite later

- Draft PR #34 reached full green on exact head `90b9960cb7fabc89135ad4dcd421c4d00ec52766`: Repository policy, Maven verify, required dependency-security, Java security/CodeQL, Semgrep, and informational Snyk succeeded; event-inapplicable jobs skipped as designed.
- Under the repository owner's explicit green-gated authorization, PR #32 and PR #33 were closed unmerged with comments linking their GitHub-native successor, PR #34.
- Issue #31 remains open. PR #34 contains `Resolves #31`, so GitHub will close the issue when PR #34 is merged; Ready and merge still require a separate explicit owner command.

## Snapshot at 2026-07-21T09:09Z — re-verify live before use

- PR #34: open Draft, merge state clean, head `90b9960cb7fabc89135ad4dcd421c4d00ec52766`, all reported CI/security checks green.
- PR #32 and PR #33: closed, unmerged. Issue #31: open.
- Remote `main`: `8940abe2dd92db6450f1934649581d4e00b6d1a1`.

## Next steps — conditional, each requires the stated live check

1. Push this metadata-only tracker/handoff commit, then require the complete check set to return green on the resulting new latest HEAD.
2. On any failure, diagnose and fix in a new commit without weakening the gate; repeat latest-head review and checks.
3. Do not mark Ready, merge, close Issue #31 manually, remove the NVD secret, or change branch protection without a separate explicit owner command.
