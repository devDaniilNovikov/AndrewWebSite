# Backend operations contract

## Purpose

This runbook defines the safe configuration, startup, telemetry, incident, deployment, and release boundaries for the AndrewWebSite backend. It contains binding names but no credential, domain, phone number, legal text, or production infrastructure value. The design is in [`architecture.md`](architecture.md); the database-free decision is in [`docs/superpowers/specs/2026-09-21-lead-form-telegram-delivery-design.md`](../superpowers/specs/2026-09-21-lead-form-telegram-delivery-design.md).

## Runtime profiles

Every process activates exactly one profile explicitly; a startup guard rejects none, several, or an unknown profile.

| Profile | Dependencies | Network behavior | Secret policy |
| --- | --- | --- | --- |
| `test` | fake or non-routable Telegram endpoint | no production outbound calls | the only profile with a committed, visibly non-production HMAC key |
| `local` | loopback fake Telegram endpoint | explicit loopback development CORS origins only | local untracked environment; never a tracked `.env` |
| `prod` | Telegram Bot API | application on `127.0.0.1:8090` behind nginx on `8080`; management on `127.0.0.1:8081`; Telegram outbound HTTPS | platform runtime bindings only; startup fails when one is absent or invalid |

## Configuration bindings

Environment variables are names, not storage. Values are entered only in the platform's variable settings (or, for a local stand, in an untracked file outside the repository). Dockerfiles, image `ENV` instructions, tracked files, issues, PRs, CI logs, and chat must not contain them.

| Environment binding | Spring property | Sensitivity | Validation |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `spring.profiles.active` | operational | exactly one of `test|local|prod`; the image also passes `prod` |
| `LEAD_FINGERPRINT_HMAC_KEY` | `app.leads.fingerprint-key` | secret | required in `local` and `prod`; at least 32 UTF-8 bytes (for example `openssl rand -hex 32`); no default |
| `TELEGRAM_BOT_TOKEN` | `app.telegram.bot-token` | secret | required in `local` and `prod`; never included in a logged URI |
| `TELEGRAM_CHAT_ID` | `app.telegram.chat-id` | sensitive | required in `local` and `prod`; the bot must be able to write there (`/start` in a private chat, or membership in a group) |
| `TELEGRAM_BASE_URL` | `app.telegram.base-url` | operational | `local`: required, explicit loopback host and port. `prod`: optional, defaults to `https://api.telegram.org`; when the host network blocks Telegram, set it to the bare HTTPS origin of the operator relay (no port, path, trailing slash, query, or credentials; see [Telegram relay](#telegram-relay)) |
| `LOCAL_CORS_ORIGINS` | `app.web.local-cors-origins` | operational | `local` only; absent in `prod` |

No other binding is read. Former `SPRING_DATASOURCE_*`, `OTLP_*`, and `SENTRY_DSN` variables have no effect and should be removed from the platform.

Fixed application defaults: lead body 16 KiB (nginx allows 32 KiB); source path 2048 characters; public perimeter 10 000 requests per rolling minute; lead global limit 60 per rolling minute; per-visitor burst 5, refilling one per minute; Telegram connect 3 s, read 8 s, no redirects; idempotency memory one hour, 10 000 entries.

## Container layout

| Port | Listener | Reachable from |
| --- | --- | --- |
| `8080` | nginx: static site, `= /api/leads` proxy, `404` for other `/api/` and `/actuator/` paths | the platform proxy (public) |
| `127.0.0.1:8090` | Spring Boot application connector | nginx only |
| `127.0.0.1:8081` | management: `/actuator/health/liveness`, `/actuator/health/readiness` | the container `HEALTHCHECK` only |

nginx forwards the visitor address as `X-Real-IP` (last `X-Forwarded-For` element, or the TCP peer when that header is absent) and strips every other forwarded header. Both processes run as uid `10001`; `deploy/entrypoint.sh` stops the container when either exits.

## Startup failure conditions

The application exits before accepting traffic when:

- the active-profile set is not exactly one allowed profile;
- the HMAC key is missing or shorter than 32 bytes, or the Telegram token or chat ID is missing or blank;
- the production Telegram base URL is not a bare HTTPS origin;
- the production HTTP layout differs from the table above, forwarded-header trust is anything but `X-Real-IP` from `127.0.0.1`, local CORS origins are set, the rate limiter is disabled, or an actuator endpoint other than health is exposed;
- production logging is not ECS with `root: OFF`, or any logger other than the two allowed ERROR loggers is enabled.

A production startup failure prints one ECS line `Application startup failed` without cause details; reproduce locally with the same variables to diagnose.

## Telegram operations

A lead is sent while the visitor waits. `202` means Telegram accepted the message. Any failure returns `503` to the form (which invites a retry) and logs one ERROR line `Telegram delivery failed: <code>`:

| Code | Meaning | Action |
| --- | --- | --- |
| `telegram_permanent_401` | token rejected | replace `TELEGRAM_BOT_TOKEN` in the platform, redeploy |
| `telegram_permanent_400`, `telegram_permanent_403` | chat not found, or the bot may not write there | fix `TELEGRAM_CHAT_ID`; send `/start` to the bot or add it to the group |
| `telegram_429` | Telegram throttling | transient; retries succeed after the throttle window |
| `telegram_5xx`, `network`, `telegram_unexpected` | Telegram or network unavailable | usually transient; a `network` failure on every lead after about 3 s means the host network blocks `api.telegram.org`, so route through the [Telegram relay](#telegram-relay) |

Messages are plain text containing the lead's personal data, so auto-delete of no more than 30 days in the destination chat is a production gate. Telegram bodies are never logged. Never paste a token, chat ID, full Telegram URL, or lead message into a task, issue, log, or chat.

## Telegram relay

The Timeweb `MSK-1` zone cannot open TCP connections to `api.telegram.org` (confirmed on 2026-09-23: every lead failed with `network` after the 3 s connect timeout). The site stays in Moscow and reaches Telegram through a relay the operator controls: a small server abroad with Caddy. Caddy terminates HTTPS with a public certificate and forwards only this bot's `sendMessage` calls to `https://api.telegram.org`. Every other path returns `404`.

- The relay sees the bot token and each lead message. It must be the operator's own server, with Caddy access logs disabled.
- Set `TELEGRAM_BASE_URL` to the relay origin, for example `https://relay.example`, then redeploy.
- Remove the variable to go back to direct delivery once the platform can reach Telegram again.

## Health and diagnostics

Liveness and readiness are dependency-free (Telegram is not probed) and return only `status` with `Cache-Control: no-store`. Allowed diagnostics: deployment SHA, container state, health status, and the bounded ERROR codes above. Forbidden: request/response bodies, names, phones, comments, request IDs, fingerprints, Telegram messages or URLs, tokens, chat IDs, environment dumps, heap dumps.

## Local test stand

1. Send `/start` to the bot from the account whose ID is `TELEGRAM_CHAT_ID`.
2. Create a file outside the repository, for example `~/andrew-stand.env`, with `SPRING_PROFILES_ACTIVE=prod`, `TELEGRAM_BOT_TOKEN=…`, `TELEGRAM_CHAT_ID=…`, and `LEAD_FINGERPRINT_HMAC_KEY=…`.
3. `docker build -t andrew-website:stand .`
4. `docker run -d --name andrew-stand -p 18080:8080 --env-file ~/andrew-stand.env andrew-website:stand`
5. Open `http://localhost:18080`, submit the form, and confirm the message in Telegram; `docker logs andrew-stand` must show no ERROR line.
6. `docker rm -f andrew-stand` when done.

## Deployment and rollback boundary

Every product task follows the [canonical Git Flow](../../.agents/workflows/GIT_FLOW.md): one worktree and one `task-*`/`fix-*` branch from the latest `origin/main`, one Draft PR, required CI green, squash merge only with explicit user authorization. The platform app must build with the repository `Dockerfile`. A deploy is immutable: build the reviewed commit, set the four runtime bindings, wait for the healthcheck, and run the smoke tests below. Rollback selects a previously verified image; there is no schema or stored lead data to reconcile.

## Verification and release gates

- `./mvnw -B verify` passes with 100% JaCoCo line and branch coverage; the CI `container-build` job verifies uid `10001`, no Node runtime, and the loopback liveness healthcheck.
- OpenAPI behaviors for `202`, `400`, `409`, `413`, `415`, `429`, and `503` hold.
- Smoke: home page `200` without the pre-publication banner and with `index, follow`; `/api/x` and `/actuator/health/liveness` `404` from outside; a fictional lead arrives in the destination chat; a second submission of the same attempt sends nothing new; invalid input returns `400`.
- Destination chat auto-delete is no more than 30 days.
- The user explicitly authorizes merge and production release.

## Jules CI-task boundary

Merging `task-backend-skeleton` causes only the normal `push` CI run. It does not create `task-ci-backend-gates`. That planned Jules task begins only when `JULES_ALLOWED_ACTOR` creates an owner-authored sanitized Issue and the same allowed account applies exactly one label, `jules-action`. Requirements from another account are copied into a new verified owner Issue; untrusted issue text is not labeled. Never combine `jules` and `jules-action`. `jules-ci-failure.yml` remains limited to eligible failed trusted pushes, and `pr-event-relay.yml` remains disabled by default and does not assign Jules.
