# F6 frontend quality hardening — Draft PR handoff

## State

- Task: `task-frontend-quality-hardening` (F6).
- Branch: `task-frontend-quality-hardening`.
- Worktree: `/Users/daniilnovikov/.codex/worktrees/frontend-quality-hardening/AndrewWorkWebSite`.
- Base: `origin/main` at `1372bbd2e1c50e0dc549e4be8c42909e7359e714`.
- Draft PR: [#67](https://github.com/devDaniilNovikov/AndrewWebSite/pull/67), `test(frontend): harden preview quality gates`.
- PR #30 remained open Draft and unchanged.
- Production deployment, Timeweb/CD, branch deletion, Ready transition, merge, F3, and F4 remain unauthorized.

## Implemented

- Added exact `lighthouse@13.4.1` and `chrome-launcher@1.2.1`.
- Added `test:lighthouse` and wired it into `pnpm run verify`.
- Lighthouse runner builds and serves `frontend/out/`, launches Playwright Chromium through `chrome-launcher`, performs three cold mobile audits, excludes SEO because the preview is intentionally `noindex`, saves reports only under ignored `frontend/test-results/lighthouse`, and requires median scores of at least 90 for Performance, Accessibility, and Best Practices.
- Replaced the Motion reveal runtime with a CSS plus minimal `IntersectionObserver` path so hero/LCP content is visible before client JavaScript.
- Removed the hero media reveal wrapper.
- Bundled only upright local Inter Variable through `frontend/app/InterVariable-cyrillic.woff2`, a 56KB Latin/Cyrillic subset derived from the exact `inter-ui@4.1.1` source font. The full source font remains in `node_modules`; no remote font request is used.
- Added behavior-only visual-contract E2E coverage at 390×844, 768×1024, and 1440×900 for section order, responsive equipment grid, overflow, anchors, drawer, skip-link, reduced motion, metadata, 404, hosted-preview form restrictions, placeholders, and absence of real photos.
- Added a repository visual contract checker that forbids screenshot baselines, browser screenshot capture, and published image/media files outside `/public/media/verified/`.
- Preserved the placeholder media path and the existing `MediaSlot`/verified-photo contract for future user-provided licensed photos.

## Verification

- RED was reproduced for missing Lighthouse/visual gate contracts and for real Lighthouse Performance below the F6 threshold before the font/reveal hardening.
- `pnpm run verify` passed:
  - pinned Node runtime `24.14.0`;
  - format check;
  - OpenAPI type drift check;
  - ESLint;
  - strict typecheck;
  - Vitest coverage: statements 93.37%, branches 90.76%, functions 93.23%, lines 93.66%;
  - static boundary;
  - visual contract;
  - production gate refusal with the canonical blocker IDs;
  - deterministic static export;
  - Lighthouse medians: Performance 98, Accessibility 100, Best Practices 100;
  - Playwright E2E: 22 passed, 20 expected skips;
  - `pnpm audit --audit-level high`: no known vulnerabilities.
- `./mvnw -B verify` passed 660 tests and all configured coverage checks.
- Checksum-verified actionlint 1.7.12 for Darwin arm64 passed `.github/workflows/ci.yml`.
- Changed-diff secret/PII scan returned no findings.
- `git diff --check` passed.

## Reviews

- Specification review: F6 requirements are represented by explicit package scripts, unit tests, visual-contract E2E tests, Lighthouse runner assertions, static boundary rules, and production gate refusal.
- Code/correctness review: no actionable correctness finding remained after stabilizing the Lighthouse Chrome cleanup and expanding drawer keyboard/control/anchor assertions.
- Security/privacy review: no real photos were added, no screenshot baselines were added, no external runtime font/media requests were introduced, no backend/API/CI/deployment path changed, and the lead form remains disabled for hosted preview.

## Next

1. Wait for exact-head GitHub checks on PR #67.
2. Do not mark PR #67 Ready or merge without a separate explicit user authorization.
3. After an authorized F6 merge, pause before F3/F4 and request confirmed product/legal/content/photo materials as recorded in the plan.
