# fix-frontend-tz-preview Draft publication handoff

Signature: HND fix-frontend-tz-preview [draft] topics: frontend, design, accessibility, privacy, testing, standalone-export → predecessor: none

## Durable — safe to cite later

- The user supplied `TZ-ISPRAVLENIE-FRONTA.md`, authorized the frontend design
  corrections, and on 2026-08-16 explicitly authorized commit, PR publication,
  Ready transition, required CI, and squash merge.
- The task keeps the existing preview and production-safety boundaries: missing
  business facts remain placeholders, local `file://` form submission remains
  disabled, lead PII is not persisted or emitted to analytics, and production
  deployment is not part of this authorization.
- In addition to the responsive design, navigation, CTA, form, cookie, and
  analytics corrections, the branch contains a deterministic single-file HTML
  builder and verifier. The generated delivery file is intentionally ignored;
  source, tests, and the reproducible build path are committed.

## Verified snapshot at 2026-08-16T07:41:43Z — re-verify live before use

- The clean task branch head was
  `2c6a0b1812b79ce6983a46665a880b0e045c6248`, based directly on current
  `origin/main` `172d20f3fbf8709089789f0c0b8b791e35637f4d`; no task PR or remote task
  branch existed.
- PR #69 was live-verified merged as `172d20f3fbf8709089789f0c0b8b791e35637f4d`,
  and its source branch remained retained. PR #30 remained open Draft and was
  outside this task.
- Local frontend evidence on the implementation head: format, lint, strict
  typecheck, 200/200 Vitest tests with 92.24% coverage, deterministic export,
  Lighthouse medians performance 96 / accessibility 100 / best-practices 100,
  Playwright rerun 30 passed with 30 intentional project skips, standalone
  `file://` verification, and dependency audit with no known vulnerabilities.
- Independent correctness and security reviews found no Critical, High, or
  Medium findings. Path containment, case-insensitive HTML-context escaping,
  strict CSP, and pre-network Playwright request blocking were included before
  the final standalone artifact was regenerated and verified.

## Conditional continuation — re-verify live

1. Commit and push this Draft checkpoint without force, then open one Draft PR
   from `fix-frontend-tz-preview` to `main` with the verified scope and evidence.
2. Update the tracker with the canonical PR URL, push the exact metadata head,
   and wait for Repository policy, `verify`, dependency-security, Frontend
   quality, and applicable security checks.
3. Mark Ready only while the branch remains current, mergeable, green, and
   conversation-free; re-check the exact Ready head after the Ready event.
4. Squash-merge without admin bypass, auto-merge, deployment, force push,
   branch deletion, or any change to Draft PR #30; then verify `origin/main`
   and retained source-branch state.
