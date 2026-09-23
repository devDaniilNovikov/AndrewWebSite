# Backend architecture and executable contracts

## Status and scope
This document is the backend design for AndrewWebSite. It defines the names, types, boundaries, and operating rules reused by the OpenAPI contract. The MVP is a Russian-language B2B static website plus one public lead command. It has no login, sessions, user accounts, administration UI, CRM, ecommerce, booking, payments, CMS, blog, database, or message broker.

Since 2026-09-21 the backend is database-free: an accepted lead is delivered synchronously to the owner's Telegram chat before the visitor receives a response. The decision record is [`docs/superpowers/specs/2026-09-21-lead-form-telegram-delivery-design.md`](../superpowers/specs/2026-09-21-lead-form-telegram-delivery-design.md). PostgreSQL, Flyway, the Telegram outbox worker, the retention worker, Sentry, and OTLP export were removed; the task chain and traceability sections below record how the earlier design was built.

The fixed platform is Java 25 LTS, Spring Boot 4.1.0, one root Maven module, Maven Wrapper, and one final container running nginx plus the Spring Boot JAR. The Java root package is `ru.andrew.website`. The frontend remains owned under `frontend/` and uses Next.js 16.2.11, React 19.2.x, strict TypeScript, Tailwind CSS 4, Motion, and Node 24 only during the build.

## System and trust boundaries

```text
Public browser
  | same-origin HTTPS: static GET/HEAD, POST /api/leads
  v
Timeweb Cloud App Platform proxy (appends the visitor to X-Forwarded-For)
  |
  v
Single container, uid 10001
  |-- nginx on 0.0.0.0:8080
  |     |-- static frontend export (/var/www/html)
  |     |-- = /api/leads  --X-Real-IP--> 127.0.0.1:8090
  |     +-- any other /api/ or /actuator/ path -> 404
  |-- Spring Boot on 127.0.0.1:8090
  |     |-- coarse public perimeter admission
  |     |-- public web/security boundary and rate limits
  |     |-- lead acceptance and synchronous Telegram delivery
  |     +-- in-memory idempotency registry
  +-- management on 127.0.0.1:8081 (liveness/readiness, container HEALTHCHECK)
        |
        +---- HTTPS ------> Telegram Bot API
```

The public browser and all request headers are untrusted. nginx takes the last `X-Forwarded-For` element (the address the platform proxy observed; the TCP peer when the header is absent), sends it to the application as `X-Real-IP`, and removes `X-Forwarded-For`, `X-Forwarded-Host`, `X-Forwarded-Port`, `X-Forwarded-Proto`, and `Forwarded`. Tomcat's `RemoteIpValve` trusts `X-Real-IP` only from `127.0.0.1`; `ProductionHttpInvariantGuard` fails startup unless exactly this layout is configured. Rate-limit identity is therefore the visitor address, and a visitor cannot choose it.

Before media-type, body, routing, or authorization work, one coarse rolling perimeter gate admits at most 10,000 requests per application instance in every half-open `(t - 60 seconds, t]` interval across every method and public path, including malformed and otherwise rejected traffic. Its fixed problem response reveals neither the requested path nor the connection address. The limit reuses the canonical 10,000-entry in-memory client bound as a deliberately high availability ceiling; it is not the stricter lead-submission policy. The production management connector is bound to `127.0.0.1:8081`, bypasses this public gate, and is unreachable through the public listener, so platform liveness and readiness probes cannot consume or be starved by public admissions.

After body-size validation, the lead-only global limiter is a rolling window, not a token bucket: for every instant `t`, the half-open interval `(t - 60 seconds, t]` contains at most 60 admitted `POST /api/leads` requests. It stores only the at-most-60 admission timestamps needed for that window; timestamps at or before `t - 60 seconds` expire before the next decision. A separate bounded per-connection-address token bucket has capacity 5 and refills exactly one token per minute. The per-connection decision is evaluated first so traffic already rejected for one connection cannot consume lead-global admissions; a request that passes it then attempts the rolling global decision, so any request passing both lead gates is necessarily within the global cap. A rejection returns the ceiling in whole seconds until the applicable oldest timestamp or client token becomes available.

Telegram is the only outbound dependency. The bot token, chat ID, and HMAC key cross into the container only as platform runtime bindings described in `operations.md`; no secret is embedded in a file, image layer, log, metric, health response, or exception response. The process necessarily holds the token in memory because the Telegram protocol authenticates with it in the HTTPS request path.

## Container and component boundaries
One Spring Boot process contains these feature packages:

| Package | Responsibility | May depend on |
| --- | --- | --- |
| `ru.andrew.website` | `AndrewWebsiteApplication` bootstrap only | feature configuration |
| `ru.andrew.website.common` | UTC `Clock`, profile guard, detail-free production startup failure reporting | JDK and Spring configuration only |
| `ru.andrew.website.web` | security chain, perimeter and lead rate limits, body limit, problem responses, production HTTP invariant guard | leads metrics |
| `ru.andrew.website.leads` | request DTOs, normalization, HMAC fingerprinting, `LeadDelivery`, `TelegramLeadDelivery`, `LeadIdempotencyRegistry` | Telegram gateway port, `Clock` |
| `ru.andrew.website.telegram` | Telegram gateway, message formatting, endpoint guard, bounded client telemetry | `RestClient`, `ObservationRegistry` |
| `ru.andrew.website.observability` | health cache headers, bounded meter filter, production logging invariant guard | Micrometer, Spring configuration |

Controllers do not call Telegram directly, and the gateway neither remembers requests nor decides idempotency. Records and enums are immutable.

## Public HTTP surface

The executable request/response detail is in `openapi.yaml`. The only public backend-owned route is:

- `POST /api/leads` with `Content-Type: application/json` and a hard 16 KiB body limit.

Production health operations are available only to the loopback management connector on `127.0.0.1:8081`:

- `GET /actuator/health/liveness`;
- `GET /actuator/health/readiness`.

Static `GET` and `HEAD` requests are served by nginx from the frontend export. nginx answers every other `/api/` and `/actuator/` path with `404`, so they never fall through to static content, and the application connector itself is reachable only from loopback. `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`, environment, configuration, shutdown, heap, mappings, loggers, and every other actuator endpoint are unavailable externally. There are no authentication or login routes.

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
normalization and returns the exact empty `202` without HMAC or Telegram
side effects. Thus `{"website":"filled-by-bot"}` is accepted synthetically, while an
unknown property or an invalid typed known property is `400`. The website value has no
field-size bound beyond the 16 KiB body limit because it is never stored.

For legitimate requests, `requestId` must be a canonical RFC-variant UUIDv4. `LeadNormalizer.normalize(LeadRequest)` produces immutable `NormalizedLead` fields `requestId`, `name`, `phoneDigits`, `comment`, `sourcePath`, `intent`, and `consentedAt`. The payload fingerprint excludes `requestId`, `website`, and `consentedAt`; it is HMAC-SHA-256 over UTF-8 canonical JSON with keys in this fixed order: `name`, `phone`, `comment`, `sourcePath`, `intent`, `consent`. `comment` is JSON `null` when absent and `consent` is always JSON `true`. Production obtains the HMAC key only from `LEAD_FINGERPRINT_HMAC_KEY`; startup fails if its UTF-8 representation has fewer than 32 bytes. Only the `test` profile may bind the visibly non-production value `test-only-key-material-not-for-production-0001`.

## End-to-end lead and idempotency flow
1. nginx rejects bodies over 32 KiB; the application rejects bodies over 16 KiB as `413`, non-JSON media types as `415`, malformed JSON, unknown fields, and typed deserialization failures as `400`, and exhausted limits as `429`.
2. A non-empty honeypot returns an empty `202` and stops.
3. The service normalizes the request and computes its keyed fingerprint without logging any request field.
4. `TelegramLeadDelivery` looks up `requestId` in `LeadIdempotencyRegistry`:
   - remembered with an equal fingerprint: return `202` without sending again;
   - remembered with a different fingerprint: return `409`;
   - otherwise send the Telegram message now.
5. Telegram confirms (`2xx`): remember `requestId` with the fingerprint and return the empty `202`.
6. Any failure (network, timeout, `429`, `4xx`, `5xx`, redirect) returns `503` and writes one ERROR line `Telegram delivery failed: <code>`; nothing is remembered, so the form's retry with the same `requestId` sends again.

Concurrent requests that reuse one `requestId` are not serialized; the form never submits the same attempt in parallel. A response lost after Telegram accepted the message can lead to a second, identical message on retry.

## Idempotency registry

`LeadIdempotencyRegistry` keeps `requestId → (fingerprint, expiresAt)` in process memory for one hour and at most 10 000 entries. Expired entries are purged on every write; when full, the oldest entry is evicted. It never holds a name, phone, comment, or message. A restart forgets the registry; that only widens the duplicate-message window described above.

## Telegram message and gateway
`TelegramGateway.send(TelegramLeadMessage)` returns a sealed `TelegramDeliveryResult`: `Delivered`, `Retryable(code, retryAfter)`, or `PermanentFailure(code)`. The message is plain text with only the owner-facing contact details: `Имя`, `Телефон` (digits), and `Комментарий` when present. It is never persisted or logged. The synchronous gateway uses the Boot-managed `RestClient.Builder` (connect 3 s, read 8 s, no redirects — below the form's 15 s budget) and classifies every HTTP status without logging a Telegram response body. Because the Telegram protocol places the credential in the request path and Spring network exceptions retain the expanded URI, framework HTTP observations are disabled and all preconfigured request interceptors are removed from this client. A dedicated `andrew.telegram.client` observation exposes only the static `/bot{token}/sendMessage` route, method, and bounded outcome. The JSON request projection has a redacted `toString`. Bot token and chat ID come only from `TELEGRAM_BOT_TOKEN` and `TELEGRAM_CHAT_ID`; production uses the fixed `https://api.telegram.org` origin.

## Logging and telemetry contract
Application logs never contain a lead name, phone, comment, request body, canonical payload, fingerprint, Telegram message, bot token, chat ID, or exception content derived from those values; `requestId` is not a log or metric tag. In `prod` the console uses ECS JSON, the root level is `OFF`, and only `ProductionStartupFailureReporter` and `TelegramLeadDelivery` may log, at `ERROR`. `ProductionLoggingInvariantGuard` fails startup if that configuration is weakened.

Micrometer meters stay in process (no exporter) and use only enumerated tags:

- `andrew.leads.accepted` with `outcome=created|duplicate|honeypot`;
- `andrew.leads.rejected` with `reason=validation|conflict|payload|media_type|rate_limit|unavailable`;
- `andrew.telegram.client` with `method=POST`, the static token-free route, and `outcome=delivered|retryable|permanent_failure`.

## Health and heartbeat semantics
On the loopback-only management listener, `/actuator/health/liveness` includes only Spring application liveness and `/actuator/health/readiness` only Spring readiness; both return just `{"status":"UP"}` or `{"status":"DOWN"}`. Telegram availability is not part of either probe: a Telegram outage is reported to the visitor as `503`. Every liveness and readiness response has exactly `Cache-Control: no-store`. The container `HEALTHCHECK` probes liveness on `127.0.0.1:8081`.

## Build and runtime topology
The Dockerfile builds the frontend with `pnpm run build:production` on `node:24.14.0-alpine` (the production content gate must pass), packages the JAR with `./mvnw -B clean package -Dmaven.test.skip=true` (CI runs the full `./mvnw -B verify` on the same commit), and assembles the runtime on the pinned `eclipse-temurin:25.0.3_9-jre-noble` image with nginx. The image runs as `10001:10001`, contains no Node runtime, exposes only `8080`, and starts `deploy/entrypoint.sh`, which runs the JAR (`prod` profile) and nginx and exits non-zero as soon as either process exits so the platform restarts the container. `deploy/nginx.conf` keeps all writable paths in `/tmp` and has no access log. `.dockerignore` excludes secrets, key material, and local frontend build output; `ContainerContractTest` pins the Dockerfile, entrypoint, and nginx contract.

Production gates are: runtime bindings present and valid (startup fails otherwise), the bot able to write to the configured chat, Telegram auto-delete in that chat no longer than 30 days, complete smoke tests, and an explicitly user-authorized squash merge.

## Ordered product-task dependency chain
Historical: this chain built the earlier PostgreSQL/outbox design that the 2026-09-21 decision replaced.


The [canonical Git Flow](../../.agents/workflows/GIT_FLOW.md) governs every product task: `main` is the only long-lived branch; one approved task uses one dedicated external worktree and one lowercase `task-*` or `fix-*` branch created from the latest `origin/main`; the user-authorized Sentry integration is the one-time exact branch-name exception `integration-sentry`; direct pushes to `main`, stacked PRs, reused worktrees, and auto-merge are forbidden. The PR opens as Draft only after explicit current user publication authorization, becomes Ready only after a separate current user authorization plus green required CI and complete Codex review, and squash-merges only after explicit user authorization. After merge, confirm `main` and the linked issue, preserve the remote source branch and verify automatic head-branch deletion remains disabled, remove the local worktree only after checking that it has no tracked or untracked work to preserve, and run `git fetch --prune`. The next task waits for its predecessor to reach `main`:

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
  -> merged frontend prerequisite
  -> task-static-jar-integration
```

The skeleton merge triggers only the normal `push` CI path. It does not assign Jules. `task-ci-backend-gates` starts only when `JULES_ALLOWED_ACTOR` authors a sanitized Issue and that same allowed account applies exactly one label, `jules-action`. Never add both `jules` and `jules-action`. `jules-ci-failure.yml` is only the eligible failed-push repair path; `pr-event-relay.yml` is disabled by default and never assigns Jules.

## Requirement-to-plan traceability
Historical: rows about PostgreSQL, the outbox worker, retention, OTLP, and Sentry describe removed code.


| Approved requirement | Product task | Executable plan |
| --- | --- | --- |
| executable architecture, OpenAPI 3.1 contract, operations runbook, five exact implementation plans, and full requirement traceability | `task-backend-contract-plans` | this document, `openapi.yaml`, `operations.md`, and all five plan files |
| one Maven module, wrapper, Java 25, Boot 4.1.0, feature packages, profiles, exact no-store health filter/tests, smoke tests, JaCoCo | `task-backend-skeleton` | `2026-07-18-backend-foundation.md` Task 1 |
| Temurin 25 CI, verify, 100% line and branch coverage, Testcontainers runtime, required GitHub Dependency Review for newly introduced `high` and `critical` vulnerabilities across runtime, development, and unknown scopes, continuous Dependabot alerts/security updates backed by full Maven dependency submission from trusted `main`, informational Snyk, Java security gate, sanitized owner Issue trigger | `task-ci-backend-gates` and `task-dependency-security-github-native` | backend foundation Task 2 plus the GitHub-native replacement task |
| multi-stage Java 25 image, non-root runtime, liveness healthcheck, Docker-context environment/credential/key exclusions, smoke test, no secrets/deploy mutation | `task-backend-deploy-stub` | backend foundation Task 3 |
| stateless security, allowlist, no auth endpoints, RFC 9457, 16 KiB JSON boundary, same-origin, bounded rate limits, proxy fallback, closed actuator | `task-backend-http-security` | `2026-07-18-lead-intake.md` Task 1 |
| PostgreSQL 18, separate constrained lead/outbox tables, unique request ID, indexes, leases, privacy timestamps, Flyway/Testcontainers | `task-db-flyway-baseline` | lead intake Task 2 |
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
- Next.js 16.2.11 static export guide: `output: 'export'`, `next build`, and the default `out/` artifact.
- PostgreSQL 18 documentation: transactions, row-level locks, constraints, partial indexes, and `FOR UPDATE SKIP LOCKED`.
- OpenAPI 3.1.1 and RFC 9457 for the HTTP contract and problem details.
- Testcontainers for Java 2.0.5 for the `testcontainers-postgresql` and JUnit Jupiter integration.
