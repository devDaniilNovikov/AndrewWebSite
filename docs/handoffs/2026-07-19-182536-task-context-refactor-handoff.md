# task-context-refactor handoff
Signature: HND task-context-refactor [ready] topics: memory, process, tracker → predecessor: 2026-07-19-180745-task-context-refactor-handoff.md

## Durable — safe to cite later

- The supplied v2 role context, v3 memory, and thin `docs/SPEC.md` routing
  index are consolidated in one non-stacked task based on merge `c703ae7`.
- The manual twelve-file replacement set matched the expected scope. Rebase
  conflicts in the Codex role, Git Flow, and handoff index preserved the
  supplied v2/v3 structure while live CI and merge state were reconciled.
- Mechanical fixes swapped the misplaced memory README contents back to
  their intended paths, filled four verified template values, shortened one
  over-budget signature, updated shifted tracker references, and removed one
  trailing blank line.
- The user authorized `product` as a topic tag; DEC-20260719-006 records the
  decision and Q-20260719-005 is resolved.
- T1, T3, and T7 are cancelled and absorbed here. T2 remains a separate
  unpublished local task. T4 and T6 remain separate; T5 waits for this PR to
  merge. No absorbed local branch was merged or opened as a PR.
- Self-review, specification review, and quality/security review found no
  remaining Critical or Important issue. LES-20260719-009 records the observed
  gap between a successful CodeQL workflow and pending server-side processing.

## Snapshot at 2026-07-19T18:25Z — re-verify live before use

- `origin/main` is `c703ae78bcd14cfda2a6ae17085eb6dfbc5edb4a`.
- Draft PR #24 is mergeable at reviewed head `7457b0e`; this final metadata
  commit requires its own fresh checks before Draft is removed.
- Local verification passed: exact clean-archive checklist, all relative
  links and closed tags, all signature lengths, hot layer at 65 lines,
  scope/attribution/credential-pattern review, `git diff --check`, and
  `./mvnw -B verify` with 32 tests and the JaCoCo gate.
- At head `7457b0e`, PR CI run `29698424354`, Semgrep run `29698424360`,
  push CI/Java-security run `29698423154`, and Snyk succeeded. The separate
  GitHub Advanced Security CodeQL processing check was still queued at this
  snapshot after its workflow Analyze step succeeded.
- `TASKS.md` records the task as `ready`; merge remains unauthorized.
- Untracked `prompts/` and ignored `.DS_Store` files remain outside the diff.

## Next steps — conditional, each requires the stated live check

1. Verify the final pushed head equals the PR head, remains based on current
   `origin/main`, and has green required CI plus completed security checks;
   only then remove Draft.
2. Merge only after a separate explicit user command and a final live check
   of head, base, conversations, reviews, checks, and branch protection.
3. After an authorized merge, the next controller reconciles this task to
   `merged`, collapses its active chain into `## Closed chains`, and retains
   the source branch under the canonical policy.
