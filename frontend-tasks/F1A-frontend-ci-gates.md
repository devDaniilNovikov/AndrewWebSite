# F1A — Frontend CI gates

## Objective

Add a stable frontend quality context that runs the exact commands declared by
the merged F1 manifest without changing frontend product behavior.

## Ownership and prerequisites

- **Owner:** Jules, through the separately authorized sanitized Issue flow.
- **Prerequisite:** F1 merged; explicit current user authorization for a new
  Jules dispatch and publication.
- **Owned paths:** the dedicated frontend CI workflow or narrow shared-CI
  changes, associated policy tests, and the task-specific handoff.
- **Sources:** merged `frontend/package.json`, lockfile, [SPEC](../docs/SPEC.md),
  [Jules automation](../.agents/workflows/JULES_AUTOMATION.md), and
  [Git Flow](../.agents/workflows/GIT_FLOW.md).

## Tasks

1. Install Node 24 and invoke the manifest-declared pnpm through Corepack with
   the exact frozen lockfile.
2. Run Prettier format check, ESLint, strict TypeScript typecheck, Vitest and
   Testing Library coverage, axe accessibility checks, Playwright smoke,
   dependency audit, and preview export when `frontend/**` changes.
3. Add or update the stable required context `Frontend quality` together with
   branch protection, then submit the implementation for Codex review.

## Acceptance

- Backend-only changes do not perform frontend installation, while every
  frontend change receives the stable required result.
- No secret, production deployment, product behavior, or package-manager
  conversion is introduced.
- Jules may create a reviewable PR only under separate authorization and may
  never mark Ready or merge it.
