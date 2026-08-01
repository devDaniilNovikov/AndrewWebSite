# Single-page frontend preview contract

## Status and authority

This contract records the user's 2026-08-01 decision for the first frontend
delivery. It is a preview slice of the broader product brief, not a promotion
of any unverified recommendation, assumption, or example into production
content.

The canonical visual reference is
[`reference/landing-ui-2026-08-01.jpg`](reference/landing-ui-2026-08-01.jpg),
SHA-256
`d79cae4ea8647b6a4f81975debd3ce96b229b468b6f9a37f1a4a6184d1e8af35`.
The user authorized preserving this supplied image in the repository. Its
visible phone, prices, cases, review, hours, logo, and wording are design-only
examples and are never business facts.

## Delivery slice

- Export exactly one public product route, `/`, plus a real static 404.
- Use in-page navigation anchors: `equipment`, `works`, `pricing`, `about`,
  and `contact`.
- Defer the product brief's multi-page routes and verified trust-content
  population until separately authorized follow-up tasks.
- Keep the backend and its OpenAPI contract unchanged. The lead form calls
  the browser-facing API client defined by the canonical OpenAPI source.

The page order is header, hero, benefit strip, equipment grid, completed-work
placeholders, pricing placeholders, company section, review placeholder,
contact and lead form, then footer. The dark popover visible over the reference
pricing section is a screenshot artifact and is not part of the product.

## Visual system

| Token | Value |
| --- | --- |
| `navy` | `#0B1220` |
| `surface` | `#F6F8FB` |
| `primary` | `#176BFF` |
| `accent` | `#28B8D5` |
| `content-width` | approximately `72rem` |

- Use a locally bundled Inter Variable font and no remote font request.
- Preserve the reference's compact corporate rhythm, thin dividers, small
  radii, dark hero/price bands, blue calls to action, and light gray media
  placeholders.
- Match composition, palette, rhythm, and information density rather than
  attempting a pixel diff against the 439 by 1280 reference export.
- Do not add GSAP, 3D, glassmorphism, parallax, scroll hijacking, or continuous
  expensive animation.
- Restrained reveals move no more than 16 px and complete in roughly 420 ms;
  micro-interactions complete in roughly 180 ms. `prefers-reduced-motion`
  disables all non-essential reveal and smooth-scroll behavior.

## Responsive and accessibility behavior

- Use Tailwind's mobile-first default breakpoints; equipment grids collapse
  from three to two to one column, while cases, pricing, and contact layouts
  stack without horizontal scrolling.
- The mobile header exposes a visible request CTA and an accessible drawer.
  The drawer manages focus, reports `aria-expanded`, closes with Escape and
  its overlay, restores focus, and locks only background scrolling.
- Do not add a persistent bottom bar that covers content.
- Use semantic landmarks, one `h1`, ordered section headings, visible labels,
  visible focus, keyboard-complete controls, and live regions for form status.
- Placeholder media is explicitly labelled as non-production content and is
  hidden from assistive technology when purely decorative.

## Preview and production boundary

Preview output is visibly marked as a demonstration and uses
`noindex, nofollow`. Missing logo, phone, media, prices, cases, review, legal
text, and other facts remain explicit placeholders; the frontend never
invents them.

Preview API submission is enabled only when both the page and configured API
origin are loopback HTTP origins and the backend has an explicit local CORS
configuration. A hosted preview cannot collect leads.
Production uses only the relative same-origin path `/api/leads` and fails its
content-validation build before export until the real company name, phone,
consent text, personal-data policy, prices, warranty terms, cases, testimonial,
and licensed photographs are all verified and supplied.

Lead PII never enters a URL, analytics, browser persistence, logs, fixtures,
or snapshots. The frontend never calls Telegram and contains no Telegram,
database, HMAC, or OTLP credential.
