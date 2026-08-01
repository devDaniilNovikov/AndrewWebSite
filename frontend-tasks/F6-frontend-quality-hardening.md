# F6 — Frontend preview quality hardening

## Objective

Run the final single-page preview hardening pass and produce the reviewed
static-export contract consumed by later backend JAR integration.

## Ownership and prerequisites

- **Owner:** Codex.
- **Prerequisite:** F2 and F5 merged into fresh `origin/main`.
- **Owned paths:** `frontend/**` and the task-specific handoff. Backend,
  container, deployment, API, and CI architecture remain outside scope.
- **Sources:** [SPEC](../docs/SPEC.md),
  [preview design](../docs/frontend/landing-preview.md),
  [architecture](../docs/backend/architecture.md), and
  [Git Flow](../.agents/workflows/GIT_FLOW.md).

## Tasks

1. Audit `/` and the form flow for semantic structure, keyboard and
   screen-reader use, responsive behavior, reduced motion, metadata, anchors,
   real 404 behavior, safe failures, placeholder status, and preview noindex.
2. Optimize the deterministic export within the approved stack; prove a
   hosted preview cannot collect leads and production export fails while any
   required fact, legal text, price, proof item, or media asset is missing.
3. Run the complete unit, component, accessibility, visual, and E2E suites;
   document the exact package-manager command and `out/` path for static JAR
   integration.

## Acceptance

- Format, lint, strict typecheck, at least 80% coverage, full tests, preview
  export, dependency audit, secret scan, performance/accessibility review,
  frontend CI, and Codex final review pass.
- Playwright passes at 390 by 844, 768 by 1024, and 1440 by 900 without
  horizontal overflow; axe reports zero Critical or Serious findings.
- Preview Lighthouse scores at least 90 for Performance, Accessibility, and
  Best Practices. SEO remains intentionally excluded while preview noindex is
  active.
- `build:preview` creates `frontend/out/`; `build:production` is expected to
  fail until every production-content blocker is satisfied.
- No open Critical or Important accessibility, security, contract, privacy,
  content, or performance finding remains.
- Multi-page F3/F4 work and production content remain deferred and do not
  block completion of this safe preview.
- The PR stops at Ready and requires separate merge authorization.
