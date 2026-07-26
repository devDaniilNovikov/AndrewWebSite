# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Added a scheduled Telegram outbox worker with bounded PostgreSQL lease
  recovery, deterministic `FOR UPDATE SKIP LOCKED` claims, per-row lease
  tokens, privacy revalidation, capped retries, and bounded Micrometer
  telemetry ([PR #52](https://github.com/devDaniilNovikov/AndrewWorkWebSite/pull/52)).

### Security

- Added fail-closed startup validation for production runtime profiles,
  fingerprint-key configuration, HTTP server bindings, forwarded headers,
  rate limiting, error responses, and Actuator exposure.
- Denied direct access to the application error endpoint while preserving
  redacted internal servlet error dispatches.
- Pinned public health endpoints to minimal, non-cached responses and rejected
  unsafe management-port, health-group, and probe overrides.
- Replaced the runtime container base with the Alpine Temurin JRE while
  preserving non-root execution and the liveness healthcheck.

### Testing

- Added early-startup guard tests, embedded-server HTTP boundary regressions,
  container contract checks, and production PostgreSQL/Flyway smoke coverage.
- Added PostgreSQL 18 worker coverage for concurrent claims, lease recovery,
  stale-token rejection, privacy boundaries, durable delivery outcomes, and
  the accepted at-least-once duplicate window.

### References

- [PR #42: Enforce production HTTP invariants](https://github.com/devDaniilNovikov/AndrewWebSite/pull/42)
