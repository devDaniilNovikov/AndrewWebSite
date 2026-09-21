# Placeholder-first frontend preview contract

## Status and authority

This contract records the user's 2026-08-01 decision for the first frontend
delivery, their corrected visual-reference decision later that day, and the
2026-08-02 authorization to extend that composition into provisional product
pages. It is a preview slice of the broader product brief, not a promotion of
any unverified recommendation, assumption, or example into production content.

The canonical visual reference is the user's external Telegram attachment
`tg_image_2961269225.jpeg`, a 724 by 2172 image with SHA-256
`6ee924016b146c528c1f72118aab7f5a0260d15369af07e0da9892308b76ecbc`.
The image itself is intentionally not stored in the repository. Its visible
phone, prices, cases, reviews, hours, logo, staff identities, imagery, and
wording are design-only examples and are never business facts.

## Delivery slice

The user authorized legal document publication on 2026-09-09: `/privacy`
contains the supplied policy and `/personal-data` the separate consent,
both dated 2026-09-06. Preserve their body text, lists and tables. The footer
links to both documents and displays the user-confirmed INN. This content
delivery does not change the lead-processing or analytics implementation.

- Export `/` plus `/uslugi`,
  `/remont-torgovogo-holodilnogo-oborudovaniya`,
  `/remont-ledogeneratorov`, `/o-kompanii`, `/raboty`, `/tseny`, and
  `/kontakty`, plus a real static 404.
- Keep the home-page anchors `equipment`, `works`, `pricing`, `about`, and
  `contact`; shared navigation may link to the exact product routes while
  page-level request actions target the local `contact` section.
- Product pages are provisional structures. Verified trust-content population
  remains deferred to F4, and every missing production fact remains an
  explicit placeholder.
- Keep the backend and its OpenAPI contract unchanged. The existing F5 lead
  client derives `sourcePath` from the current browser pathname, remains usable
  only for explicitly configured loopback preview, and stays disabled on a
  hosted preview.

The home-page order is header, hero, benefit strip, equipment grid, services,
completed-work cards, repair CTA, pricing placeholders, request
process, team/company section, planned-maintenance section, review
placeholders, contact and form shell, then footer. The cookie banner and any
analytics UI visible in the reference are design artifacts and are not part of
the preview while the analytics vendor and legal text remain unresolved.

The user supplied and assigned six equipment photographs on 2026-09-13 and
2026-09-15:
`equipment-cold-rooms.jpg` belongs to “Холодильные камеры”,
`equipment-display-cases.webp` belongs to “Витрины и горки”,
`equipment-ice-makers.webp` belongs to “Льдогенераторы”,
`equipment-chest-freezers.jpg` belongs to “Морозильные лари”,
`equipment-refrigeration-systems.jpg` belongs to “Холодильные системы”, and
`equipment-cabinets-and-tables.jpg` belongs to “Шкафы и столы”. These local assets
replace all six home-page equipment media placeholders.

On 2026-09-16 the user supplied `work-ice-maker.jpg` for the completed-work card
“Переход ледогенератора на воздушное охлаждение” together with its problem and
result copy, and `work-refrigerated-cabinet.jpg` for “Кейс холодильного шкафа”.
The latter is titled “Поиск утечки промышленного агрегата”; its supplied
problem records a refrigerant leak in the central refrigeration system, and
its supplied result records pressure testing, discovery of leaks in several
places, and remediation. The user also supplied `work-retail-site.jpg` and the
copy for “Реставрация испарителя холодильного шкафа/стола”: a refrigerant leak
in the system was addressed by replacing evaporator tubes and elbows, with the
user-supplied result stating that the leak is gone and the work carries a
lifetime warranty. Missing media elsewhere remain placeholder-first.

On 2026-09-18 the user supplied `about-team-installation.webp` for the
“О компании” team's photograph placeholder. The portrait image is shown in
full without cropping; this placement does not independently verify the
subjects' identities. On 2026-09-20 the user confirmed rights to publish all
ten supplied photos and consent of people pictured in the team photograph.
The offline standalone HTML embeds the verified images as data URIs; hosted
preview pages serve the same local files with the normal static CSP.

On 2026-09-21 the user supplied `hero-commercial-refrigeration.webp` for the
home-page hero media block (“Сервисная модель”), the last remaining home-page
media placeholder. Unlike the other photographs it is rendered in `cover`
mode: it fills the hero block edge to edge and is cropped to the block's
proportions, with the “Сервисная модель” card kept on top of it. It loads
eagerly with high fetch priority because it is the largest above-the-fold
image. The home page now shows eleven verified photographs and no media
placeholders.

Product pages reuse the same demonstration banner, header, compact split hero,
light card grids, dark information bands, contact section, and footer. They do
not duplicate every home-page section or invent route-specific business facts
to fill space.

## Visual system

| Token           | Value                 |
| --------------- | --------------------- |
| `navy`          | `#0B1220`             |
| `surface`       | `#F6F8FB`             |
| `primary`       | `#176BFF`             |
| `accent`        | `#28B8D5`             |
| `content-width` | approximately `72rem` |

- Use a locally bundled Inter Variable font and no remote font request.
- Preserve the reference's compact corporate rhythm, thin dividers, small
  radii, light split hero, dark CTA/price bands, blue calls to action, and
  light gray media placeholders.
- Equipment cards show the assigned photo, title, short description, compact
  request examples, and one request CTA. They do not repeat symptom or work
  lists in an expandable panel.
- Match composition, palette, rhythm, and information density rather than
  attempting a pixel diff against the 724 by 2172 reference export.
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
