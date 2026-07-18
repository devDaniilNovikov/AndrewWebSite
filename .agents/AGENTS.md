# AndrewWebSite — Codex Instructions

## Mission and Ownership
- Act as the primary implementation and coordination agent for `AndrewWebSite`.
- Communicate with the user in Russian; write code, identifiers, contracts, branches, and commits in English.
- Own architecture, Spring Boot backend, API, PostgreSQL queue, integrations, deployment, security, and final PR review.
- Execute one explicitly assigned atomic task at a time; make the smallest safe change.
- Do not dispatch agents or make architectural/product decisions without explicit user approval.
- Claude Code owns frontend product UI; Jules owns CI, regression infrastructure, dependency updates, and isolated maintenance.

## Agent Documentation
- Codex context and common rules: [`.agents/AGENTS.md`](AGENTS.md) (this document).
- Claude Code specification: [`.agents/CLAUDE.md`](CLAUDE.md).
- Jules documentation entry point: [`.agents/workflows/JULES_AUTOMATION.md`](workflows/JULES_AUTOMATION.md).
- Canonical Jules automation guide: [`.github/JULES_AUTOMATION.md`](../.github/JULES_AUTOMATION.md).
- Root [`AGENTS.md`](../AGENTS.md) and [`CLAUDE.md`](../CLAUDE.md) remain symbolic entry points to their matching documents in `.agents`.

## Sources of Truth
- Repository: `devDaniilNovikov/AndrewWebSite`; default branch: `main`.
- Read `README.md`, `.github/JULES_AUTOMATION.md`, `docs/handoffs/2026-07-17-backend-superpowers-handoff.md`, task-specific docs, and the live Git/GitHub state before editing.
- Read `.planning/PROJECT.md`, `.planning/ROADMAP.md`, `.planning/STATE.md`, and `.planning/config.json` when present.
- Approved user decisions override older planning text; never invent missing business or legal facts.


## Repository State and Package Manager
- The repository currently contains GitHub/Jules automation and documentation; application scaffolds have not landed.
- No package manager is configured. After scaffolding, use only committed manifests and lockfiles.
- Do not invent application commands, dependencies, paths, or conventions before their manifests exist.

## File-Scoped Commands
| Task | Command |
| --- | --- |
| Validate one workflow | `actionlint .github/workflows/<file>.yml` |
| Validate all workflows | `actionlint .github/workflows/*.yml` |
- Use application scripts declared by the relevant manifest once available.


## Approved Product, Backend, and Execution Specification

### Product Context

AndrewWebSite is a Russian-language B2B website for commercial refrigeration equipment repair in Moscow and the nearest Moscow region.

Primary conversion:

- telephone call.

Secondary conversion:

- short lead form;
- durable PostgreSQL commit;
- asynchronous Telegram delivery;
- after a successful submission, the UI offers the user a direct call.

Not included in MVP:

- user registration or login;
- personal accounts;
- CRM or admin panel;
- ecommerce, payments, booking, CMS, or blog;
- Redis or a separate message broker.

Project sources:

- Technical brief: `/Users/daniilnovikov/Desktop/VibeCoding/Андрей/Техническое_задание_на_разработку_сайта.md`.
- Original project prompt: `/Users/daniilnovikov/.codex/attachments/90639068-9baf-44c3-a236-83b890e38160/pasted-text.txt`.
- Shared repository instructions: `AGENTS.md`.
- Jules automation description: `.github/JULES_AUTOMATION.md`.

### Fixed Architecture Decisions

#### Runtime and repository layout

- One Maven module, not a multi-module build.
- Root `pom.xml`, Maven Wrapper, and `src/` contain the Java application.
- The frontend will live under `frontend/` and is owned by Claude.
- The Java root package is `ru.andrew.website`.
- Code is organized by feature: lead intake, Telegram delivery, privacy, observability, and common web/security infrastructure.
- Production uses one Java container on Timeweb Cloud App Platform.
- PostgreSQL 17 is a managed service in the same Moscow region/VPC.

#### Public surface and authorization

- MVP has no user authentication and no login endpoints.
- Public surface is limited to static pages, `POST /api/leads`, liveness, and a minimal readiness response.
- Spring Security is stateless: no sessions, form login, or HTTP Basic.
- Actuator environment, configuration, shutdown, heap, and raw metrics endpoints are not publicly exposed.
- Same-origin is the production default. Development origins are enabled only in the local profile.

#### Lead API

`POST /api/leads` accepts:

```json
{
  "requestId": "11111111-1111-4111-8111-111111111111",
  "name": "Иван",
  "phone": "+7 999 123-45-67",
  "comment": "Не охлаждает витрина",
  "sourcePath": "/remont-torgovogo-holodilnogo-oborudovaniya/",
  "intent": "repair",
  "consent": true,
  "website": ""
}
```

Contract:

- `202 Accepted` is returned only after the lead and outbox transaction commits.
- A non-empty `website` honeypot receives an indistinguishable synthetic `202` but creates no database row.
- Repeating the same `requestId` with the same canonical payload returns `202` and creates no second outbox item.
- Reusing the same `requestId` with another payload returns RFC 9457 `409 Conflict` while the fingerprint is retained.
- After PII retention expires, the payload fingerprint is removed; a replay of the retained request ID returns a safe `202` without creating a new lead.
- The fingerprint uses a keyed HMAC. Production reads `app.leads.fingerprint-key` only from the `LEAD_FINGERPRINT_HMAC_KEY` environment/secret-store value and fails fast when it is missing; only the test profile may use a fixed non-production key.
- Expected problem responses: `400`, `409`, `413`, `415`, `429`, and `503` using RFC 9457.
- No name, phone number, or comment may be written to application logs.

Validation defaults:

- `name`: trimmed, 2–100 characters.
- `phone`: at most 32 input characters and 7–15 digits after normalization.
- `comment`: optional, at most 1000 characters.
- `sourcePath`: local path only, without scheme, host, query, or fragment.
- `intent`: `repair` or `maintenance`.
- `consent`: exactly `true`.

#### Rate limiting

- Bounded in-memory token buckets; no Redis in MVP.
- Global limit: 60 requests per minute.
- Per-client default: burst 5, refill 1 request per minute.
- Never trust `X-Forwarded-For` until Timeweb proxy behavior and trusted proxy CIDRs are verified.
- Before that verification, use the connection address plus the global fallback.
- The limiter protects application resources but is not a replacement for edge/L3/L4 DDoS protection.

#### PostgreSQL outbox

Separate `leads` and `telegram_outbox` tables are required. The outbox references the lead and does not store a duplicate JSON copy of PII.

Queue states:

- `pending`;
- `processing`;
- `retry`;
- `blocked`;
- `delivered`.

Worker behavior:

- poll every 15 seconds;
- claim at most 10 records;
- use `FOR UPDATE SKIP LOCKED`;
- lease records for two minutes;
- perform the Telegram HTTP request outside the claim transaction;
- retry from 30 seconds to six hours with exponential backoff;
- honor Telegram `retry_after` for HTTP 429;
- recover expired leases after crashes or restarts;
- include `requestId` in the Telegram message for human duplicate recognition.

Delivery is at-least-once. A crash after Telegram accepts a request but before PostgreSQL records `delivered` can result in a duplicate Telegram message; this is an accepted and documented limitation.

#### Privacy

- PII must not remain longer than 30 days from lead creation, including undelivered leads.
- The application anonymizes at an operational threshold of 29 days so scheduled execution does not cross the hard limit.
- Anonymization removes name, phone, comment, and payload fingerprint.
- Undelivered outbox entries become `blocked` with the technical reason `privacy_expired`.
- Anonymized technical rows are deleted after 12 months.
- PostgreSQL backup retention must not exceed 30 days.
- The Telegram destination must have auto-delete configured for no more than 30 days; this is a production operations gate.

#### Observability

- Actuator and Micrometer are present from the foundation phase.
- Liveness never depends on PostgreSQL or Telegram.
- Readiness reflects PostgreSQL availability and worker heartbeat without exposing sensitive details.
- Queue/worker code records Micrometer telemetry when it is introduced. OTLP push to Grafana Cloud is configured in `task-backend-observability` before production release.
- Do not self-host Prometheus or Grafana in MVP.
- Metric tags must never contain PII or unbounded request IDs.

### Git and Superpowers Execution Protocol

Every task follows this exact lifecycle:

1. Fetch `origin` and verify that the required previous PR has been merged.
2. Create a new external worktree from fresh `origin/main`.
3. Create exactly one branch named `task-<short-kebab-description>`; fixes use `fix-<short-kebab-description>`.
4. Record a git-ignored task brief and `.superpowers/sdd/progress.md`.
5. Dispatch one fresh implementer subagent. Do not run parallel implementers against the same task.
6. Apply strict TDD for behavior:
   - RED: write the test and observe the intended failure;
   - GREEN: implement the smallest working change and observe the pass;
   - REFACTOR: improve structure while keeping the suite green.
7. Run implementer self-review and create conventional commits.
8. Dispatch a fresh specification reviewer, followed by a quality/security reviewer.
9. Fix every Critical and Important finding and repeat the relevant review.
10. Run fresh verification in the same turn: tests, coverage, formatting, dependency/security scans, and container checks where applicable.
11. Push and create a Draft PR.
12. Wait for GitHub CI. After green CI and final review, mark the PR Ready.
13. Never merge automatically. Stop until the user explicitly authorizes merge.
14. After merge, remove the completed worktree and create the next task from the new `origin/main`.

Model allocation for ephemeral Superpowers workers:

- routine scaffolding, documentation, and mechanical implementation: `gpt-5.6-terra`;
- security, transactions, queue concurrency, privacy, and integration: `gpt-5.6-sol`;
- reviews: independent reviewer role with high reasoning.

Persistent project roles:

- Codex: coordinator, architecture, backend, integration, security, deployment, and final review.
- Jules: CI/test infrastructure, regression tests, dependency maintenance, and isolated fixes.
- Claude: frontend implementation and frontend-owned tests after its instructions are refreshed.

### Ordered Implementation Phases

No stacked PRs are allowed. The next task starts only after the required previous PR is merged.

#### Phase 0 — executable specifications

#### `task-backend-contract-plans` — Codex

Create:

- `docs/backend/architecture.md`;
- `docs/backend/openapi.yaml`;
- `docs/backend/operations.md`;
- `docs/superpowers/plans/2026-07-18-backend-foundation.md`;
- `docs/superpowers/plans/2026-07-18-lead-intake.md`;
- `docs/superpowers/plans/2026-07-18-telegram-delivery.md`;
- `docs/superpowers/plans/2026-07-18-privacy-observability.md`;
- `docs/superpowers/plans/2026-07-18-static-deployment.md`.

Each implementation plan must contain exact file paths, Java signatures, test code, RED/GREEN commands, expected outcomes, and commit boundaries. Placeholders such as `TBD`, `TODO`, “add tests,” or “handle errors appropriately” are forbidden.

Acceptance:

- OpenAPI and architecture agree on field names and responses.
- Queue states and transitions are complete.
- Every approved requirement maps to one implementation task.
- Independent specification review has no Critical or Important findings.

#### Phase 1 — application foundation

#### `task-backend-skeleton` — Codex

- Maven Wrapper and one-module Spring Boot 4.1.0 application.
- Java 25 toolchain and root package `ru.andrew.website`.
- Test, local, and production configuration profiles.
- Actuator/Micrometer foundation and dependency-free liveness.
- JUnit, Spring test, and JaCoCo baseline.
- First context and smoke tests.

#### `task-ci-backend-gates` — Jules

This task starts only after `task-backend-skeleton` is merged.

- Temurin Java 25 in GitHub Actions.
- `./mvnw -B verify`.
- JaCoCo coverage threshold of at least 80%.
- Testcontainers-compatible CI runtime.
- Dependency review and Java security analysis.
- Maven caching and least-privilege workflow permissions.

Important trigger behavior:

- Merging skeleton triggers a normal `push` CI run on `main`.
- A successful merge does **not** automatically create `task-ci-backend-gates` in Jules.
- `jules-ci-failure.yml` invokes Jules only for an eligible failed push CI run.
- `pr-event-relay.yml` sends signed PR lifecycle events to a future product backend receiver; it does not assign Jules tasks and is currently disabled by default.
- To start this planned Jules task, `JULES_ALLOWED_ACTOR` must create a sanitized GitHub Issue containing its task brief and the same allowed account must apply exactly one label: `jules-action`.
- If a task originates from another account, copy its verified requirements into a new owner-authored sanitized issue; do not label the untrusted source issue.
- Do not combine `jules` and `jules-action`, because that can create two Jules sessions.

#### `task-backend-deploy-stub` — Codex

- Multi-stage Java 25 container build.
- Non-root runtime user.
- Container healthcheck targets liveness.
- No embedded secrets or production deployment mutation.
- Docker build and container smoke test.

#### Phase 2 — public lead intake

#### `task-backend-http-security` — Codex

- Stateless Spring Security and public route allowlist.
- Closed Actuator surface.
- RFC 9457 error foundation.
- Request body and field-size enforcement.
- Same-origin production behavior.
- Bounded in-memory rate limiter and trusted-proxy fallback policy.
- Tests for forbidden endpoints, content type, CORS, rate limits, and payload limits.

#### `task-db-flyway-baseline` — Codex

- PostgreSQL/JdbcClient/Flyway configuration.
- `leads` and `telegram_outbox` migrations.
- Unique idempotency constraint.
- Queue indexes, leases, attempt counters, and privacy timestamps.
- Testcontainers migration and constraint tests.

#### `task-leads-api` — Codex

- DTOs and server-side validation.
- Canonical payload fingerprinting through keyed HMAC.
- `LEAD_FINGERPRINT_HMAC_KEY` production binding, fail-fast validation, and a test-profile-only fixed key.
- Atomic lead and outbox transaction.
- Safe duplicate and conflict behavior.
- Silent honeypot acceptance.
- MockMvc, JdbcClient, rollback, race, and unavailable-database tests.

#### Phase 3 — Telegram delivery

#### `task-telegram-client` — Codex

- `RestClient` adapter behind a gateway interface.
- Minimal Telegram message containing source, intent, time, and request ID.
- Tokens and chat ID only through secret configuration.
- Fake Telegram tests for success, 400/401/403, 429, 5xx, timeouts, and network errors.

#### `task-telegram-worker` — Codex

- Scheduled polling, batch claim, lease, retry, blocked, and delivered transitions.
- `FOR UPDATE SKIP LOCKED` concurrency.
- Retry-after and exponential backoff.
- Restart and expired-lease recovery.
- Bounded, PII-free Micrometer counters and queue/worker measurements for later OTLP export.
- Tests with two workers and all failure transitions.

#### Phase 4 — privacy and operations

#### `task-lead-retention` — Codex

- Clock-driven PII anonymization at the 29-day operational threshold.
- Privacy expiry for undelivered queue records.
- Deletion of anonymized technical rows after 12 months.
- Retention heartbeat and failure visibility.
- Boundary tests proving that PII cannot survive the 30-day limit.
- Post-retention idempotency test: anonymize the lead and clear its fingerprint, replay the retained `requestId` with a changed payload, expect `202`, and assert that no new lead or outbox row exists.

#### `task-backend-observability` — Codex

- Safe liveness/readiness.
- Structured redacted logs.
- Lead, rate-limit, queue, worker, delivery, and retention metrics.
- OTLP configuration without exposing raw metrics publicly.
- Tests for degraded dependencies and PII-free telemetry.

#### Phase 5 — final application artifact

#### `task-static-jar-integration` — Codex with Claude frontend dependency

- Refresh and merge Claude instructions before frontend execution.
- Do not start this task until the frontend scaffold, package-manager lockfile, static-export command, output path, and frontend tests are merged into `main`.
- Build the Next.js static export using Node 24.
- Embed the export into the Spring Boot JAR.
- Preserve `/api/**` routing and real 404 behavior.
- Configure cache headers for HTML and hashed assets.
- Produce a single production Java container.
- Smoke-test the home page, static assets, 404, lead API, liveness, and readiness.

### Verification Requirements

Backend completion requires fresh evidence for all of the following:

- `./mvnw -B verify` succeeds.
- JaCoCo coverage is at least 80%.
- Flyway migrations succeed against PostgreSQL 17.
- MockMvc API contract tests pass.
- Duplicate requests and conflicting payloads behave as specified.
- Replays after fingerprint deletion return safe `202` without recreating a lead or outbox item.
- Two workers cannot claim the same outbox item concurrently.
- Expired leases and application restarts are recoverable.
- Telegram 429 respects `retry_after`.
- PII is removed within the retention boundary, including from undelivered leads.
- Logs and metrics contain no lead PII.
- Sensitive Actuator endpoints are not public.
- Secret and dependency scans pass.
- The final container passes smoke tests.

No task may be described as complete using old, partial, or inferred test results.

### Systematic Debugging Rule

For every unexpected failure:

1. Reproduce it reliably.
2. Identify the failing layer and collect evidence.
3. Form one testable root-cause hypothesis.
4. Add a failing regression test where technically possible.
5. Apply the smallest fix.
6. Repeat the focused test and then the complete quality gate.

After three unsuccessful fixes for the same root condition, stop implementation and revisit the architecture with the user.

### Security Handoff

- GitHub PAT and Jules API key previously posted in chat are compromised.
- Never repeat, use, save, log, or forward them to any agent.
- Never request replacement secrets in chat.
- GitHub access uses browser OAuth, SSH, GitHub Connector, or the local credential manager.
- Jules uses a newly generated secret stored only as the GitHub Actions secret `JULES_API_KEY`.
- Telegram and production credentials must be supplied only through an approved secret store.

### Production Blockers That Do Not Block Local Backend Development

- Final company name and legal requisites.
- Production domain and public phone number.
- Telegram destination and safely configured bot credential.
- Final privacy, consent, and warranty texts.
- Analytics provider decision.
- Timeweb environment and managed PostgreSQL credentials.
- Confirmation of Timeweb trusted proxy behavior.
- PostgreSQL backup retention of no more than 30 days.
- Telegram chat auto-delete of no more than 30 days.


## Security and Automation
- Never commit, expose, log, extract, or request secrets, tokens, credentials, private keys, or lead PII.
- Validate external input at system boundaries and use parameterized database access.
- Keep third-party Actions pinned to full SHAs and GitHub permissions least-privilege.
- Never weaken actor, event, branch, signature, replay-protection, authorization, or rate-limit checks.
- `pull_request_target` must never check out or execute PR code.
- Jules may create reviewable PRs but must never merge them; keep `.github/JULES_AUTOMATION.md` synchronized.
- Stop on secret exposure, data-loss risk, ownership conflict, or architectural drift.


## Implementation and Quality
- Use `set -euo pipefail` in multi-line Bash workflow steps.
- Use TDD for business logic, API handlers, validation, transformations, state machines, and workflows.
- Add regression coverage for bug fixes when technically possible; never weaken a valid failing test.
- Target at least 80% meaningful coverage when application test infrastructure exists.
- Handle errors at every boundary; provide safe client messages and detailed server-side context without sensitive data.
- Prefer immutable data flow, small focused functions, and feature/domain organization.
- Review the final diff for unrelated changes and credentials; report only checks that actually ran.


## Commit Attribution
AI-authored commits must include the agent's own attribution footer, never a human identity:

```text
Co-Authored-By: <agent name/model> <agent-provided noreply address>
```
