# fix-mobile-navigation-scroll handoff

Signature: HND fix-mobile-navigation-scroll [in_progress] topics: frontend, accessibility, testing, security, tracker → predecessor: `none`

## Durable — safe to cite later

- The user reproduced a mobile drawer regression: from a product route, a
  navigation choice could leave the document at the previous scroll position
  or make only lower content reachable.
- The user explicitly authorized the isolated hotfix lifecycle through commit,
  Draft PR, exact-head CI/security review, Ready, and squash-merge.
- Production deployment, branch deletion, history rewriting, PR #30 changes,
  and weakening any release or production-readiness gate remain excluded.
- The fix keeps the approved single-page landing navigation contract: primary
  links target verified landing anchors while provisional product routes remain
  directly addressable.

## Verified snapshot at 2026-08-08T06:06:21Z — re-verify live before use

- Fresh `origin/main` and the clean branch base matched
  `25ce867e79348f91e3021cb10714402b7c1b66b7`, the squash merge of F3 PR #68.
- PR #30 remained open Draft at
  `00f55eea452884dc4c286ae17fcb6e1d4ebc25d8`, with its historical update
  timestamp unchanged; GitHub reported zero deployments.
- The implementation explicitly unlocks body scrolling before navigation,
  distinguishes same-page anchor scrolling from cross-route navigation, and
  avoids restoring focus to the drawer trigger during link navigation.
- Focused component and mobile Chromium regression checks passed. The full
  frontend verification passed 151 tests, coverage above 91%, deterministic
  export, production-gate refusal, Lighthouse medians 99/100/100, 27 applicable
  Playwright tests, and an audit with no high findings.
- `./mvnw -B verify` passed 660 tests. Two patched transitive tool versions are
  pinned because the inherited lockfile otherwise failed the required high
  severity audit gate.

## Conditional continuation — re-verify live

1. Complete independent specification/code/security reviews and resolve every
   Critical or Important finding.
2. Commit implementation and metadata atomically, push without force, and open
   one Draft PR from `fix-mobile-navigation-scroll`.
3. Record the Draft and Ready checkpoints, then require green exact-head
   protected and security checks after each metadata push.
4. Squash-merge only with exact-head matching and no bypass, auto-merge, branch
   deletion, deployment, or PR #30 mutation.
