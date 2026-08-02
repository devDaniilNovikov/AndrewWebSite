# task-frontend-product-pages Draft handoff

Signature: HND task-frontend-product-pages [draft_pr] topics: frontend, product, accessibility, testing, security, tracker → predecessor: `2026-08-02-123351-task-frontend-product-pages-handoff.md`

## Durable — safe to cite later

- F3 keeps `/` as the approved long scrolling landing page and adds exactly
  seven explicit static product routes: `/uslugi`,
  `/remont-torgovogo-holodilnogo-oborudovaniya`,
  `/remont-ledogeneratorov`, `/o-kompanii`, `/raboty`, `/tseny`, and
  `/kontakty`. A typed content registry, shared preview frame and renderer,
  static route metadata, internal navigation, local `#contact`, and the
  existing lead form are reused across them.
- Prices, contacts, service region, warranties, cases, reviews, staff facts,
  company facts, legal copy, and photographs remain visibly provisional.
  Every media position uses `MediaSlot`; the external reference JPEG was not
  copied, cropped, extracted, or committed, and no stock, found, generated,
  or otherwise unverified image was introduced.
- The production-readiness manifest, package manifest, lockfile, OpenAPI,
  backend, workflows, branch protection, deployment configuration, and PR #30
  are unchanged. `noindex, nofollow` remains active and production export
  remains fail-closed on all canonical missing blocker IDs.
- Draft [PR #68](https://github.com/devDaniilNovikov/AndrewWebSite/pull/68)
  is the only F3 PR. Ready, merge, F4, JAR integration, branch deletion,
  production mutation, and deployment require separate authorization and are
  outside this checkpoint.

## Verified snapshot at 2026-08-02T13:32:29Z — re-verify live before use

- `origin/main` remained
  `6511017a03453e6d0f4a6f3522559f9647332d3e`. Startup reconciliation is
  commit `85d23d2`; the tested implementation is commit `54a8bcc`. PR #68 was
  open Draft, mergeable, based on exact `6511017`, and pointed to exact tested
  head `54a8bcc` before this metadata commit. GitHub reported zero repository
  deployments and zero deployments for that implementation head.
- RED was reproduced for the missing product registry and the seven
  unapproved static-boundary route files. GREEN covers the exact route set,
  unique metadata, internal links, explicit placeholder policy, shared
  renderer, accessibility, custom 404, deterministic HTML export, and
  product-route `sourcePath` derived only from `window.location.pathname`.
- The final pinned Node 24.14.0 / pnpm 11.18.0 `pnpm run verify` passed: 148
  Vitest tests; 93.57% statements, 91.02% branches, 93.47% functions, and
  93.86% lines; format, ESLint, strict typecheck, OpenAPI drift, static and
  media boundaries; required production-gate refusal; deterministic export
  SHA-256 `d0dffc97edddd33cb395bc7fda9a7e5df4f04ffc178660676b26a14afa484f32`;
  three cold Lighthouse runs with median Performance 98, Accessibility 100,
  and Best Practices 100; 27 Playwright passes across 390 by 844, 768 by 1024,
  and 1440 by 900 with 24 intentional project-specific skips; and no known
  high dependency vulnerability.
- `./mvnw -B verify` completed 660 tests with zero failures, errors, or skips;
  JaCoCo recorded zero missed lines and branches. A staged-file TruffleHog
  scan found zero verified or unverified secrets, and the staged media scan
  found no PNG, JPEG, WebP, GIF, SVG, or AVIF addition.
- Independent specification and code/correctness reviews passed without open
  findings. Security review identified missing product-route `sourcePath`
  coverage; the new `/kontakty?preview=1#contact` loopback test proves the
  payload contains only `/kontakty`, excludes query/fragment data, and leaves
  synthetic PII out of browser persistence. Security re-review passed.
- PR #30 remained open Draft at
  `00f55eea452884dc4c286ae17fcb6e1d4ebc25d8`, with its prior update timestamp
  unchanged. No production or deployment action was performed.

## Conditional continuation — re-verify live

1. Commit and push this Draft metadata, then wait for every required and
   security check on the new exact head without weakening or bypassing a gate.
2. Recheck PR #68 discussions, mergeability, exact head, PR #30, and zero
   deployments after CI. Resolve any new in-scope Critical or Important
   finding before stopping.
3. Keep PR #68 Draft. Ready and merge each require a separate current user
   authorization; F4, JAR integration, and deployment remain unauthorized.
