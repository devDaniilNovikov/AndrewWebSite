# Lessons

**Experience-first rule:** a lesson is created only from a real incident,
failure, or surprising outcome, with linked evidence (handoff, commit, CI
run). Policy restatements are not lessons — a rule without an incident
belongs in the canonical process documents. Startup reads `## Active` only;
bodies are cold. Signature format: [`README.md`](README.md).

## Active

```text
LES-20260718-005 [active] ci: diff agents' later pushes against the last reviewed tree, not green checks → 6bde363
LES-20260718-006 [active] process: verify platform settings, not only policy documents → merge 288fdd5
LES-20260718-007 [active] ci: provision and prove external-scan credentials before a gate becomes required → run #100
LES-20260718-008 [active] memory: handoff filename timestamps are UTC; mixed zones broke ordering → HANDOFFS rows 7-9
```

## Records

## LES-20260718-005 — Implementer agents can silently revert reviewed gates

`LES-20260718-005 [active] ci: diff agents' later pushes against the last reviewed tree, not green checks → 6bde363`

- **Date:** 2026-07-18
- **Lesson:** a follow-up push from an autonomous implementer can undo an
  explicitly reviewed security configuration while checks stay green. Before
  Ready, final review diffs against the last reviewed tree.
- **Evidence:** Jules push `05f65d2` restored the rejected non-blocking
  Dependency Review job and removed the approved OWASP configuration on
  PR #23; Codex restored the reviewed tree in `6bde363` (ready handoff
  `2026-07-18-110204`).
- **Applicability:** every PR where an implementer pushes after review began.
- **Review-by:** any change to the final-review protocol in `AGENTS.md`.

## LES-20260718-006 — Verify platform settings, not only policy documents

`LES-20260718-006 [active] process: verify platform settings, not only policy documents → merge 288fdd5`

- **Date:** 2026-07-18
- **Lesson:** a GitHub setting (automatic head-branch deletion) silently
  violated the documented branch-retention policy. A policy that depends on
  a platform setting is incomplete until the live setting is verified.
- **Evidence:** branch-retention handoff `2026-07-18-101804`, remediation
  merge `288fdd5`, DEC-20260718-005.
- **Applicability:** any rule enforced by a GitHub or platform setting.
- **Review-by:** any repository-settings change.

## LES-20260718-007 — External scan gates need credentials before enablement

`LES-20260718-007 [active] ci: provision and prove external-scan credentials before a gate becomes required → run #100`

- **Date:** 2026-07-18
- **Lesson:** enabling OWASP Dependency-Check without an NVD API key
  produced HTTP 429 rate-limit failures. A required gate depending on an
  external service has its credential provisioned and proven in a fresh CI
  run before it becomes required.
- **Evidence:** CI run #100 failure context; `NVD_API_KEY` provisioning
  (dispatch handoff `2026-07-18-062911`).
- **Applicability:** every externally backed CI gate (NVD, Snyk, CodeQL
  packs, registries).
- **Review-by:** adding or promoting any externally backed required check.

## LES-20260718-008 — Handoff filename timestamps are UTC

`LES-20260718-008 [active] memory: handoff filename timestamps are UTC; mixed zones broke ordering → HANDOFFS rows 7-9`

- **Date:** 2026-07-18
- **Lesson:** mixed-timezone timestamps produced a non-monotonic sequence
  (`131926 → 062911 → 110204` on 2026-07-18), defeating the lexicographic
  ordering the filename format exists to provide. New filenames use UTC;
  historical names stay unchanged; the index order column is authoritative.
- **Evidence:** [`HANDOFFS.md`](HANDOFFS.md) chronology rows 7–9.
- **Applicability:** every new handoff.
- **Review-by:** any change to the handoff lifecycle.

