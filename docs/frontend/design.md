# Frontend design contract

This document is the canonical home for frontend visual foundations, motion,
responsive breakpoints, and static-export image handling. Performance budgets
live in the [project specification](../SPEC.md#frontend-budgets) and are not
restated here.

## Approved foundations

The following values and wording are restored from the historical frontend
section at `c703ae7:.agents/CLAUDE.md`:

- hero background: `#0B1220`
- site background: `#F6F8FB`
- primary action: `#176BFF`
- accent: `#28B8D5`
- local Inter Variable font
- expressive but lightweight animations
- micro-interactions around 160–220 ms
- reveal animations around 420–560 ms
- no heavy 3D, animation noise, or aggressive parallax
- mandatory `prefers-reduced-motion` support
- visible keyboard focus and complete keyboard navigation

The experience is mobile-first, trustworthy, clear, and conversion-focused.
The dark hero introduces the service; the rest of the site is light and easy
to scan.

## Static-export image strategy

- A build-time Sharp pipeline generates AVIF and WebP content-photo variants.
- Generate a manifest with intrinsic dimensions and variant paths, then emit
  responsive `srcset` and `sizes` from that manifest.
- Content photographs must not use the `unoptimized` escape hatch. A content
  photo without generated variants fails the production export gate.
- Keep the pipeline deterministic and compatible with the architecture's
  static export; it must not require an image optimizer at runtime.

## Proposed details

Every item in this section is **[предложено, требует утверждения]** and is not
an approved implementation contract until the user explicitly approves it.

### Motion behavior

- Use `cubic-bezier(0.2, 0, 0, 1)` for micro-interactions and reveal motion.
- Under `prefers-reduced-motion: reduce`, disable non-essential transforms,
  reveals, and smooth scrolling. State changes remain immediate, perceivable,
  keyboard-complete, and screen-reader coherent.

### Responsive breakpoints

Use the Tailwind CSS mobile-first defaults so the frontend does not add a
custom breakpoint system:

| Token | Minimum width |
| --- | --- |
| `sm` | `40rem` |
| `md` | `48rem` |
| `lg` | `64rem` |
| `xl` | `80rem` |
| `2xl` | `96rem` |

### Image generation grid

- Generate content-photo variants at `480`, `768`, `1024`, `1440`, and `1920`
  px.
- Never upscale an asset beyond its source width.
