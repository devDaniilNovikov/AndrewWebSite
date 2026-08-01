# F2 — Single-page landing preview

## Objective

Implement the complete responsive `/` preview from the canonical visual
reference while keeping every unverified fact visibly non-production.

## Ownership and prerequisites

- **Owner:** Codex.
- **Prerequisite:** F1 and `task-frontend-ci-gates` merged; start from fresh
  `origin/main`.
- **Owned paths:** `frontend/**` and the task-specific handoff.
- **Sources:** [preview design](../docs/frontend/landing-preview.md),
  [product brief](../docs/product/technical-brief.ru.md),
  [SPEC](../docs/SPEC.md), and [Git Flow](../.agents/workflows/GIT_FLOW.md).

## Tasks

1. Build the semantic header, hero, benefit strip, equipment, work, pricing,
   company, review, contact, form-shell, and footer sections with anchors
   `#equipment`, `#works`, `#pricing`, `#about`, and `#contact`, plus explicit
   media/content placeholders.
2. Implement the responsive grids, sticky mobile header, keyboard-complete
   drawer, CTA-to-form behavior, restrained reveals, and reduced-motion path.
3. Add component, accessibility, keyboard, responsive, anchor-navigation,
   placeholder-policy, and visual-regression tests; verify preview export.

## Acceptance

- The page matches the reference's composition, palette, rhythm, and density
  at desktop width and remains coherent at tablet and mobile widths.
- No screenshot phone, price, case, review, hours, logo, or unreadable copy is
  presented as real.
- The header phone slot is not a `tel:` link until a verified number exists.
- Format, lint, strict typecheck, tests, export, dependency audit, secret scan,
  frontend CI, and Codex review pass before Ready.
