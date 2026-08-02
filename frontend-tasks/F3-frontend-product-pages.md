# F3 — Provisional product pages

## Objective

Extend the approved placeholder-first frontend preview with the user-selected
product route set while keeping every unverified business fact visibly
non-production.

## Ownership and prerequisites

- **Owner:** Codex under the user's 2026-08-02 authorization.
- **Prerequisite:** preview F6 merged as `6511017`; the exact route set,
  placeholder-only content policy, and Draft publication are authorized.
  Verified production copy, prices, legal text, proof, and media remain
  deferred blockers rather than prerequisites for this preview task.
- **Owned paths:** `frontend/**`, this task brief, the frontend preview
  contract and canonical index, plus task-specific tracker and handoff
  metadata.
- **Sources:** [product brief](../docs/product/technical-brief.ru.md),
  [SPEC](../docs/SPEC.md), and [Git Flow](../.agents/workflows/GIT_FLOW.md).

## Tasks

1. Export `/uslugi`, `/remont-torgovogo-holodilnogo-oborudovaniya`,
   `/remont-ledogeneratorov`, `/o-kompanii`, `/raboty`, `/tseny`, and
   `/kontakty` with reusable semantic sections, shared navigation, and the
   existing custom 404 behavior.
2. Use only confirmed general service categories from the product brief.
   Render missing prices, guarantees, requisites, cases, reviews, staff facts,
   contacts, and media as explicit placeholders; never promote reference-image
   examples into business facts.
3. Preserve the verified-local-media and static-export boundaries. Add route,
   content-status, link, component, accessibility, export, and critical
   navigation E2E tests before Draft publication.

## Acceptance

- All seven routes export deterministically, keep one `h1`, inherit
  `noindex, nofollow`, remain same-origin only, and preserve the real static
  404.
- No fabricated phone, price, guarantee, service-area detail, legal text,
  company fact, case, review, staff identity, or photograph appears.
- Every media location remains a `MediaSlot`; future photographs are accepted
  only through `VerifiedLocalPhoto` under `/media/verified/*` after provenance
  and alt text are confirmed.
- The production-readiness manifest remains unchanged and production export
  continues to fail closed on every missing blocker.
- Format, lint, strict typecheck, coverage, tests, preview build, Lighthouse,
  dependency audit, Maven verify, secret/media scans, required CI, and Codex
  specification/code/security reviews pass.
- The PR remains Draft. Ready, merge, F4, JAR integration, and deployment each
  require separate authorization.
