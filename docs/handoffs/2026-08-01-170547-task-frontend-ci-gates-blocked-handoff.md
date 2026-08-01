# task-frontend-ci-gates blocked dispatch handoff

Signature: HND task-frontend-ci-gates [in_progress] topics: frontend, ci, testing, tracker, incident → predecessor: `2026-08-01-165805-task-frontend-ci-gates-dispatch-handoff.md`

## Durable — safe to cite later

- Owner-authored [Issue #61](https://github.com/devDaniilNovikov/AndrewWorkWebSite/issues/61)
  has exactly the `jules-action` label. Its author and labeling actor match
  `JULES_ALLOWED_ACTOR`, and the automation guard ran exactly once.
- Guarded workflow
  [run 30709431088](https://github.com/devDaniilNovikov/AndrewWorkWebSite/actions/runs/30709431088)
  failed before invoking the Jules API. No Jules session, implementation
  branch, or F1A pull request was created; the API key was masked and was not
  the cause of failure.
- The pinned upstream action v1.0.0 declares `include_last_commit` and
  `include_commit_log` as false, but composite action inputs are strings. Its
  raw `if: inputs.include_*` expressions therefore treated `false` as truthy,
  appended the large F1 squash diff and commit log, then passed the entire
  prompt through one `jq --arg` process argument. Linux rejected that argument
  with `Argument list too long`.
- Context7 and the official upstream repository showed v1.0.0 at commit
  `bff7875eaa123cac6742b7cfc51005b95ba4d566` is still the newest release;
  there is no upstream fixed version to pin. Re-running the same workflow is
  deterministic failure, not a useful retry.

## Snapshot at 2026-08-01T17:05:47Z — re-verify live before use

- Controller branch `task-frontend-ci-gates` remained clean and published at
  `70f3cf597041e24971c48238e67a97e506224396`, directly after merged F1
  `d0346b716772f2d1a3debe5692c604950c4b143f`.
- Issue #61 remained open. It had one label, `jules-action`; do not toggle or
  reapply that label until a different authorized dispatch path is selected.
- F1A remained `in_progress`; no CI implementation or branch-protection
  change occurred. Production deployment remained disabled and PR #30 was
  untouched.

## Decision required before continuation

Choose one explicitly:

1. Authorize the documented native Jules GitHub App path: remove
   `jules-action`, update the task dispatch exception, and apply only the
   mutually exclusive `jules` label to Issue #61.
2. Transfer ownership of a minimal Jules automation hotfix to Codex. That
   separate fix must use explicit string comparisons and file-based JSON
   payload ingestion, pass workflow/security review, merge to `main`, and
   then retrigger Issue #61 through `jules-action`.

Do not silently implement F1A as Codex, use both labels, expose or rotate the
API key, bypass branch protection, retry the unchanged failed workflow, or
deploy the preview.
