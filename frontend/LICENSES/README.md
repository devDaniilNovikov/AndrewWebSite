# Third-party font notice

The frontend bundles Inter Variable v4.1 locally from the exact
`inter-ui@4.1.1` package. `app/InterVariable-cyrillic.woff2` is an upright
Latin/Cyrillic subset derived from
`node_modules/inter-ui/variable/InterVariable.woff2`; it preserves the
variable optical-size and weight axes while avoiding unused glyph transfer.
The static export copies this one font into its own `_next/static` assets and
makes no remote font request.

The subset includes printable ASCII, common Russian Cyrillic, typographic
punctuation, the rouble sign, the numero sign, and directional arrows. It was
generated with FontTools 4.59.0 without modifying the font software name.

Inter is distributed under the SIL Open Font License 1.1. The complete
license and attribution are recorded in `Inter-OFL-1.1.txt`.

Headings use Manrope Variable from the exact
`@fontsource-variable/manrope@5.3.0` package. Its unicode-range declarations
let browsers fetch only the Cyrillic and Latin variable subsets needed by the
page, with `font-display: swap`; no remote font request is made. Manrope is
also distributed under the SIL Open Font License 1.1, included by the pinned
package and covered by the repository's OFL notice.
