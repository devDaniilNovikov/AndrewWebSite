# F1 — Frontend preview foundation

## Objective

Create the production frontend workspace and a tested static-export foundation
for the approved single-page preview without adding business content.

## Ownership and prerequisites

- **Owner:** Codex.
- **Prerequisite:** `fix-frontend-track-preview-replacement` merged; start from
  fresh `origin/main` in a dedicated `task-frontend-foundation` worktree.
- **Owned paths:** `frontend/**` and the task-specific handoff.
- **Sources:** [SPEC](../docs/SPEC.md), [preview design](../docs/frontend/landing-preview.md),
  [architecture](../docs/backend/architecture.md), and
  [Git Flow](../.agents/workflows/GIT_FLOW.md).

## Tasks

1. Scaffold the architecture-approved Next.js 16.2.9, React 19.2.x, strict
   TypeScript, Tailwind CSS 4, Motion, and Node 24 stack under `frontend/`;
   select and pin one pnpm release and exact lockfile.
2. Configure `output: 'export'` to `out/`, a real static 404, preview and
   production build modes, production-content validation, local Inter
   Variable, design tokens, and the approved ESLint, Prettier, Vitest,
   Testing Library, axe, and Playwright toolchain.
3. Prove frozen install, format, lint, typecheck, smoke tests, and deterministic
   preview export; prove production export fails safely while required content
   remains missing.

## Acceptance

- No API routes, middleware, Server Actions, SSR, runtime proxy, redirects,
  rewrites, headers, server image optimizer, backend, CI, analytics, or
  production behavior is introduced.
- `frontend/out/` is generated and ignored, never committed.
- Dependency audit, secret scan, whole-diff review, required existing CI, and
  Codex review pass before Ready.
- Push, Draft PR, Ready, and merge remain separately authorized stages.
