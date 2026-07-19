# task-context-refactor handoff
Signature: HND task-context-refactor [paused] topics: memory, process, tracker → predecessor: 2026-07-18-110204-ci-backend-gates-ready-handoff.md

## Durable — safe to cite later

- The user authorized `task-context-refactor` through Ready, but not merge.
- The supplied twelve-file v2/v3 context commit was rebased onto merge
  `c703ae78bcd14cfda2a6ae17085eb6dfbc5edb4a`; three overlapping context
  files retained the supplied v2/v3 structure while live state was reconciled
  separately.
- T1 and T3 exist only as unpublished local commits and are absorbed here
  together with T7. T2 remains a separate unpublished local task. T4 and T6
  remain separate; T5 starts only after this PR merges.
- Mechanical review fixes return the two memory README files to their
  intended paths, replace supplied template tokens with verified canonical
  values, shorten an over-budget signature, and update tracker row references.
- Q-20260719-005 records the unresolved conflict between the closed topic
  vocabulary and the `product` tag on Q-20260718-003.

## Snapshot at 2026-07-19T18:07Z — re-verify live before use

- `origin/main` is `c703ae78bcd14cfda2a6ae17085eb6dfbc5edb4a`.
- Local branch `task-context-refactor` is rebased; its published remote still
  points to the pre-rebase commit until the next authorized push.
- No PR exists for the branch. `TASKS.md` records the task as `in_progress`.
- Required branch checks are `Repository policy`, `verify`, and
  `dependency-security`; branch protection was verified strict.
- Untracked `prompts/` and ignored `.DS_Store` files remain outside the task
  diff.

## Next steps — conditional, each requires the stated live check

1. After the user resolves Q-20260719-005, apply only that decision and mark
   the question resolved.
2. Re-fetch `origin`; if the base is unchanged, run the complete context,
   link, signature, hot-layer, Maven, and security verification suite.
3. If verification and review pass, commit and push with lease, open the
   Draft PR, wait for fresh CI, update the tracker and handoff, then mark Ready.
