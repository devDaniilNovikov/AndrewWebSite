# F2 — Shared frontend shell

## Objective

Build the accessible responsive shell shared by all product pages: document
structure, navigation, header, footer, contact actions, and reusable layout
primitives.

## Ownership and prerequisites

- **Owner:** Claude Code.
- **Prerequisite:** F1 and `task-frontend-ci-gates` merged; verified company
  name, navigation labels, phone, and contact facts available in canonical
  product sources.
- **Owned paths:** `frontend/**` and the task-specific handoff. Coordinate
  tracker and shared-memory changes with Codex.
- **Sources:** [product brief](../product/technical-brief.ru.md),
  [SPEC](../SPEC.md), and [Git Flow](../../.agents/workflows/GIT_FLOW.md).

## Tasks

1. Implement the semantic root layout with `lang="ru"`, skip navigation,
   header, primary navigation, footer, and responsive layout primitives using
   only verified content.
2. Implement phone and form-entry CTAs, keyboard/focus behavior, mobile
   navigation, reduced-motion behavior, and active-route feedback.
3. Add component, accessibility, keyboard, responsive-navigation, and
   reduced-motion tests; verify the production export.

## Acceptance

- Shell behavior is keyboard complete, screen-reader coherent, responsive,
  and preserves a working call fallback.
- Missing company or contact facts block Ready rather than being invented.
- Format, lint, strict typecheck, tests, production build, dependency audit,
  secret scan, required CI, and Codex review pass.
- The PR is non-stacked, stops at Ready, and requires separate merge
  authorization.
