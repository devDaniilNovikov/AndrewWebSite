# fix-mobile-navigation-scroll Ready handoff

Signature: HND fix-mobile-navigation-scroll [ready] topics: frontend, accessibility, testing, security, tracker, incident → predecessor: `2026-08-08-062129-fix-mobile-navigation-scroll-draft-handoff.md`

## Durable — safe to cite later

- The user explicitly authorized Ready and squash-merge of Draft PR #69 after
  exact-head protected and security checks remain green.
- The hotfix restores deterministic mobile navigation into the single-page
  landing composition, including repeated selection from a product route and
  release of the document scroll lock before anchor movement.
- The implementation and additive correction preserve the original strict test
  timeouts. Production deployment, backend/API behavior, PR #30, release
  readiness blockers, branch retention, and protected-branch rules are not
  changed.

## Verified snapshot at 2026-08-08T06:30:36Z — re-verify live before use

- `origin/main` and the PR base remained
  `25ce867e79348f91e3021cb10714402b7c1b66b7`; the clean local and remote PR
  branch matched `f3b5cd5f938a4a4db203edc542165aa2f29007d3`.
- GitHub reported PR #69 open Draft, `MERGEABLE`, and `CLEAN`, with zero
  comments, reviews, or unresolved review threads.
- Exact-head Repository policy, both `verify` runs, both Frontend quality runs,
  dependency-security, Java security, CodeQL, Semgrep, and Snyk passed. Event-
  and path-specific skips had corresponding successful contexts.
- PR #30 remained open Draft at its unchanged head and timestamp; GitHub
  deployments remained zero.
- Local implementation, accessibility, performance, deterministic export,
  Maven, audit, secret, specification, correctness, and security evidence is
  recorded in the predecessor handoffs.

## Conditional continuation — re-verify live

1. Commit and push this Ready checkpoint without force; wait for every required
   and security check on the new exact head.
2. Mark PR #69 Ready only while it remains current, mergeable, green, and
   conversation-free; wait for Ready-event checks on the same SHA.
3. Squash-merge with exact-head matching and Codex attribution, without admin
   bypass, auto-merge, rebase, merge commit, deployment, or branch deletion.
4. Confirm the new `main`, successful post-merge CI/security, retained source
   branch, unchanged PR #30, and zero deployments.
