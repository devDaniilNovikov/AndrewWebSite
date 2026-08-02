# task-frontend-lead-form Draft PR handoff

Signature: HND task-frontend-lead-form [draft_pr] topics: frontend, api, accessibility, privacy, testing, security, tracker → predecessor: `2026-08-02-041333-task-frontend-lead-form-handoff.md`

## Durable — safe to cite later

- F5 implementation commit `c5fd99dff36eafca41714e4dd769cf8959522700`
  generates immutable TypeScript types from the canonical backend OpenAPI
  document and makes drift checking part of the frontend `verify` contract.
- The contact shell is now an accessible client form with generated request
  types, OpenAPI-aligned validation, a honeypot-only synthetic path, immutable
  payload snapshots, UUIDv4 idempotency, explicit retry/cooldown behavior, and
  fixed non-PII status messages.
- The only runtime `fetch` remains in `frontend/lib/leads/transport.ts`.
  Preview submission requires a bare HTTP loopback API origin and a loopback
  page; hosted previews remain inert. Production uses only relative
  `/api/leads` after the separate readiness gate succeeds.
- Draft [PR #64](https://github.com/devDaniilNovikov/AndrewWebSite/pull/64)
  is the only F5 pull request. Ready, squash-merge, F5A dispatch, deployment,
  branch deletion, and changes to PR #30 remain outside current authorization.
- No photograph, reference image, screenshot baseline, or generated media was
  added. Existing landing media areas remain placeholders for later
  provenance-verified local photographs.

## Verified snapshot at 2026-08-02T05:05:34Z — re-verify live before use

- `origin/main` remained
  `6477af195c87598e0283cd53a0b09135b8b20c21`; no pre-existing F5 PR or remote
  branch existed before publication. PR #30 remained open Draft at
  `00f55eea452884dc4c286ae17fcb6e1d4ebc25d8`.
- Frozen pnpm installation used Node 24.14.0, Corepack 0.34.5, and pnpm 11.18.0.
  `pnpm run verify` passed the exact-runtime guard, format, OpenAPI drift,
  ESLint, strict TypeScript, 118 unit/component/axe tests, all coverage gates,
  static-boundary and production-gate checks, two deterministic exports, 19
  applicable Playwright scenarios with 20 intentional viewport skips, and a
  high-level dependency audit with no known vulnerabilities.
- Coverage was 95.29% statements, 91.84% branches, 96.42% functions, and
  95.34% lines. The final deterministic export manifest SHA-256 was
  `cf31107c94db94ac33cdd0e306c52550672b1a6b51d408c160bc88ef6502ba6a`.
- Browser coverage included exact payload and source path, 202 PII clearing,
  same-UUID 503 retry, edit and 409 UUID invalidation, 429 cooldown, 15-second
  timeout, hosted-preview zero-request behavior, custom 404, and absence of
  browser persistence and third-party requests.
- `./mvnw -B verify` passed 660 tests with zero failures or errors and all
  JaCoCo gates. TruffleHog 3.96.0 reported zero verified and zero unknown
  secrets in the staged diff; no generated artifacts or image files are
  tracked.
- Independent code review findings for pinned runtime enforcement and 409
  integration coverage were fixed. Security/privacy review found no P1/P2
  findings. The specification recommendation for a bespoke E2E server was not
  adopted because the existing static server demonstrably returns 200 for
  `/`, the real `out/404.html` with status 404 for unknown paths, and the form
  suite now explicitly asserts the baked local-preview mode before submission.

## Conditional continuation — re-verify live

1. Push this metadata commit and wait for every required check on the exact
   Draft PR #64 head; investigate failures without weakening any gate.
2. Keep F5 at `draft_pr`. A separate current user decision is required before
   Ready, squash-merge, branch deletion, deployment, or any PR #30 change.
3. Only after an authorized F5 merge may a fresh controller task create the
   sanitized F5A Issue and use the guarded single `jules-action` route.
