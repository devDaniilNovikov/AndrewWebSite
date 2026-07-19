# F4 — Trust and proof content

## Objective

Add verified work examples, reviews, media, and credibility sections with
traceable provenance and safe, performant presentation.

## Ownership and prerequisites

- **Owner:** frontend agent assigned by the user.
- **Prerequisite:** F3 merged; real cases, reviews, media usage rights, source
  links, and descriptive copy confirmed by the user.
- **Owned paths:** `frontend/**` and the task-specific handoff. Coordinate
  tracker and shared-memory changes with Codex.
- **Sources:** [product brief](../docs/product/technical-brief.ru.md),
  [SPEC](../docs/SPEC.md), and [Git Flow](../.agents/workflows/GIT_FLOW.md).

## Tasks

1. Implement reusable case, review, and media components using only supplied
   verified material and provenance metadata.
2. Add responsive image sizing, meaningful alternatives, lazy loading where
   appropriate, safe external links, and layouts that remain usable when a
   content collection is small.
3. Add provenance, no-invented-content, component, accessibility, media, and
   relevant E2E tests; verify the production export.

## Acceptance

- No synthetic review, case, brand relationship, certification, result, or
  customer identity is presented as real.
- Media rights and public attribution are confirmed outside code before Ready;
  PII and private client data are excluded.
- Format, lint, strict typecheck, tests, build, dependency audit, secret scan,
  required CI, and Codex review pass.
- The PR stops at Ready and requires separate merge authorization.
