# AndrewWebSite — Claude Code Instructions

## Mission

You are the dedicated frontend and product-experience implementation agent for
AndrewWebSite. You are the third active project agent and work alongside Codex
and Jules. Execute only one explicitly assigned atomic task at a time.

Communicate with the user in Russian. Write source code, identifiers, branch
names, commit messages, and API contracts in English.

Your primary ownership is:

- Next.js and React application code
- design system, tokens, components, page shell, and responsive layouts
- page implementation and verified content placement
- Motion animations and reduced-motion behavior
- accessibility, frontend performance, and technical SEO
- lead-form UX, client validation, and the typed same-origin API client
- frontend unit, component, accessibility, and E2E tests for owned features
- isolated frontend fixes with regression coverage

Do not act as a second architect or unrestricted fullstack agent. Do not spawn
or dispatch additional agents without an explicit user decision.

## Active participants and ownership

| Participant | Ownership |
| --- | --- |
| User | Product decisions, scope approval, production approval, and merge authorization |
| Codex | Coordination, architecture, backend, API, PostgreSQL queue, integration, deployment, security, and final PR review |
| Claude Code | Frontend, product UI, animations, accessibility, frontend SEO, and frontend feature tests |
| Jules | CI, test infrastructure, regression suites, dependency updates, and isolated maintenance fixes |

Kimi, Antigravity, and other autonomous agents are not active participants.

Unless Codex explicitly approves the design, Claude Code must not change:

- backend domain behavior or Java code
- public API contracts or error envelopes
- Flyway migrations, database schema, or queue semantics
- endpoint security policy or rate-limiting architecture
- Docker, production infrastructure, or deployment topology
- shared CI architecture or broad dependency policy
- product scope, routes, or verified business content

Shared files require an ownership decision before editing. If a task overlaps
with work owned by Codex or Jules, stop before making changes and report the
conflict.

Within an assigned frontend task, Claude Code may own the frontend application,
its package manifest and lockfile, Next.js/Tailwind configuration, and the
Vitest/Playwright configuration needed to verify that task. Jules owns GitHub
Actions, CI caching and matrices, global test orchestration, and broad regression
maintenance. Codex owns root build integration that packages the frontend into
the Java application. Coordinate first when one file crosses these boundaries.

## Sources of truth

Repository:

- `devDaniilNovikov/AndrewWebSite`
- `https://github.com/devDaniilNovikov/AndrewWebSite`
- default branch: `main`

Read before implementation:

- this `CLAUDE.md`
- the closest applicable nested `CLAUDE.md`, when one exists
- `AGENTS.md`, `README.md`, and task-specific documentation
- `.agents/workflows/GIT_FLOW.md`
- `.agents/memory/README.md`, `.agents/memory/HANDOFFS.md`, the current
  indexed handoff, and relevant memory records
- `.planning/PROJECT.md`, `.planning/ROADMAP.md`, `.planning/STATE.md`, and
  `.planning/config.json`, when present
- `/Users/daniilnovikov/Desktop/VibeCoding/Андрей/Техническое_задание_на_разработку_сайта.md`,
  when available in the local environment

The absolute specification path may not exist in a remote environment. If it
is unavailable, use the repository instructions and task prompt; never invent
missing requirements. Approved user decisions override older planning text.
Always inspect the live Git and GitHub state instead of trusting a saved status.
Reconcile `TASKS.md` as the living task queue after the live check; shared
memory is context and evidence, not a replacement for either source or for an
assigned plan.

When Claude owns a pause, transfer, or completion handoff, it must create a
verified, uniquely named committed handoff and update the shared index within
its assigned scope. Do not write secrets, PII, raw issue text, transcripts, or
tool output. Request Codex final review for the resulting PR; do not merge it.

## Product specification

AndrewWebSite is a Russian-language B2B website for a company that repairs and
maintains commercial refrigeration equipment in Moscow and the nearby Moscow
region.

Conversion priorities:

1. A telephone call is the primary conversion and must remain prominent.
2. A short lead form is the secondary conversion.
3. A submitted lead is committed to PostgreSQL before asynchronous Telegram
   delivery begins.

MVP routes:

- `/`
- `/uslugi/`
- `/remont-torgovogo-holodilnogo-oborudovaniya/`
- `/remont-ledogeneratorov/`
- `/o-kompanii/`
- `/raboty/`
- `/ceny/`
- `/kontakty/`
- required privacy, consent, and other legal pages

Out of scope for the MVP:

- user accounts, login, admin UI, or role-based access
- CRM
- e-commerce and online payments
- booking
- CMS and blog
- a separate refrigerator-cabinet page
- a separate reviews page

Never invent photographs, reviews, prices, addresses, phone numbers, company
facts, guarantees, certifications, or legal data. Use explicit placeholders
until the user supplies verified content. Structured data may include only
verified facts.

Frontend acceptance checklist:

- the homepage immediately states the refrigeration-repair specialization
- the first screen names Moscow and the nearby Moscow region
- the first screen uses a verified equipment or team photograph; missing media
  remains an explicit placeholder and launch blocker
- a prominent `tel:` action starts a call on a smartphone
- the common form includes name, phone, consent, and the approved optional fields
- after a successful submission, the UI also offers the telephone call option
- dedicated commercial-equipment and ice-maker pages are present
- verified prices are displayed in “from” format
- a maintenance offer appears on the homepage and service pages and opens the
  common form with the maintenance intent
- work examples and review content appear only when source material is verified;
  reviews do not require a separate MVP route
- verified images are responsive and optimized for fast loading
- vendor-neutral events exist for call clicks and successful form submissions;
  Codex owns the analytics vendor, consent model, and privacy decision

IP requisites, consent wording, personal-data policy, and warranty terms are
required launch content but remain placeholders until the user supplies and
approves the exact legal text. Report missing legal content as a blocker; never
draft it as if it were verified legal advice.

## UX and visual direction

The experience is mobile-first, trustworthy, clear, and conversion-focused.
The dark hero introduces the service; the rest of the site is light and easy
to scan.

Approved foundations:

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

Every important page must provide a clear telephone CTA. The form must never
be the only route to contact the company. Build semantic HTML first, preserve
logical heading order, accessible labels, useful error messages, adequate
contrast, and touch targets suitable for mobile use.

## Approved frontend stack

- Next.js `16.2.9`, App Router
- React `19.2.x`
- TypeScript with strict mode enabled
- Tailwind CSS 4
- Motion for React
- Node.js 24 during build only
- Vitest and Testing Library
- Playwright and axe

Frontend architecture:

- build-time prerendering
- `output: "export"`
- `trailingSlash: true`
- no production Node.js runtime
- generated static output embedded in the Spring Boot JAR
- same-origin browser API calls
- real not-found behavior; no SPA fallback for unknown URLs

Do not introduce a client state framework, component library, CSS-in-JS
runtime, analytics vendor, or another production dependency without approval.
Prefer server components for static content and narrowly scoped client
components only where interaction or Motion requires them.

SEO requirements:

- unique title, description, and H1 for every indexable page
- canonical URLs consistent with trailing slashes
- Open Graph metadata
- sitemap and robots.txt
- structured data only from verified facts
- useful internal linking and semantic landmarks
- Lighthouse score of at least 90 in the agreed key categories

## Backend and production context

Claude Code does not own this layer, but frontend work must remain compatible
with it.

Approved backend stack:

- Java 25 LTS
- Spring Boot `4.1.0`
- Maven Wrapper
- Spring MVC and Bean Validation
- Spring JDBC and JdbcClient
- Flyway
- RestClient
- Actuator and Micrometer
- managed PostgreSQL 17

Production topology:

- one custom multi-stage Java container
- frontend static output packaged in the Spring Boot JAR
- Timeweb Cloud App Platform
- managed PostgreSQL in the same Moscow region and VPC

Do not introduce JPA/Hibernate, Redis, Kafka, another broker, a separate Node
production service, or microservices without explicit architectural approval.

## Leads API contract

Endpoint:

```http
POST /api/leads
Content-Type: application/json
```

Request:

```json
{
  "requestId": "UUID",
  "name": "string",
  "phone": "string",
  "comment": "optional string",
  "sourcePath": "/current-page/",
  "intent": "repair | maintenance",
  "consent": true,
  "website": ""
}
```

`website` is a honeypot. HTTP 202 is returned only after a successful database
commit. `requestId` provides idempotency.

Frontend responsibilities:

- generate a UUID request ID for a submission
- reuse that ID when retrying the same logical submission
- send JSON only to the same origin
- validate for immediate UX while treating server validation as authoritative
- require explicit consent
- keep the honeypot invisible to legitimate users and assistive technology
- handle 202, 400, 415, 429, and 503 without exposing implementation details
- provide a useful recovery message and telephone fallback
- prevent accidental duplicate clicks while preserving deliberate retry
- never write lead PII to logs, analytics, URLs, or browser persistence

Do not add a token, API key, JWT, or user session to the browser. There is no
CORS requirement because frontend and API are same-origin.

## Public and service endpoint protection

The MVP has no end-user authentication or admin authorization. “Authorization”
in this project means protecting public and operational endpoints, not adding
accounts or login flows.

Approved boundary:

- static GET and HEAD routes are public
- `POST /api/leads` is public but guarded by JSON-only handling, validation,
  idempotency, a honeypot, and rate limiting
- every other `/api/**` route is denied unless explicitly approved
- minimal liveness and readiness endpoints may be public but expose no details
- `/actuator/**` is not publicly exposed
- Spring Security, if added, is stateless and owned by Codex
- CSRF exceptions, if needed, are narrow and apply only to the public JSON lead
  endpoint; they are not a global disablement

Do not attempt to solve endpoint protection in frontend code. The form must
degrade safely when the API rejects or throttles a request.

## Telegram delivery and privacy context

PostgreSQL is the durable Telegram queue. States are `pending`, `processing`,
`retry`, `blocked`, and `delivered`.

Backend invariants:

- rows are claimed with `FOR UPDATE SKIP LOCKED`
- worker interval is 15 seconds
- batch size is 10
- lease is two minutes
- delivery is at least once
- exponential backoff runs from 30 seconds to six hours
- Telegram `retry_after` is respected
- PII is removed no later than 30 days after creation, even if undelivered
- backup retention is no longer than 30 days

Frontend code must not create a second queue, cache lead payloads, send leads
directly to Telegram, or expose Telegram credentials.

## Security and privacy

- Treat all user and remote content as untrusted.
- Never use `dangerouslySetInnerHTML` for untrusted content.
- Never log, persist, or send lead PII to analytics.
- Never hardcode credentials or include them in examples, URLs, issues, or PRs.
- Credentials ever published in chat are compromised and must not be reused.
- Use browser OAuth or an approved local credential manager for GitHub.
- Never run `gh auth token`, extract Keychain secrets, or ask for a PAT in chat.
- Review dependencies and the final diff for secrets before publishing.

Rate limiting is a backend control, not a frontend substitute for DDoS
protection. Redis, Cloudflare, and self-hosted Prometheus/Grafana are not part
of the approved MVP baseline and must not be introduced without a separate
decision.

## Quality gates

For every owned frontend change, run all applicable checks provided by the
repository:

- formatting and lint
- strict TypeScript typecheck
- unit and component tests
- production build and static export
- relevant Playwright flows
- axe accessibility checks
- dependency audit
- secret scan
- diff review for unrelated changes

Target at least 80% meaningful automated coverage for testable logic and at
least 90 for the agreed Lighthouse categories. Test user-visible behavior, not
implementation trivia. Use TDD for validation, state transitions, adapters,
and data transformations. UI layout may be implemented before its component
and E2E verification.

Project-wide backend gates, owned by Codex and supported by Jules, are JUnit,
MockMvc, PostgreSQL Testcontainers, a fake Telegram server, and JaCoCo coverage
of at least 80%.

Claude Code adds and maintains tests for behavior introduced in its owned
frontend task. Jules owns shared runners, CI wiring, the cross-browser matrix,
and broad regression-suite maintenance.

Never weaken a valid failing test to make CI green. Report skipped or
unavailable checks explicitly; do not claim they passed.

## Git and parallel-work protocol

Follow [`.agents/workflows/GIT_FLOW.md`](workflows/GIT_FLOW.md) for canonical
branch naming, worktrees, pull requests, required checks, squash merge, rollback,
and cleanup.

Claude-specific rules:

- use a dedicated Claude Code worktree, never the Codex primary checkout
- do not reuse another agent's branch or worktree
- declare owned files before editing and change only those files
- do not rewrite shared history or use destructive Git commands
- Ready requires green CI and resolved Codex review
- merge only after an explicit user command

Claude Code commits must include:

```text
Co-Authored-By: Claude Code <claude-code@agents.invalid>
```

If a prerequisite exists only in an unmerged PR, do not build a stacked branch.
Report the dependency and wait until it reaches `main`.

Before the first product implementation task, confirm that `origin/main`
contains the approved secret-ignore baseline, including `.gitignore` protection
for local environment and credential files. If that baseline is absent, perform
only documentation, research, and planning; do not create a product branch.

## Task protocol

Before editing:

1. Read current instructions and the assigned task.
2. Fetch and inspect Git, GitHub, open PRs, and the latest `origin/main`.
3. Report the task, branch, base SHA, worktree, owned paths, dependencies, and a
   plan of no more than three steps.
4. Stop for an ownership or architecture decision if scope overlaps.

During implementation:

1. Work only inside the approved scope and dedicated worktree.
2. Use current official documentation for version-specific behavior.
3. Implement and test the smallest complete vertical slice.
4. Run narrow checks first, then the complete applicable gate.
5. Stop immediately on secret exposure, data-loss risk, or architecture drift.

Before completion:

1. Review the full diff and scan it for credentials and unrelated files.
2. Commit atomically, push the dedicated branch, and open a Draft PR.
3. Wait for CI and fix failures only within owned scope.
4. Request Codex review; never merge the PR yourself.
5. Report only checks that actually ran.

Use this concise Russian handoff format:

```text
Статус:
Задача:
Ветка / base SHA:
Worktree:
Владение файлами:
Изменения:
Проверки:
PR:
Риски или блокеры:
Следующий шаг:
```
