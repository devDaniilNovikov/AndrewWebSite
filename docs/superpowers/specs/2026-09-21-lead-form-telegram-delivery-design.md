# Lead form → Telegram without a database

Status: approved by the owner on 2026-09-21 (backend, container, frontend sections). First target is a local test
stand; merging to `main` and the Timeweb rollout are a separate decision.

## Why

Production (Timeweb App Platform, restholod17.ru) will not get PostgreSQL. The backend required a DataSource, OTLP and
Sentry, so the jar never started and `POST /api/leads` was dead. The image also served the frontend in preview mode
(form disabled, pre-publication banner, `noindex`) and proxied `/api/` onto its own port.

Owner decisions: no database, no Sentry, no OTLP; a Telegram failure is reported to the visitor honestly (synchronous
delivery); minimal footprint.

## Backend: synchronous delivery

`LeadController` → `LeadAcceptanceService` (honeypot, validation, `LeadNormalizer`, `LeadFingerprintService` unchanged)
→ `LeadDelivery` (replaces `LeadAcceptanceTransaction`, same signature) → `TelegramLeadDelivery`:

1. `LeadIdempotencyRegistry` — in memory, `requestId → fingerprint`, TTL 1 h, at most 10 000 entries, no PII.
   Same fingerprint → `DUPLICATE` (202, nothing is sent again). Different fingerprint → 409 as before.
2. Otherwise `TelegramGateway.send(message)`. `Delivered` → remember the request → `CREATED` (202). Any failure →
   `LeadDeliveryUnavailableException` → 503 `urn:andrew:problem:service-unavailable`, metric
   `rejected{reason=unavailable}` and one PII-free ERROR line `Telegram delivery failed: <code>`.
3. Concurrent requests with the same `requestId` are not serialized; the form never submits twice in parallel.

Telegram HTTP timeouts: connect 3 s + read 8 s, below the form's 15 s budget. `AcceptanceOutcome.RETAINED`,
`TelegramLeadMessage.leadId` and the gateway's `latestStart` deadline disappear with the outbox.

Removed: outbox, worker, retry policy, delivery window, retention/anonymization (`privacy`), Flyway migrations, JDBC,
database and worker readiness indicators, Sentry, OTLP export. The production logging guard keeps its ECS/`root: OFF`
rules without the OTLP part.

## Container

One container, two processes, uid 10001, supervised by `deploy/entrypoint.sh` (if either process exits, the container
exits and the platform restarts it).

- nginx is the only public listener on 8080: static site, `location = /api/leads` → Java on `127.0.0.1:8090` without
  stripping the prefix, every other `/api/` and `/actuator/` path → 404, `access_log off`, body limit 32 KiB.
- The client address is the last `X-Forwarded-For` element added by the platform proxy; nginx forwards it as
  `X-Real-IP`, and Tomcat trusts `X-Real-IP` only from loopback. Rate limits apply per visitor again.
- `ProductionHttpInvariantGuard` requires exactly this layout in `prod`; management stays on `127.0.0.1:8081` and the
  image HEALTHCHECK probes liveness there.

## Frontend

The image builds `build:standalone:production`: the form posts to `/api/leads`, the preview banner and the placeholder
form badge are hidden, robots become `index, follow`. The production content gate now passes because every
production-readiness item is verified; its self-test uses a temporary manifest instead of expecting a failure.

## Runtime configuration

`SPRING_PROFILES_ACTIVE=prod`, `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`, `LEAD_FINGERPRINT_HMAC_KEY` (at least 32
characters). The bot must be able to write to the chat (`/start` in a private chat, or membership in a group).
