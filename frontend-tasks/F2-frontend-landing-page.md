# F2 — Single-page landing preview

## Objective

Implement the complete responsive `/` preview from the user's external visual
reference while keeping every unverified fact visibly non-production.

## Ownership and prerequisites

- **Owner:** Codex.
- **Prerequisite:** F1 and `task-frontend-ci-gates` merged; start from fresh
  `origin/main`.
- **Owned paths:** `frontend/**`, the corrected preview contract,
  tracker/index reconciliation, and the task-specific handoff. Source
  photography remains outside Git.
- **Sources:** [preview design](../docs/frontend/landing-preview.md),
  [product brief](../docs/product/technical-brief.ru.md),
  [SPEC](../docs/SPEC.md), and [Git Flow](../.agents/workflows/GIT_FLOW.md).

## Tasks

1. Build the semantic header, light hero with media placeholder, benefit
   strip, equipment, services, completed work, repair CTA, pricing, request
   process, team/company, planned maintenance, reviews, contact, form shell,
   and footer from the corrected 724 by 2172 reference, with anchors
   `#equipment`, `#works`, `#pricing`, `#about`, and `#contact`.
2. Implement the responsive grids, sticky mobile header, keyboard-complete
   drawer, CTA-to-form behavior, restrained reveals, and reduced-motion path.
3. Add component, accessibility, keyboard, responsive, anchor-navigation, and
   placeholder-policy tests; verify preview export. Keep visual comparison
   screenshots temporary and outside Git.

## Acceptance

- The page matches the reference's composition, palette, rhythm, and density
  at desktop width and remains coherent at tablet and mobile widths.
- No screenshot phone, price, case, review, hours, logo, staff identity,
  photograph, or unreadable copy is presented as real.
- The header phone slot is not a `tel:` link until a verified number exists.
- Format, lint, strict typecheck, tests, export, dependency audit, secret scan,
  frontend CI, and Codex review pass before Ready.
