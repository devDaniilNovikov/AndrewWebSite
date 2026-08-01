# task-frontend-foundation handoff

Signature: HND task-frontend-foundation [in_progress] topics: frontend, security, testing → predecessor: `2026-08-01-111916-fix-frontend-track-preview-replacement-ready-handoff.md`

## Durable — safe to cite later

- F1 owns only the standalone static frontend foundation. The landing page,
  lead form, backend and OpenAPI integration, CI, deployment, and verified
  production content remain outside this task.
- The public export has one neutral Russian `/` preview marked as a
  demonstration with `noindex, nofollow`, plus a real custom `404.html`.
  There is no form, business claim, API route, middleware/proxy, Server
  Action, SSR, runtime image optimizer, or frontend network request.
- `build` delegates to the preview export. `build:production` deletes stale
  output and fails closed while any canonical production blocker remains
  `missing`; it reports blocker IDs only and leaves no `out/` directory.
- Node `24.14.0`, Corepack `0.34.5`, and pnpm `11.18.0` are exact-pinned;
  pnpm includes the Corepack integrity hash and a frozen lockfile. Inter
  Variable v4.1 is bundled locally with its OFL-1.1 notice.
- Next.js and `eslint-config-next` use exact `16.2.11`, not the planned
  `16.2.9`. The mandatory audit found seven HIGH and six MODERATE advisories
  at `16.2.9`; official fixes for the Next 16 line begin at `16.2.11`.
  PostCSS is overridden to `8.5.18`, and unused optional `sharp` is removed
  because image optimization is forbidden. This is the smallest secure
  deviation that leaves `pnpm audit --audit-level high` clean.
- The package scripts and `pnpm-lock.yaml` are the stable input contract for
  the later F1A CI task. Generated `node_modules/`, `.next/`, `out/`,
  coverage, and Playwright artifacts remain ignored.

## Snapshot at 2026-08-01T14:44:53Z — re-verify live before use

- Branch `task-frontend-foundation` remains in the dedicated worktree
  `/Users/daniilnovikov/.codex/worktrees/frontend-foundation/AndrewWorkWebSite`
  and is based directly on unchanged `origin/main`
  `2e51f44dd9227f3c0c008be27597fb19728b3fc8`.
- Startup reconciliation is commit `70124658f8a396408a9ec5586f1be33836ef0670`.
  The tested foundation is commit
  `a4a37b7f4086962dbb9e94a9de984e9a71032f88`. No push, Draft PR, Ready
  transition, merge, deployment, or PR #30 change occurred.
- TDD began with 4 expected failures and 2 passes: the draft shell exposed
  future content, the production validator lacked canonical IDs and stale
  output cleanup, and the static-boundary command was absent. The final
  Vitest run passed 11/11 tests with 100% statements, lines, and functions
  plus 95.55% branch coverage.
- A clean frozen install passed under the pinned toolchain. `pnpm verify`
  passed format, ESLint, clean-checkout `next typegen`, strict TypeScript,
  coverage, static-boundary and production-gate checks, two deterministic
  preview builds, Chromium E2E, and dependency audit.
- The two preview exports had the identical SHA-256 manifest digest
  `56d799f9c69539abe74e6713604778c7d3abeda7f13e94c9a2034de860966cc4`
  and contained `index.html`, `404.html`, and `_next/static`.
- Playwright passed 4/4 mobile and desktop cases: `/` returned 200, an
  unknown route returned 404 with the custom page, WCAG A/AA through 2.2 had
  no axe violations, keyboard focus was visible, no horizontal overflow was
  present, and every HTTP request remained on the loopback preview origin.
- `pnpm audit --audit-level high` reported no known vulnerabilities.
  TruffleHog found no changed-file secret, `git diff --check` passed, and
  independent specification, code, and security reviews returned PASS.
- The unchanged backend passed `./mvnw -B verify`: 660 tests, zero failures
  or errors, and all JaCoCo checks met.
- Live GitHub reconciliation still showed Draft PR #30 open, dirty, and
  untouched at head `00f55eea452884dc4c286ae17fcb6e1d4ebc25d8`.

## Next steps — each requires fresh state and separate authorization

1. Keep F1 `in_progress` and local until the user explicitly authorizes both
   push and creation of a Draft PR. Re-check `origin/main`, ownership, PR #30,
   the exact branch head, and all publication boundaries first.
2. After any publication authorization, rerun the exact-head frontend,
   backend, secret, diff, and review gates before requesting a later Ready
   transition. Ready, merge, deployment, and PR #30 closure each require
   their own current authorization.
3. Do not start F1A, F2, F5, or F6 from this branch. Their prerequisite and
   ownership rules still apply, and every successor needs a fresh worktree
   from the then-current `origin/main`.
