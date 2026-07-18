# Backend architecture and executable contracts

## Status and scope

This document is the binding Phase 0 backend design for AndrewWebSite. It defines the names, types, boundaries, transactions, and operating rules reused by the OpenAPI contract and the implementation plans. The MVP is a Russian-language B2B static website plus one public lead command. It has no login, sessions, user accounts, administration UI, CRM, ecommerce, booking, payments, CMS, blog, Redis, or separate message broker.

The fixed platform is Java 25 LTS, Spring Boot 4.1.0, one root Maven module, Maven Wrapper, PostgreSQL 17, and one final Java container. The Java root package is `ru.andrew.website`. The frontend remains owned under `frontend/` and uses Next.js 16.2.9, React 19.2.x, strict TypeScript, Tailwind CSS 4, Motion, and Node 24 only during the build.

## System and trust boundaries

```text
Public browser
  | same-origin HTTPS: static GET/HEAD, POST /api/leads, health GET
  v
Timeweb Cloud App Platform ingress (proxy trust not yet established)
  |
  v
Single non-root Spring Boot container
  |-- static resource handler
  |-- public web/security boundary
  |-- lead intake transaction
  |-- Telegram outbox worker
  |-- privacy retention worker
  |-- safe health and telemetry
  |
  +---- TLS/VPC ----> managed PostgreSQL 17 in the Moscow region
  +---- HTTPS ------> Telegram Bot API
  +---- HTTPS/OTLP -> Grafana Cloud collector (production only after gate)
```

The public browser and all request headers are untrusted. Until Timeweb publishes or confirms the actual forwarding behavior and trusted proxy CIDRs, the application ignores `Forwarded` and `X-Forwarded-For` for rate-limit identity and uses only `HttpServletRequest.getRemoteAddr()` plus the global limiter. Enabling forwarded-header processing is a production change gate, not a default.

The global limiter is a rolling window, not a token bucket: for every instant `t`, the half-open interval `(t - 60 seconds, t]` contains at most 60 globally admitted requests. It stores only the at-most-60 admission timestamps needed for that window; timestamps at or before `t - 60 seconds` expire before the next decision. A separate bounded per-connection-address token bucket has capacity 5 and refills exactly one token per minute. The global decision is evaluated first, so any request that passes both gates is necessarily within the rolling global cap; a rejection returns the ceiling in whole seconds until the applicable oldest timestamp or client token becomes available.

PostgreSQL, Telegram, and OTLP are outbound dependencies. Credentials cross into the container only through the platform secret store and environment bindings described in `operations.md`; no secret is embedded in a file, image layer, log, metric, health response, or exception response.

## Container and component boundaries

One Spring Boot process contains these feature packages:

| Package | Responsibility | May depend on |
| --- | --- | --- |
| `ru.andrew.website` | `AndrewWebsiteApplication` bootstrap only | feature configuration |
| `ru.andrew.website.common` | UTC `Clock` and other cross-feature value-only infrastructure | JDK and Spring configuration only |
| `ru.andrew.website.web` | public route allowlist, stateless security, RFC 9457 mapping, payload/content-type checks, CORS, client address, rate limits, static fallback | Spring MVC/Security, lead application port |
| `ru.andrew.website.leads` | request DTOs, normalization, HMAC fingerprinting, idempotency decision, transactional lead/outbox creation | `JdbcClient`, transaction manager, `Clock` |
| `ru.andrew.website.telegram` | Telegram gateway, outbox claim/lease state machine, scheduler, retry policy, bounded telemetry | lead projection, `JdbcClient`, `RestClient`, `Clock`, `MeterRegistry` |
| `ru.andrew.website.privacy` | 29-day anonymization, queue privacy block, 12-month deletion, retention heartbeat | `JdbcClient`, transaction manager, `Clock`, `MeterRegistry` |
| `ru.andrew.website.observability` | minimal health contributors, worker heartbeats, redaction policy, bounded metrics | dependency probes and heartbeat ports only |

Feature internals expose explicit ports; controllers do not issue SQL, workers do not parse HTTP requests, and the Telegram gateway does not own queue state. Records and enums are immutable.

## Public HTTP surface

The executable request/response detail is in `openapi.yaml`. The only public backend-owned routes are:

- `POST /api/leads` with `Content-Type: application/json` and a hard 16 KiB body limit;
- `GET /actuator/health/liveness`;
- `GET /actuator/health/readiness`.

Static `GET` and `HEAD` requests are served from the packaged frontend. `/api/**` and `/actuator/**` never fall through to static content. `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`, environment, configuration, shutdown, heap, mappings, loggers, and every other actuator endpoint are denied externally. There are no authentication or login routes.

Production is same-origin and has no CORS allowlist. The `local` profile may allow only explicitly configured development origins. The lead endpoint is stateless, uses no cookies, and has CSRF disabled only for `POST /api/leads`; all other non-safe methods remain denied by the route allowlist.

## Lead request model

`LeadRequest` is the mutually exclusive `oneOf` of a legitimate lead object and a
honeypot object. Both shapes reject unknown JSON properties and retain the declared
JSON types for every known property. The legitimate shape has exactly these JSON
properties:

| Property | Java type | Contract |
| --- | --- | --- |
| `requestId` | `UUID` | required RFC 4122 UUID for legitimate requests |
| `name` | `String` | required; trim; Unicode NFC; 2–100 characters after normalization |
| `phone` | `String` | required; at most 32 input characters; normalize to digits only; 7–15 digits |
| `comment` | `String` | optional; trim; Unicode NFC; blank becomes `null`; at most 1000 characters |
| `sourcePath` | `String` | required; trim; 1–2048 characters; starts with `/`; no scheme, authority, query, fragment, backslash, control character, or `..` path segment |
| `intent` | `LeadIntent` | required; exactly `repair` or `maintenance` |
| `consent` | `Boolean` | required and exactly `true` |
| `website` | `String` | optional; missing, null, or empty selects the legitimate shape |

The alternate honeypot shape requires only a non-empty `website` string; every
legitimate field may be absent. A honeypot may include
known fields, but they must still cross the normal JSON type boundary (`UUID`, string,
enum, or boolean) because deserialization and unknown-property rejection happen before
classification. After the common request-size, media-type, JSON-deserialization, and
rate-limit boundary, the service checks `website != null && !website.isEmpty()` before Bean Validation or
normalization and returns the exact empty `202` without persistence, HMAC, or Telegram
side effects. Thus `{"website":"filled-by-bot"}` is accepted synthetically, while an
unknown property or an invalid typed known property is `400`. The website value has no
field-size bound beyond the 16 KiB body limit because it is never stored.

For legitimate requests, `LeadNormalizer.normalize(LeadRequest)` produces immutable `NormalizedLead` fields `requestId`, `name`, `phoneDigits`, `comment`, `sourcePath`, `intent`, and `consentedAt`. The payload fingerprint excludes `requestId`, `website`, and `consentedAt`; it is HMAC-SHA-256 over UTF-8 canonical JSON with keys in this fixed order: `name`, `phone`, `comment`, `sourcePath`, `intent`, `consent`. `comment` is JSON `null` when absent and `consent` is always JSON `true`. Production obtains the HMAC key only from `LEAD_FINGERPRINT_HMAC_KEY`; startup fails if its UTF-8 representation has fewer than 32 bytes. Only the `test` profile may bind the visibly non-production value `test-only-key-material-not-for-production-0001`.

## End-to-end lead and idempotency flow

1. The web boundary rejects bodies over 16 KiB as `413`, non-JSON media types as `415`, malformed JSON, unknown fields, and typed deserialization failures as `400`, and exhausted bounded token buckets as `429`.
2. A non-empty honeypot returns an empty `202` and stops.
3. The service normalizes the request and computes its keyed fingerprint without logging any request field.
4. In one PostgreSQL transaction, lock or insert by `request_id`:
   - no retained row: insert one `leads` row and one `telegram_outbox` row;
   - retained row with equal non-null fingerprint: insert nothing;
   - retained row with a different non-null fingerprint: roll back and return `409`;
   - retained row with a null fingerprint: insert nothing and return safe `202`.
5. Only after the transaction commits does the controller return the same empty `202` for first acceptance, equal duplicate, post-retention replay, and honeypot.
6. PostgreSQL unavailable before commit returns `503`; a rollback can never produce `202`.

The unique `leads.request_id` constraint is the concurrency authority. A racing loser rereads the committed row and applies the same equal/conflict/null-fingerprint decision. No response reveals which acceptance branch occurred.

## Persistence contract

Flyway migration `src/main/resources/db/migration/V1__lead_outbox_baseline.sql` creates both tables in one versioned migration. All timestamps use `timestamp with time zone` and application/database values are UTC.

### `leads`

| Column | PostgreSQL type | Constraints and meaning |
| --- | --- | --- |
| `id` | `bigint generated by default as identity` | primary key |
| `request_id` | `uuid` | not null; constraint `uk_leads_request_id` unique |
| `payload_fingerprint` | `bytea` | nullable only after anonymization; 32 bytes while present |
| `name` | `varchar(100)` | nullable only after anonymization |
| `phone` | `varchar(15)` | nullable only after anonymization; normalized digits |
| `comment` | `varchar(1000)` | nullable |
| `source_path` | `varchar(2048)` | not null; retained as non-contact operational context |
| `intent` | `varchar(16)` | not null; check `repair` or `maintenance` |
| `consented_at` | `timestamptz` | not null |
| `created_at` | `timestamptz` | not null |
| `anonymized_at` | `timestamptz` | nullable; once set, PII and fingerprint are null |

Checks enforce a 32-byte fingerprint when non-null, phone length 7–15 when non-null, valid intent, and the all-or-none PII invariant (`anonymized_at is null` with name/phone/fingerprint present, or `anonymized_at is not null` with all four PII columns null). Index `idx_leads_retention(created_at, id) where anonymized_at is null` supports anonymization; `idx_leads_anonymized_cleanup(anonymized_at, id) where anonymized_at is not null` supports deletion.

### `telegram_outbox`

| Column | PostgreSQL type | Constraints and meaning |
| --- | --- | --- |
| `id` | `bigint generated by default as identity` | primary key |
| `lead_id` | `bigint` | not null; unique; foreign key to `leads(id)` with cascade delete |
| `state` | `varchar(16)` | `pending`, `processing`, `retry`, `blocked`, or `delivered` |
| `attempt_count` | `integer` | not null default 0; non-negative; incremented on claim |
| `next_attempt_at` | `timestamptz` | not null; claim eligibility |
| `lease_token` | `uuid` | present only while processing |
| `lease_until` | `timestamptz` | present only while processing |
| `last_error_code` | `varchar(64)` | nullable bounded technical code; never exception text or PII |
| `created_at` | `timestamptz` | not null |
| `updated_at` | `timestamptz` | not null |
| `delivered_at` | `timestamptz` | present only when delivered |

The outbox stores no name, phone, comment, message JSON, or other duplicate PII. Check constraints enforce state-dependent lease and delivery columns. `idx_telegram_outbox_claim(next_attempt_at, id) where state in ('pending','retry')` and `idx_telegram_outbox_expired_lease(lease_until, id) where state = 'processing'` bound queue scans.

## Queue transactions and state machine

Polling occurs every 15 seconds. A claim transaction first recovers expired `processing` rows to `retry`, clears their lease, assigns `next_attempt_at = now()`, and records `last_error_code = 'lease_expired'`. It then selects at most 10 due `pending`/`retry` rows whose joined lead is non-anonymized and younger than the 29-day operational privacy threshold, ordered by `next_attempt_at, id`, using `FOR UPDATE OF telegram_outbox SKIP LOCKED`. It changes each to `processing`, increments `attempt_count`, assigns a random `lease_token`, and sets `lease_until = now() + interval '2 minutes'`. The transaction returns immutable `ClaimedDelivery` values and commits before any Telegram HTTP call. Immediately before send, the worker reloads the lead using the processing state, lease token, non-anonymized flag, and the same privacy-age predicate; an absent projection is not sent.

Every completion update matches `id`, `state = 'processing'`, and `lease_token`; a stale worker therefore cannot overwrite a recovery or privacy decision.

| From | Event | To | Atomic effects |
| --- | --- | --- | --- |
| `pending` | due claim | `processing` | increment attempt; set two-minute lease/token |
| `retry` | due claim | `processing` | increment attempt; set two-minute lease/token |
| `processing` | Telegram success | `delivered` | clear lease; set `delivered_at`; clear error |
| `processing` | timeout, network error, 5xx, or 429 | `retry` | clear lease; set bounded error code and `next_attempt_at` |
| `processing` | Telegram non-429 4xx | `blocked` | clear lease; set `telegram_permanent_<status>` |
| `processing` | lease expires | `retry` | clear lease; set due now and `lease_expired` |
| `pending`, `retry`, or `processing` | lead reaches privacy threshold | `blocked` | clear lease; set `privacy_expired`; no later delivery |
| `blocked` | none in MVP | `blocked` | terminal |
| `delivered` | none | `delivered` | terminal |

For delivery attempt number `n >= 1`, exponential delay is `min(30 seconds * 2^(n - 1), 6 hours)`, calculated without numeric overflow. For Telegram 429, parse a positive bounded `retry_after` and use `min(6 hours, max(exponential delay, retry_after seconds))`. Invalid `retry_after` is treated as a normal transient failure. Telegram HTTP occurs outside the claim transaction.

Delivery is at least once. A crash before the HTTP call leaves a lease that is recovered. A crash during a call also recovers after lease expiry. A crash after Telegram accepts but before `delivered` commits can send a duplicate; including `requestId` in the Telegram message lets the recipient recognize it. A database failure while recording an outcome leaves the lease to recover. This accepted duplicate window cannot be removed without a Telegram-side idempotency contract.

## Telegram message and gateway

`TelegramGateway.send(TelegramLeadMessage)` returns a sealed `TelegramDeliveryResult`: `Delivered`, `Retryable(code, retryAfter)`, or `PermanentFailure(code)`. The message contains the normalized name, phone, optional comment, source path, intent, lead creation time, and `requestId`. It is assembled only after claim and is never persisted in the outbox or logged. Bot token and chat ID come only from `TELEGRAM_BOT_TOKEN` and `TELEGRAM_CHAT_ID`, and production startup fails if either is absent.

## Privacy lifecycle

The retention worker runs hourly against a supplied `Clock` and updates bounded batches. At `created_at <= now - 29 days`, one transaction:

1. changes all non-delivered outbox states, including leased `processing`, to `blocked` with `privacy_expired` and clears leases;
2. nulls `name`, `phone`, `comment`, and `payload_fingerprint` and sets `anonymized_at`;
3. commits both effects together.

This 29-day operational threshold provides a one-day margin before the hard 30-day PII limit and applies even when Telegram is unavailable. At `anonymized_at <= now - 12 months`, technical rows are deleted; cascade removes the associated outbox row. The retained `request_id` gives safe replay only until that deletion. PostgreSQL backup retention must be at most 30 days, and the Telegram destination auto-delete must be at most 30 days; both are verified production release gates.

## Logging and telemetry contract

Application logs never contain a lead name, phone, comment, request body, canonical payload, fingerprint, Telegram message, bot token, chat ID, database credentials, OTLP headers, or exception content derived from those values. Safe correlation uses generated bounded operation IDs, outbox numeric IDs, state, outcome code, and aggregate counts; `requestId` is not a log or metric tag.

Micrometer names are bounded and use only enumerated tags:

- `andrew.leads.accepted` with `outcome=created|duplicate|retained|honeypot`;
- `andrew.leads.rejected` with `reason=validation|conflict|payload|media_type|rate_limit|unavailable`;
- `andrew.telegram.delivery` with `outcome=delivered|retry|blocked` and bounded `reason`;
- `andrew.telegram.queue.depth` with `state` from `OutboxState`;
- `andrew.telegram.worker.last_success.age`;
- `andrew.privacy.anonymized`, `andrew.privacy.deleted`, and `andrew.privacy.last_success.age`.

No PII, raw URL, exception message, dynamic status text, or request ID is a metric tag. OTLP is added and enabled only in `task-backend-observability`; no Prometheus/raw metrics endpoint is public.

## Health and heartbeat semantics

`/actuator/health/liveness` is dependency-free and includes only Spring application liveness. It never checks PostgreSQL, Telegram, worker delivery, retention, or OTLP.

`/actuator/health/readiness` returns only `{"status":"UP"}` or `{"status":"DOWN"}`. It is `UP` only when PostgreSQL accepts the bounded validation query and the outbox worker has completed a successful poll within 45 seconds. A successful poll means lease recovery and claiming completed and the entire claimed batch finished: every Telegram delivered/retry/blocked decision was durably recorded, while a privacy-invalidated reload was safely skipped. An empty completed poll is successful. Any exception from reload, send, or state persistence, or any lease-token state update returning false, aborts the poll and does not advance the heartbeat. The worker has a 45-second startup grace. Telegram availability itself is not readiness because expected Telegram failures become durable queue outcomes. Retention success is not readiness; its last-success heartbeat becomes stale after two hours and raises an operational alert. Health bodies never include dependency names, errors, hostnames, durations, counts, or configuration. Every liveness and readiness response, including `200` and `503`, has exactly `Cache-Control: no-store`; a path-scoped response filter pins that value and MockMvc tests protect both paths.

## Build and runtime topology

Phase 1 creates root `pom.xml`, `.mvn/wrapper/`, `mvnw`, `mvnw.cmd`, and `src/`. Phase 5 may start only after the merged frontend supplies its package-manager manifest, lockfile, static-export command, tests, and output path. Maven then runs Node 24 only in the build stage, invokes the manifest-declared manager directly through Corepack with a writable `COREPACK_HOME` and no shim installation, copies `frontend/out/` into generated static resources, and packages one executable Spring Boot JAR. Before any `COPY frontend/`, `.dockerignore` excludes root and nested `.env*`, local secret/credential directories, and private-key/keystore material; an executable container contract test pins those exclusions. The final container contains Java 25 runtime plus that JAR, runs as a non-root numeric user, exposes no Node runtime, and health-checks liveness.

Production gates are: PostgreSQL 17 in the same Moscow region/VPC; secret-store bindings and fail-fast startup; schema migration success; backup retention no more than 30 days; Telegram auto-delete no more than 30 days; verified OTLP delivery; verified Timeweb proxy behavior before forwarded-header trust; complete smoke tests; and an explicitly user-authorized squash merge. No plan mutates production infrastructure or embeds a production domain, phone, legal text, or credential.

## Ordered product-task dependency chain

The [canonical Git Flow](../../.agents/workflows/GIT_FLOW.md) governs every product task: `main` is the only long-lived branch; one approved task uses one dedicated external worktree and one lowercase `task-*` or `fix-*` branch created from the latest `origin/main`; direct pushes to `main`, stacked PRs, reused worktrees, and auto-merge are forbidden. The PR opens as Draft, becomes Ready only after required CI is green and Codex review is complete, and squash-merges only after explicit user authorization. After merge, confirm `main` and the linked issue, allow automatic remote-branch deletion, remove the local worktree only after checking that it has no tracked or untracked work to preserve, and run `git fetch --prune`. The next task waits for its predecessor to reach `main`:

```text
task-backend-contract-plans
  -> task-backend-skeleton
  -> task-ci-backend-gates
  -> task-backend-deploy-stub
  -> task-backend-http-security
  -> task-db-flyway-baseline
  -> task-leads-api
  -> task-telegram-client
  -> task-telegram-worker
  -> task-lead-retention
  -> task-backend-observability
  -> merged Claude frontend prerequisite
  -> task-static-jar-integration
```

The skeleton merge triggers only the normal `push` CI path. It does not assign Jules. `task-ci-backend-gates` starts only when `JULES_ALLOWED_ACTOR` authors a sanitized Issue and that same allowed account applies exactly one label, `jules-action`. Never add both `jules` and `jules-action`. `jules-ci-failure.yml` is only the eligible failed-push repair path; `pr-event-relay.yml` is disabled by default and never assigns Jules.

## Requirement-to-plan traceability

| Approved requirement | Product task | Executable plan |
| --- | --- | --- |
| executable architecture, OpenAPI 3.1 contract, operations runbook, five exact implementation plans, and full requirement traceability | `task-backend-contract-plans` | this document, `openapi.yaml`, `operations.md`, and all five plan files |
| one Maven module, wrapper, Java 25, Boot 4.1.0, feature packages, profiles, exact no-store health filter/tests, smoke tests, JaCoCo | `task-backend-skeleton` | `2026-07-18-backend-foundation.md` Task 1 |
| Temurin 25 CI, verify, 80% coverage, Testcontainers runtime, dependency/security gates, sanitized owner Issue trigger | `task-ci-backend-gates` | backend foundation Task 2 |
| multi-stage Java 25 image, non-root runtime, liveness healthcheck, Docker-context environment/credential/key exclusions, smoke test, no secrets/deploy mutation | `task-backend-deploy-stub` | backend foundation Task 3 |
| stateless security, allowlist, no auth endpoints, RFC 9457, 16 KiB JSON boundary, same-origin, bounded rate limits, proxy fallback, closed actuator | `task-backend-http-security` | `2026-07-18-lead-intake.md` Task 1 |
| PostgreSQL 17, separate constrained lead/outbox tables, unique request ID, indexes, leases, privacy timestamps, Flyway/Testcontainers | `task-db-flyway-baseline` | lead intake Task 2 |
| mutually exclusive legitimate/website-only honeypot shapes, typed/unknown JSON boundary, HMAC, atomic commit, duplicate/conflict/post-retention behavior, no-row/rollback/race/unavailable tests | `task-leads-api` | lead intake Task 3 |
| RestClient gateway, secret binding, actionable minimal message, compiling OptionalLong retry parser, Telegram success/permanent/429/5xx/timeout/network outcomes | `task-telegram-client` | `2026-07-18-telegram-delivery.md` Task 1 |
| 15-second poll, batch 10, two-minute lease, SKIP LOCKED, HTTP outside transaction, retries, recovery, transitions, two-worker tests, bounded metrics | `task-telegram-worker` | telegram delivery Task 2 |
| 29-day anonymization, 30-day hard limit, privacy-expired block, fingerprint removal, post-retention replay, 12-month deletion, heartbeat | `task-lead-retention` | `2026-07-18-privacy-observability.md` Task 1 |
| dependency-free liveness, database/worker readiness, exact no-store 200/503 headers, redacted logs, safe metrics, OTLP only here, no raw metrics | `task-backend-observability` | privacy observability Task 2 |
| merged frontend prerequisite, direct non-root Corepack under writable home, Node 24 static export, JAR embedding, secret-safe Docker context, `/api/**` preservation, true 404, cache rules, one Java image, final smoke matrix | `task-static-jar-integration` | `2026-07-18-static-deployment.md` Tasks 1–2 |
| PostgreSQL backup and Telegram auto-delete production gates; safe configuration, recovery, deploy and rollback | all operational tasks | `operations.md` and verification steps in all five plans |
| canonical task/fix worktree, Draft-to-Ready review, green CI/Codex gates, user-authorized squash merge, and safe cleanup | every product task | [canonical Git Flow](../../.agents/workflows/GIT_FLOW.md) and Global Constraints in all five plans |

## Primary implementation references

- Spring Boot 4.1.0 reference/API: health groups, `spring-boot-starter-webmvc`, configuration-property validation, Actuator, Micrometer OTLP, and static resources.
- Next.js 16.2.9 static export guide: `output: 'export'`, `next build`, and the default `out/` artifact.
- PostgreSQL 17 documentation: transactions, row-level locks, constraints, partial indexes, and `FOR UPDATE SKIP LOCKED`.
- OpenAPI 3.1.1 and RFC 9457 for the HTTP contract and problem details.
- Testcontainers for Java 2.0.3 for the `testcontainers-postgresql` and JUnit Jupiter integration.
