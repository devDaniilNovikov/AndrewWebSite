# Equipment card images handoff

## State

Codex controls `task-equipment-card-images` in the dedicated external worktree
`/Users/daniilnovikov/.codex/worktrees/equipment-card-images/AndrewWorkWebSite`,
based on fresh `origin/main` at `4dd990d`. The local media implementation is
complete, including the team's portrait. The user subsequently authorized a
commit and PR on 2026-09-16, requested merge into `main` after successful
checks, and confirmed rights for all ten photos plus consent of the pictured
people on 2026-09-20.

At this pre-publication handoff checkpoint, no commit, push, pull request,
Ready transition, merge, or deployment had been performed.

## Implemented scope

- Assigned six supplied photographs to every equipment card: “Холодильные
  камеры”, “Витрины и горки”, “Льдогенераторы”, “Морозильные лари”,
  “Холодильные системы”, and “Шкафы и столы”.
- Assigned the seventh supplied photograph to the completed-work card
  “Переход ледогенератора на воздушное охлаждение”. The user-confirmed problem
  is “Чрезмерное потребление воды и утечка фреона”; the result is
  “Ледогенератор стабильно работает и меньше потребляет воды”.
- Assigned the eighth supplied photograph to “Поиск утечки промышленного
  агрегата”. The user-confirmed problem is “Утечка фреона в централи
  холодильной системы”; the result records pressure testing, finding the leak
  in several places, and eliminating it.
- Assigned the ninth supplied photograph to “Реставрация испарителя
  холодильного шкафа/стола”. The user-confirmed problem is “Утечка фреона в
  системе”; the result records replacement of evaporator tubes and elbows, no
  remaining leak, and the user-supplied lifetime-warranty claim.
- Assigned the tenth supplied photograph to the “О компании” team-photo block
  under the user's 2026-09-18 instruction. Its portrait proportions are
  preserved in the existing verified-media component.
- Renamed the visible “Холодильные шкафы” card to “Холодильные камеры” under
  the user's explicit fifth-task clarification.
- Removed the duplicated “Типичные симптомы” and “Примеры работ” expandable
  sections from all six equipment cards, including the now-redundant “Частые
  неисправности” toggle. Compact request examples and the lead CTA remain.
- Converted both 1672×941 JPEG sources to 1200×676 WebP assets under
  `frontend/public/media/verified/` at 50,186 and 31,092 bytes.
- Preserved the four later 1672×941 JPEG sources byte-for-byte at 149,165,
  212,403, 256,811, and 155,978 bytes so no additional image-quality loss was
  introduced.
- Preserved all three 1448×1086 completed-work JPEGs byte-for-byte, so no
  additional image-quality loss was introduced.
- Converted the supplied 1087×1447 team portrait to a 307,382-byte WebP with
  metadata removed and the full image retained.
- Recorded the original SHA-256 provenance and descriptive alternative text.
- Rendered all ten assets through the verified local `MediaSlot`.
- Enforced full-width, intrinsic-height rendering with `object-fit: contain`
  so each photograph determines its frame height and remains uncropped without
  aspect-ratio distortion or visible side gutters.
- Avoided `next/image` inline styles because the static CSP rejects them; the
  native images are pre-sized, lazy-loaded, asynchronously decoded, and remain
  same-origin.
- Embedded all ten verified images as data URIs in the offline standalone HTML,
  including React hydration data. The standalone build now uses preview mode,
  so opening `file://` keeps lead submission disabled.

## Verification

- Focused interactive-section test: 6/6 passed on Node 24.14.0 and pnpm
  11.18.0.
- The new about-section team-photo test passed; the complete preview-shell
  file passed 12/13 tests, with its stale phone-placeholder assertion failing
  as it does on the unchanged base.
- ESLint: passed with zero warnings.
- Strict TypeScript check: passed.
- Visual media contract: passed.
- Static preview build/export: passed; 12 documents received hosted CSP headers.
- Deterministic static export: passed with the tenth photograph under the
  pinned Node 24.14.0 and pnpm 11.18.0 toolchain.
- Local browser verification confirmed the new refrigerator-cabinet image,
  title, problem, and result are present in their intended card, and the
  ice-maker image and supplied case copy remain intact. The evaporator image
  and all user-supplied copy are present in the left completed-work card.
- Desktop and mobile checks confirmed full-image presentation without cropping
  or aspect-ratio distortion, the three-column desktop row, and the one-column
  mobile layout.
- The team portrait loaded at 1087×1447 and displayed at 470×625 on desktop
  and 348×463 on mobile; `object-fit: contain` and no horizontal overflow were
  verified in the browser.
- Standalone-export unit tests passed 7/7; its `file://` Chromium verification
  confirmed all ten photos decode with no CSP violations or external requests.
- The responsive visual E2E contract passed in desktop, tablet, and mobile
  Chromium after routing the hosted E2E server to the preview build.
- The full E2E suite completed with 31 passed, 30 skipped, and 8 failures in
  existing contact-link/phone assertions; the media and standalone tests passed.

The full inherited test suite remains red on 15 pre-existing `origin/main`
failures in stale contact/production/static-boundary expectations; 199 tests
passed. The repository-wide format check also retains the pre-existing
`frontend/content/product-pages.ts` formatting failure. The dependency audit
reports existing advisories in Next.js 16.2.11 and transitive packages. None of
those unrelated baseline issues was changed in this media task.

## Next step

The focused and export gates are current and the final local rendering has been
reviewed. The 2026-09-20 user response confirmed rights and depicted-person
consent. The latest user instruction clarified that the commit should appear
in Timeweb's `main` picker, which requires a published PR and merge. The
repository's separate Ready and green-check gates remain in force, and a
GitHub merge does not by itself prove a Timeweb deployment.
