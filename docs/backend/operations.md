# Backend operations contract

## Purpose

This runbook defines the safe configuration, startup, migration, recovery, telemetry, incident, deployment, and release boundaries for the AndrewWebSite backend. It contains binding names but no credential, domain, phone number, legal text, or production infrastructure value.

## Runtime profiles

The application has a safe common configuration plus exactly three explicit profiles.

| Profile | Dependencies | Network behavior | Secret policy |
| --- | --- | --- | --- |
| `test` | PostgreSQL 17 Testcontainers and an in-process/fake Telegram server | no production outbound calls; scheduling disabled unless a focused test enables it | the only profile allowed to use a committed fixed non-production HMAC test key; no realistic token-shaped test data |
| `local` | developer-supplied PostgreSQL 17 and fake Telegram endpoint by default | only explicitly configured loopback development CORS origins; forwarded client headers ignored | local untracked environment or approved developer secret store; never a tracked `.env` |
| `prod` | Timeweb managed PostgreSQL 17, Telegram Bot API, and approved Grafana Cloud OTLP endpoint | same-origin public HTTP; Telegram and OTLP outbound HTTPS; forwarded client headers ignored until CIDR verification gate | orchestration-injected secret-store values only; fail fast when required bindings are absent or invalid |

No implicit fallback activates `local` or `test` behavior in production. Production deployment sets the `prod` profile explicitly. H2 and another database dialect are not used because migration, locking, idempotency, and queue tests require PostgreSQL semantics.

## Configuration bindings

Environment variables are names, not storage. The platform secret store injects sensitive values at process start; Dockerfiles, image `ENV` instructions, Maven resources, GitHub Issues, Actions logs, repository variables, command histories, and tracked files must not contain them.

| Environment binding | Spring property | Sensitivity | Validation |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `spring.profiles.active` | operational | production must resolve to `prod` only |
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | secret-adjacent | required in `local` and `prod`; PostgreSQL JDBC scheme only |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` | sensitive | required in `local` and `prod` |
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` | secret | required in `local` and `prod` |
| `LEAD_FINGERPRINT_HMAC_KEY` | `app.leads.fingerprint-key` | secret | required in `prod`; UTF-8 representation at least 32 bytes; no default |
| `TELEGRAM_BOT_TOKEN` | `app.telegram.bot-token` | secret | required in `prod`; no default; never included in a logged URI |
| `TELEGRAM_CHAT_ID` | `app.telegram.chat-id` | sensitive | required in `prod`; validated non-blank; no default |
| `OTLP_METRICS_URL` | `management.otlp.metrics.export.url` | operational | required only when production OTLP export is enabled; HTTPS |
| `OTLP_AUTHORIZATION` | `management.otlp.metrics.export.headers.Authorization` | secret | required only when the collector requires it; no default |
| `LOCAL_CORS_ORIGINS` | `app.web.local-cors-origins` | operational | `local` only; explicit loopback HTTP origins; absent in `prod` |

Non-secret application defaults are fixed in versioned configuration: lead request body 16 KiB; source path 2048 characters; global limit 60/minute; per-connection burst 5/refill 1 per minute; bounded client bucket capacity; worker poll 15 seconds; claim batch 10; lease two minutes; retry 30 seconds through six hours; worker heartbeat stale after 45 seconds; retention run hourly; anonymization at 29 days; hard PII limit 30 days; retention heartbeat stale after two hours; and anonymized-row deletion after 12 months.

`app.leads.fingerprint-key`, `app.telegram.bot-token`, `app.telegram.chat-id`, and OTLP authorization are declared as secret configuration. Configuration `toString`, failure analysis, actuator, and debug logging must redact their values. No secret property is exposed through a public endpoint.

## Startup and release failure conditions

The application process must fail startup before accepting traffic when any applicable condition is true:

- the active profile is missing, contradictory, or permits `test` configuration in production;
- required PostgreSQL URL, user, or password is missing, malformed, or not PostgreSQL;
- Flyway validation or migration fails;
- production HMAC material is missing, cannot be decoded, or is shorter than 32 bytes;
- production Telegram token or chat ID is missing or blank;
- an unsafe public actuator exposure is configured;
- production enables local CORS origins or forwarded-header trust without the verified proxy configuration;
- production enables OTLP but its HTTPS endpoint or required authorization binding is absent.

The deployment/release gate must fail even if the process could technically start when any of these is unverified:

- PostgreSQL is version 17 and resides in the intended Moscow region/VPC;
- PostgreSQL backup retention is no more than 30 days;
- Telegram destination auto-delete is no more than 30 days;
- the Timeweb proxy behavior remains unverified while forwarded-header trust is enabled;
- OTLP telemetry has not been proven PII-free and deliverable;
- the final static frontend prerequisite or required smoke checks are missing.

Release-gate facts are operator attestations or platform checks; they are not guessed by application code.

## Database migration and connection operations

Flyway owns schema history. `V1__lead_outbox_baseline.sql` creates `leads`, `telegram_outbox`, constraints, and indexes before lead traffic is enabled. Migrations are forward-only, reviewed, transactional where PostgreSQL permits, and tested against PostgreSQL 17 with Testcontainers. Hibernate/JPA schema generation is not used.

Before deployment:

1. verify a recoverable backup exists and its retention policy remains at most 30 days;
2. run the complete migration/constraint tests against PostgreSQL 17;
3. review SQL lock duration, index construction, and backward compatibility with the currently running JAR;
4. deploy one schema-compatible JAR and wait for Flyway plus readiness;
5. run the API, queue, privacy, and health smoke matrix.

Never edit Flyway history, apply ad hoc DDL from an application container, or use a destructive down migration as routine rollback. A schema change that cannot support both old and new JARs must use an expand/migrate/contract sequence in separate merged tasks.

The pool is bounded. Connection validation and readiness queries have bounded timeouts. Database exceptions returned to clients become generic `503` problem details and do not expose the JDBC URL, SQL, constraint text, credentials, lead data, or stack traces.

## Backup, restore, and privacy recovery

Managed PostgreSQL backup retention must not exceed 30 days. A restore is performed into an isolated recovery database first. Before restored data serves traffic:

1. identify the backup timestamp without dumping table content;
2. run the current Flyway migration set;
3. keep lead intake and Telegram scheduling disabled;
4. run retention immediately using current UTC time so every lead at or beyond 29 days is anonymized and every undelivered associated outbox row is blocked as `privacy_expired`;
5. verify by aggregate SQL that no row at or beyond the 30-day hard limit retains `name`, `phone`, `comment`, or `payload_fingerprint`;
6. enable the outbox worker only after that aggregate verification;
7. switch traffic through the normal deploy gate.

Do not export restored lead rows for diagnosis. If the backup provider cannot enforce the 30-day maximum, production release remains blocked.

## Worker operations

The outbox worker polls every 15 seconds and claims at most 10 due records per transaction. Its claim query orders by `next_attempt_at, id`, excludes leads at or beyond the 29-day privacy threshold, uses `FOR UPDATE SKIP LOCKED`, updates state/attempt/lease in the same transaction, and commits before Telegram HTTP. A two-minute lease and random lease token prevent stale completion writes. Expired leases recover to due `retry`.

Operator-visible aggregate signals are queue depth by bounded state, oldest eligible queue age, successful-poll age, delivery outcomes, retry outcomes, and privacy blocks. They never include request IDs, source paths, names, phone numbers, comments, fingerprints, Telegram bodies, exception messages, or arbitrary tags.

When the queue grows:

1. check readiness and PostgreSQL connectivity using minimal health plus platform connection signals;
2. check worker heartbeat age and aggregate state counts;
3. distinguish database contention, Telegram throttling, permanent Telegram configuration failure, and process scheduling failure using bounded outcome codes;
4. keep privacy retention enabled throughout diagnosis;
5. allow the retry/lease state machine to recover; do not bulk-copy PII or bypass leases;
6. after configuration repair, deploy normally and confirm queue age decreases while privacy-block counts remain explainable.

`blocked` is terminal in MVP. There is no public or automated replay endpoint. Any proposed manual requeue is a separately reviewed, privacy-aware operational change; entries with `privacy_expired` must never be requeued.

## Telegram operations

The gateway uses the fixed Telegram Bot API host and a Boot-managed `RestClient.Builder`; a request cannot supply a URL, bot token, or chat ID. The bot token must not appear in a loggable full URL. Telegram 429 `parameters.retry_after` is parsed as integer seconds, validated, and combined with exponential backoff under the six-hour cap. Network errors, timeouts, and 5xx retry; non-429 4xx block with a bounded technical status code.

The recipient message contains necessary lead PII plus source, intent, UTC creation time, and `requestId`. Telegram destination auto-delete of no more than 30 days is therefore a mandatory production gate. Telegram request/response bodies are never logged. Delivery is at least once; the recipient uses `requestId` to recognize a possible duplicate after a post-send database crash.

For 401/403 or sustained permanent failures, rotate or repair the credential only in the approved secret store, deploy/restart through the normal process, and verify with a fictional non-PII canary. Never paste a token, chat ID, full Telegram URL, response body, or lead message into a task, Issue, log, or chat.

## Retention operations

The privacy worker runs hourly in bounded batches with a supplied `Clock`. A single transaction first blocks every undelivered outbox row whose lead is at least 29 days old, including leased work, then clears lead name, phone, comment, and HMAC fingerprint and records `anonymized_at`. Rows anonymized for 12 months are deleted with their outbox rows. A successful complete pass advances the retention heartbeat; failures do not.

Alert before the hard boundary: retention heartbeat older than two hours is urgent, any PII-bearing row older than 29 days is critical, and any PII-bearing row at 30 days is a privacy incident. The outbox claim query repeats the `< 29 days` predicate, and the gateway reloads the lead using its lease token immediately before send, so retention wins races and stale claims cannot send expired PII.

Privacy incident response:

1. stop lead intake and Telegram worker scheduling without disabling retention;
2. preserve PII-free audit metadata and deployment identifiers; do not copy affected content into logs;
3. run the retention job and the aggregate hard-limit query;
4. block affected undelivered rows with `privacy_expired` and clear active leases;
5. verify Telegram and database backup retention gates;
6. follow the approved legal/security notification process outside this repository;
7. add a regression test for the failure before re-enabling traffic.

## Health, telemetry, and alert intent

Liveness is dependency-free. Readiness is `UP` only when PostgreSQL is available and a successful outbox poll occurred within 45 seconds after a 45-second startup grace. Both public responses contain only `status`; health details are never public. Telegram and OTLP outages do not fail liveness or readiness because accepted leads remain durable.

Actuator and Micrometer exist from foundation, but production OTLP export is introduced only in `task-backend-observability`. Public exposure includes only health routing needed for liveness/readiness. There is no public `/actuator/metrics` or Prometheus endpoint and no self-hosted Prometheus/Grafana deployment.

Alert intent:

| Signal | Intent |
| --- | --- |
| readiness down | database unavailable or worker poll stale; page after bounded confirmation |
| worker last-success age >45 seconds | investigate scheduling/database while retention continues |
| oldest eligible queue age increasing | investigate Telegram throttling/permanent failures and database contention |
| delivery blocked count increases | inspect bounded status class; never inspect raw messages in telemetry |
| retention last-success age >2 hours | urgent privacy response |
| PII-bearing rows reach 29 days | critical pre-limit remediation |
| any PII-bearing row reaches 30 days | privacy incident |
| OTLP export failure | use local bounded platform signals; do not expose raw metrics publicly |

Cardinality is bounded to documented enums/status classes. Log levels cannot be changed to reveal bodies, SQL parameter values, Spring environment contents, or HTTP authorization data.

## Safe diagnostics

Allowed diagnostics are deployment SHA, process start time, minimal liveness/readiness, aggregate queue counts by state, bounded heartbeat ages, Flyway version/checksum state, database availability without URL, and bounded outcome counters.

Forbidden diagnostics include raw request/response bodies, rejected values, name, phone, comment, request ID tags, canonical JSON, HMAC/fingerprint, Telegram message/body/full URL, bot token, chat ID, JDBC URL, database username/password, OTLP headers, full environment/configuration dumps, heap dumps, SQL with bound parameters, and public actuator detail. Shell history and command output must not expand secret environment variables.

## Deployment and rollback boundary

Every product task produces one reviewable PR from fresh `origin/main`; no stacked PRs and no automatic merges. CI, review, and explicit user merge approval precede the next task. Production mutation is never part of a documentation or deploy-stub task.

A deploy is immutable: build from a reviewed commit, verify the JAR/container, apply compatible Flyway migrations, start a non-root Java 25 container, wait for readiness, and run smoke tests. The final image contains no Node runtime or build secret.

Application rollback selects a previously verified image only when its code is compatible with the current schema. Flyway history is not rolled back. If privacy correctness is in doubt, keep intake and delivery stopped while retention and aggregate checks run. A failed static integration may roll back the JAR without reverting durable lead/outbox rows. A failed credential rotation restores only through the secret store; never through an image or tracked configuration.

## Verification and release gates

For each implementation PR, run only commands introduced by its committed manifests. Once the skeleton owns the wrapper, the backend gate is `./mvnw -B verify`; JaCoCo must fail `verify` below 80%. Database changes additionally run PostgreSQL 17 Testcontainers tests. Container tasks build and smoke-test the exact image. The final integration verifies home page, hashed asset, real 404, lead API, liveness, and readiness.

Before production release, confirm all of the following with fresh evidence:

- Java 25, Spring Boot 4.1.0, Maven Wrapper, one root module, and one final Java container;
- complete unit, integration, MockMvc, concurrency, migration, privacy, telemetry, dependency/security, and container gates with at least 80% JaCoCo coverage;
- exact OpenAPI behaviors for `202`, `400`, `409`, `413`, `415`, `429`, and `503`;
- first acceptance commits lead and outbox atomically; rollback never returns `202`;
- equal duplicates, conflicting payloads, synthetic acceptance, and post-fingerprint replay match the contract;
- two workers cannot claim one row concurrently; expired leases and restarts recover; HTTP occurs outside claim transactions; Telegram 429 uses seconds;
- no PII survives the 30-day hard limit and no privacy-expired claim can send;
- logs, metrics, problems, health, images, and test fixtures contain no secret or accidental PII;
- PostgreSQL backup retention and Telegram auto-delete are each no more than 30 days;
- forwarded headers remain untrusted until Timeweb CIDRs are verified;
- OTLP is private and functioning; raw metrics and sensitive actuator endpoints are not public;
- static frontend prerequisites are merged and final cache/routing/smoke behavior passes;
- the user explicitly authorizes merge and production release.

## Jules CI-task boundary

Merging `task-backend-skeleton` causes only the normal `push` CI run. It does not create `task-ci-backend-gates`. That planned Jules task begins only when `JULES_ALLOWED_ACTOR` creates an owner-authored sanitized Issue and the same allowed account applies exactly one label, `jules-action`. Requirements from another account are copied into a new verified owner Issue; untrusted issue text is not labeled. Never combine `jules` and `jules-action`. `jules-ci-failure.yml` remains limited to eligible failed trusted pushes, and `pr-event-relay.yml` remains disabled by default and does not assign Jules.
