# Open questions

Pending decisions with an owner. **Tripwire → question:** every "stopped and
asked the user" event creates a record here before or with the ask; the
answer resolves it with a link to the resulting decision or handoff. Before
asking the user anything, `grep` this file first. Startup reads `## Active`
only. Signature format: [`README.md`](README.md).

## Active

```text
Q-20260718-001 [active] security: may the app trust X-Forwarded-For? blocked on Timeweb proxy verification → user
Q-20260720-006 [active] product: user provides verified F2-F4 content package before F2 starts → user
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

`Q-20260718-002 [resolved] ci: T4 owner is Codex for the narrow ci.yml hardening fix → T4 handoff`

- **Question:** run the timeout/annotation fix through the Jules dispatch
  protocol (canonical CI owner) or as a direct Codex `fix-*` (precedent:
  restore `6bde363`)?
- **Why now:** blocks dispatching the fix; two-line change vs dispatch
  overhead.
- **Owner:** user.
- **Resolution:** the user selected a direct Codex fix with narrow scope;
  see [`2026-07-19-194032-fix-ci-timeouts-annotations-handoff.md`](../../docs/handoffs/2026-07-19-194032-fix-ci-timeouts-annotations-handoff.md).

## Q-20260718-003 — Committing the sanitized technical brief

`Q-20260718-003 [resolved] product: sanitized technical brief committed as canonical product source → 6cd858e`

- **Question:** user supplies a sanitized brief (no PII, credentials, or
  non-public client data) for in-repo commitment as the canonical product
  source.
- **Why now:** remote agents cannot read the workstation-only original;
  requirements are unversioned until committed.
- **Owner:** user.
- **Resolution:** the user approved the sanitized workstation source, which
  was normalized without changing requirement statuses and committed as the
  [canonical product brief](../../docs/product/technical-brief.ru.md).

## Q-20260718-004 — Frontend track granularity

`Q-20260718-004 [resolved] tracker: frontend track approved as six atomic tasks → DEC-20260719-007`

- **Question:** approve, adjust, or re-cut the proposed frontend task rows
  before they enter `TASKS.md` and row 17's prerequisites.
- **Why now:** the frontend track was unplanned while it could run parallel
  to backend phases; static JAR integration depends on its output.
- **Owner:** user with Codex.
- **Resolution:** the user approved the six-task F1-F6 split with explicit
  prerequisites and separate non-stacked PRs; see DEC-20260719-007.

## Q-20260719-005 — Topic vocabulary for product-source questions

`Q-20260719-005 [resolved] memory: product added to the topic vocabulary → DEC-20260719-006`

- **Question:** should `product` be added to the closed topic vocabulary via
  a decision record, or should Q-20260718-003 use an existing topic tag?
- **Why now:** the v3 README defines a closed vocabulary but the supplied
  active question uses `product`, so Ready would otherwise certify two
  contradictory memory rules.
- **Owner:** user with Codex.
- **Review-by:** resolution of `task-context-refactor` review findings.
- **Resolution:** the user authorized adding `product`; see
  DEC-20260719-006.

## Q-20260720-006 — Verified frontend content package

`Q-20260720-006 [active] product: user provides verified F2-F4 content package before F2 starts → user`

- **Question:** provide and approve one canonical content package containing:
  - legal and public company names;
  - the phone display format and matching `tel:` link;
  - navigation labels and their order;
  - approved route slugs;
  - F3 page copy or explicit placeholder decisions;
  - publishable price wording and pricing policy;
  - personal-data policy, form consent, and company requisites;
  - cases and reviews with publication rights and sources;
  - photographs and media with usage rights, including the first-screen asset;
  - publishable service-area and warranty wording.
- **Why now:** F1 can proceed without business content, but missing verified
  inputs block F2-F4 Ready gates and would otherwise stop the frontend track
  repeatedly.
- **Owner:** user.
- **Review-by:** before `task-frontend-shell` starts.

## Entry rules

A record = signature line, Question, Why now, Owner, optional Review-by.
Resolve via status plus a link to the resulting DEC or handoff; keep the
historical record; move superseded bodies to [`archive/`](archive/).
