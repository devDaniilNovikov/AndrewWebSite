# F6 — Frontend quality hardening

## Objective

Run the final cross-page frontend hardening pass and produce the reviewed
static-export contract consumed later by backend JAR integration.

## Ownership and prerequisites

- **Owner:** frontend agent assigned by the user.
- **Prerequisite:** F3, F4, and F5 merged into fresh `origin/main`.
- **Owned paths:** `frontend/**` and the task-specific handoff. Backend,
  container, deployment, API, and CI architecture remain outside scope.
- **Sources:** [SPEC](../docs/SPEC.md),
  [product brief](../docs/product/technical-brief.ru.md),
  [architecture](../docs/backend/architecture.md), and
  [Git Flow](../.agents/workflows/GIT_FLOW.md).

## Tasks

1. Audit all routes and critical flows for semantic structure, keyboard and
   screen-reader use, responsive behavior, reduced motion, metadata, internal
   links, real 404 behavior, and safe failure fallbacks.
2. Optimize production assets and rendering within the approved stack; verify
   deterministic static output without introducing an analytics vendor,
   runtime server, or deployment behavior.
3. Run the complete frontend unit, component, accessibility, and E2E suites;
   document the package-manager command and output path needed by the later
   backend integration task.

## Acceptance

- Format, lint, strict typecheck, full tests, production export, dependency
  audit, secret scan, performance/SEO review, required CI, and Codex final
  review pass.
- No open Critical or Important accessibility, security, contract, content,
  or performance finding remains.
- The exported output is reproducible from a clean checkout and ready for
  `task-static-jar-integration`; this task does not modify the backend.
- The PR stops at Ready and requires separate merge authorization.
