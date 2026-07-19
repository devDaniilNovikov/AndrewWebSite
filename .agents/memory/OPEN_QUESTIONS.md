# Open questions

Pending decisions with an owner. **Tripwire → question:** every "stopped and
asked the user" event creates a record here before or with the ask; the
answer resolves it with a link to the resulting decision or handoff. Before
asking the user anything, `grep` this file first. Startup reads `## Active`
only. Signature format: [`README.md`](README.md).

## Active

```text
Q-20260718-001 [active] security: may the app trust X-Forwarded-For? blocked on Timeweb proxy verification → user
Q-20260718-002 [active] ci: T4 owner — Jules dispatch or direct Codex fix for ci.yml timeouts → user
Q-20260718-003 [active] product: sanitized technical brief needed to commit docs/product/technical-brief.ru.md → user
Q-20260718-004 [active] tracker: approve frontend track granularity F1-F6 before planning row 17 deps → user
Q-20260719-005 [active] memory: add product to the topic vocabulary or retag Q-20260718-003? → user
```

## Records

## Q-20260718-001 — Trusting forwarded client addresses

`Q-20260718-001 [active] security: may the app trust X-Forwarded-For? blocked on Timeweb proxy verification → user`

- **Question:** may rate limiting and logging trust `X-Forwarded-For`, and
  with which trusted proxy CIDRs?
- **Why now:** blocks the header-trust part of backend HTTP security; until
  verified, the connection address plus global fallback applies.
- **Owner:** user (platform verification) with Codex.
- **Review-by:** completion of Timeweb proxy behavior verification.

## Q-20260718-002 — Ownership of the small CI hardening fix

`Q-20260718-002 [active] ci: T4 owner — Jules dispatch or direct Codex fix for ci.yml timeouts → user`

- **Question:** run the timeout/annotation fix through the Jules dispatch
  protocol (canonical CI owner) or as a direct Codex `fix-*` (precedent:
  restore `6bde363`)?
- **Why now:** blocks dispatching the fix; two-line change vs dispatch
  overhead.
- **Owner:** user.

## Q-20260718-003 — Committing the sanitized technical brief

`Q-20260718-003 [active] product: sanitized technical brief needed to commit docs/product/technical-brief.ru.md → user`

- **Question:** user supplies a sanitized brief (no PII, credentials, or
  non-public client data) for in-repo commitment as the canonical product
  source.
- **Why now:** remote agents cannot read the workstation-only original;
  requirements are unversioned until committed.
- **Owner:** user.

## Q-20260718-004 — Frontend track granularity

`Q-20260718-004 [active] tracker: approve frontend track granularity F1-F6 before planning row 17 deps → user`

- **Question:** approve, adjust, or re-cut the proposed frontend task rows
  before they enter `TASKS.md` and row 17's prerequisites.
- **Why now:** the frontend track is unplanned while it could run parallel
  to backend phases; row 17 depends on it.
- **Owner:** user with Codex.

## Q-20260719-005 — Topic vocabulary for product-source questions

`Q-20260719-005 [active] memory: add product to the topic vocabulary or retag Q-20260718-003? → user`

- **Question:** should `product` be added to the closed topic vocabulary via
  a decision record, or should Q-20260718-003 use an existing topic tag?
- **Why now:** the v3 README defines a closed vocabulary but the supplied
  active question uses `product`, so Ready would otherwise certify two
  contradictory memory rules.
- **Owner:** user with Codex.
- **Review-by:** resolution of `task-context-refactor` review findings.

## Entry rules

A record = signature line, Question, Why now, Owner, optional Review-by.
Resolve via status plus a link to the resulting DEC or handoff; keep the
historical record; move superseded bodies to [`archive/`](archive/).
