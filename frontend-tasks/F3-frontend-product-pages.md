# F3 — Product pages

## Objective

Implement the confirmed product routes and page content from the canonical
brief without converting recommendations, assumptions, or open questions into
approved claims.

## Ownership and prerequisites

- **Owner:** Codex when separately authorized.
- **Prerequisite:** preview F6 merged; multi-page expansion separately
  authorized; route slugs, page copy, prices, legal text, and required assets
  verified in canonical product sources.
- **Owned paths:** `frontend/**` and the task-specific handoff.
- **Sources:** [product brief](../docs/product/technical-brief.ru.md),
  [SPEC](../docs/SPEC.md), and [Git Flow](../.agents/workflows/GIT_FLOW.md).

## Tasks

1. Implement the confirmed page and route set with semantic heading structure,
   reusable content sections, navigation integration, and intentional 404
   behavior.
2. Render only verified business claims, pricing, geography, guarantees,
   requisites, and legal copy. Stop on missing required content; never invent
   production text.
3. Add route, content-status, link, component, accessibility, and critical
   navigation E2E tests; verify the production export.

## Acceptance

- Every shipped route and production claim traces to a confirmed canonical
  source; recommendations remain non-binding.
- No fabricated prices, guarantees, service areas, legal text, or company
  facts appear in the output.
- Format, lint, strict typecheck, tests, build, secret scan, required CI, and
  Codex content/specification review pass.
- The PR stops at Ready and requires separate merge authorization.
- This deferred task is not a prerequisite for the approved single-page
  preview or its F6 hardening task.
