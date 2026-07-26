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
LES-20260719-009 [active] ci: workflow success can precede CodeQL processing completion → run 29698729563
LES-20260721-010 [active] security: inspect servlet filters before changing Security matchers → handoff 114211
LES-20260725-011 [active] security: validate lexical JSON before general Jackson conversion → PR #40
LES-20260725-012 [active] ci: owner-approved branch prefixes can drift from repository gates → PR #43
LES-20260725-013 [active] process: separate current defects from unstarted roadmap contracts → handoff 205559
LES-20260726-014 [active] process: reconcile squash attribution and tracker state after merge → merge 0b0a62a
```

## Records

## LES-20260726-014 — Reconcile squash attribution and tracker state after merge

`LES-20260726-014 [active] process: reconcile squash attribution and tracker state after merge → merge 0b0a62a`

- **Date:** 2026-07-26
- **Lesson:** a green pre-merge branch cannot contain the final squash SHA or
  prove the final merge message. Merge `0b0a62a` therefore left the tracker
  and active handoff at their pre-publication state, while an overridden
  squash body dropped the required Codex attribution. Prepare the exact merge
  footer before the merge action, then use a fresh post-merge reconciliation
  task for facts that only exist after GitHub creates the squash commit.
- **Evidence:** merged
  [PR #45](https://github.com/devDaniilNovikov/AndrewWebSite/pull/45),
  squash commit `0b0a62acfce09857807c4eb11e92795af3c20576`, and the
  [rollback handoff](../../docs/handoffs/2026-07-26-055452-fix-revert-code-review-hardening-handoff.md).
- **Applicability:** every squash merge whose tracker or handoff records the
  final PR state, merge SHA, checks, or agent attribution.
- **Review-by:** any change to the squash-merge, attribution, tracker, or
  post-merge reconciliation protocol.

## LES-20260725-013 — Current defects are distinct from unstarted roadmap contracts

`LES-20260725-013 [active] process: separate current defects from unstarted roadmap contracts → handoff 205559`

- **Date:** 2026-07-25
- **Lesson:** before scoring a staged implementation, trace an apparent gap
  through the live tracker and the canonical architecture allocation. The
  first review correctly found the current Docker test-skip violation, but
  also treated final dependency-aware readiness as a defect even though that
  behavior is explicitly assigned to the blocked
  `task-backend-observability`. Fix defects in the authorized stage; keep
  unstarted roadmap contracts visible without silently expanding scope.
- **Evidence:** the accepted and rejected findings, their canonical
  traceability, and the corrected implementation are recorded in the
  [review-hardening handoff](../../docs/handoffs/2026-07-25-205559-fix-code-review-hardening-handoff.md)
  and fix commit `e9fe113`.
- **Applicability:** reviews of incremental work where end-state architecture
  documents intentionally describe behavior allocated to later tracker
  tasks.
- **Review-by:** any change to the roadmap allocation, tracker dependency, or
  authorization of `task-backend-observability`.

## LES-20260725-012 — Owner-approved branch prefixes can drift from repository gates

`LES-20260725-012 [active] ci: owner-approved branch prefixes can drift from repository gates → PR #43`

- **Date:** 2026-07-25
- **Lesson:** distinguish the owner's branch policy from its current CI
  enforcement before classifying a branch name as prohibited. The
  owner-approved `codex/` prefix produced an immediate Repository policy
  failure because the live gate had not been aligned with that decision.
- **Evidence:** rejected Draft
  [PR #43](https://github.com/devDaniilNovikov/AndrewWebSite/pull/43) and its
  [Repository policy run](https://github.com/devDaniilNovikov/AndrewWebSite/actions/runs/30169235381);
  the owner-directed correction is recorded in successor
  [PR #44](https://github.com/devDaniilNovikov/AndrewWebSite/pull/44).
- **Applicability:** every new branch created through a harness whose default
  naming convention may differ from the repository's CI enforcement.
- **Review-by:** alignment of the repository branch-name gate with the
  owner-approved prefixes, or any later branch-policy decision.

## LES-20260725-011 — General-purpose conversion can widen a strict JSON boundary

`LES-20260725-011 [active] security: validate lexical JSON before general Jackson conversion → PR #40`

- **Date:** 2026-07-25
- **Lesson:** a strict public JSON contract cannot delegate lexical acceptance
  to general-purpose value conversion. Jackson UUID and enum conversion
  accepted non-canonical representations, while tree parsing silently retained
  one duplicate key. Validate the raw token grammar and reject duplicate keys
  before honeypot classification or any application work.
- **Evidence:** post-merge review of
  [PR #38](https://github.com/devDaniilNovikov/AndrewWebSite/pull/38)
  reproduced all three boundary gaps; the attributed replacement
  [PR #40](https://github.com/devDaniilNovikov/AndrewWebSite/pull/40) and its
  [handoff](../../docs/handoffs/2026-07-25-042750-fix-leads-api-json-boundary-replacement-handoff.md)
  record the regression tests and corrected behavior.
- **Applicability:** public request deserializers that promise exact lexical
  forms, strict enums, or duplicate-key rejection.
- **Review-by:** any Jackson configuration, request DTO/deserializer, or public
  JSON-boundary change.

## LES-20260721-010 — Servlet filters can own route status before Security

`LES-20260721-010 [active] security: inspect servlet filters before changing Security matchers → handoff 114211`

- **Date:** 2026-07-21
- **Lesson:** when route status contracts fail under Spring Boot/Security,
  inspect the full servlet filter chain before iterating on authorization
  matchers. Earlier filters can determine CORS preflight and actuator fallback
  responses before the Security matcher under review can apply.
- **Evidence:** the [tripwire handoff](../../docs/handoffs/2026-07-21-110405-task-backend-http-security-handoff.md)
  records three matcher attempts with the same symptoms; the
  [successor handoff](../../docs/handoffs/2026-07-21-114211-task-backend-http-security-handoff.md)
  records the accepted filter and framework-native backlog task.
- **Applicability:** any future Spring Security route-authorization or
  CORS/filter-order change.
- **Review-by:** `fix-http-security-framework-native-deny` or any change to
  the HTTP boundary filter ordering.

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

## LES-20260719-009 — CodeQL workflow and processing are separate states

`LES-20260719-009 [active] ci: workflow success can precede CodeQL processing completion → run 29698729563`

- **Date:** 2026-07-19
- **Lesson:** a successful CodeQL workflow job proves that SARIF was uploaded,
  but not that GitHub Advanced Security finished processing it. Ready review
  checks the downstream CodeQL check-run separately.
- **Evidence:** push CI run `29698729563` job `88223961420` uploaded SARIF,
  timed out waiting for processing, and completed successfully while check-run
  `88224070561` remained queued.
- **Applicability:** CodeQL and other scanners with asynchronous server-side
  result processing.
- **Review-by:** any change to the CodeQL action, processing wait, or Ready
  security-review protocol.
