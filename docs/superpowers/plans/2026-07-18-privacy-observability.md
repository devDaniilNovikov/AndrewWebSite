# Privacy and Observability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enforce the 30-day PII hard limit and add minimal health, redacted structured telemetry, and private OTLP export.

**Architecture:** An hourly clock-driven retention worker atomically blocks undelivered queue entries and anonymizes their leads at 29 days, then deletes anonymized technical rows after 12 months. Observability composes dependency-free liveness with database/worker readiness, exports only bounded PII-free meters over OTLP, and never exposes raw metrics or health detail publicly.

**Tech Stack:** Java 25, Spring Boot 4.1.0 Actuator, JdbcClient, PostgreSQL 18, Spring Scheduling, Micrometer OTLP, JUnit Jupiter, MockMvc, Testcontainers 2.0.x

## Global Constraints

- One root Maven module; Java 25 LTS; Spring Boot 4.1.0; package `ru.andrew.website`; managed PostgreSQL 18. Frontend remains `frontend/` with Next.js 16.2.9, React 19.2.x, strict TypeScript, Tailwind CSS 4, Motion, and Node 24 only at build time.
- Public surface remains static content, JSON-only 16 KiB `POST /api/leads`, liveness, and minimal readiness; no login/session/form login/HTTP Basic or sensitive/raw actuator endpoint.
- Lead acceptance remains empty indistinguishable `202`; RFC 9457 `400/409/413/415/429/503`; name 2–100, phone input 32/normalized digits 7–15, optional comment 1000, local source path, exact `repair|maintenance`, consent true, HMAC only from `LEAD_FINGERPRINT_HMAC_KEY`.
- Bounded limits remain a rolling global maximum of 60 admissions in every `(t - 60 seconds, t]` interval and a separate per-connection burst 5/refill 1 token per minute; forwarded headers remain untrusted until Timeweb CIDRs are verified.
- Outbox states remain exactly `pending|processing|retry|blocked|delivered`; poll 15 seconds, batch 10, lease two minutes, deterministic `FOR UPDATE SKIP LOCKED`, HTTP outside claim transaction, retries 30 seconds through six hours, Telegram `retry_after` seconds, and accepted at-least-once duplicates.
- PII hard limit is 30 days from `leads.created_at`, including undelivered leads; operational anonymization threshold is exactly 29 days; anonymized technical rows delete after exactly 12 months.
- Anonymization clears `name`, `phone`, `comment`, and `payload_fingerprint`; every non-delivered outbox row becomes `blocked` with `last_error_code='privacy_expired'`, leases cleared, in the same transaction.
- The outbox claim and immediate pre-send reload exclude leads at or beyond 29 days. A stale worker's lease-token update cannot reverse privacy blocking.
- A retained request ID with null fingerprint always produces the same empty safe `202`, creates no new lead/outbox, and never returns 409 for changed payload.
- PostgreSQL backup retention and Telegram auto-delete must each be no more than 30 days before production release.
- Liveness is dependency-free. Readiness uses only PostgreSQL availability and an outbox-worker full-batch success within 45 seconds after a 45-second startup grace; reload/send/state-write exceptions and false lease-token updates do not advance that heartbeat; public bodies contain only `status`, and both paths retain exact `Cache-Control: no-store`.
- Retention runs hourly; its successful full-pass heartbeat is stale after two hours and alerts but does not change readiness.
- OTLP is introduced only in `task-backend-observability`; no self-hosted Prometheus/Grafana and no public `/actuator/metrics` or `/actuator/prometheus`.
- Logs/problems/metrics never include name, phone, comment, request/canonical body, source path tag, request ID tag, fingerprint, Telegram content/credentials, database URL/credentials, OTLP authorization, environment dump, SQL parameters, or unbounded exception text.
- Follow the [canonical Git Flow](../../../.agents/workflows/GIT_FLOW.md): each approved product task waits for its prerequisite on `main`, then uses one dedicated external worktree and one lowercase `task-*` or `fix-*` branch from the latest `origin/main`, followed by one Draft PR; direct pushes to `main`, stacked PRs, reused worktrees, non-squash merges, and auto-merge are forbidden. Mark Ready only after required CI is green and Codex review is complete; squash-merge only after explicit user authorization; then confirm `main`, close the issue, allow automatic remote-branch deletion, remove only a worktree with no tracked or untracked work to preserve, and run `git fetch --prune`. Preserve strict RED → GREEN → REFACTOR.
- Every AI-authored commit adds the executing agent's own `Co-Authored-By` attribution footer and never attributes a human identity.

---

### Task 1: `task-lead-retention` — atomic anonymization, privacy blocking, and deletion

**Files:**
- Create: `src/main/java/ru/andrew/website/privacy/RetentionProperties.java`
- Create: `src/main/java/ru/andrew/website/privacy/RetentionBatchResult.java`
- Create: `src/main/java/ru/andrew/website/privacy/RetentionRepository.java`
- Create: `src/main/java/ru/andrew/website/privacy/JdbcRetentionRepository.java`
- Create: `src/main/java/ru/andrew/website/privacy/RetentionService.java`
- Create: `src/main/java/ru/andrew/website/privacy/RetentionHeartbeat.java`
- Modify: `src/main/resources/application.yml`
- Create: `src/test/java/ru/andrew/website/privacy/LeadRetentionIntegrationTest.java`
- Create: `src/test/java/ru/andrew/website/privacy/RetentionBoundaryTest.java`
- Create: `src/test/java/ru/andrew/website/privacy/RetentionTestConfiguration.java`
- Modify: `src/test/java/ru/andrew/website/leads/LeadAcceptanceIntegrationTest.java`

**Interfaces:**
- Consumes: `Clock`, `JdbcClient`, `leads`, `telegram_outbox`, and retained-idempotency behavior.
- Produces: `RetentionRepository.expireBatch(Instant,int)`; `deleteBatch(Instant,int)`; `RetentionService.runOnce()`; last successful full-pass heartbeat; aggregate anonymized/deleted counters.

- [ ] **Step 1: RED — prove the 29-day action and 30-day invariant**

Use the existing PostgreSQL 18 service-connection configuration and mutable clock. Seed pending, retry, processing, delivered, and already anonymized examples with fictional content. Add these exact assertions:

```java
package ru.andrew.website.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import ru.andrew.website.testing.PostgresTestConfiguration;
import ru.andrew.website.testing.MutableClock;

@SpringBootTest
@ActiveProfiles("test")
@Import({PostgresTestConfiguration.class, RetentionTestConfiguration.class})
class RetentionBoundaryTest {
    @Autowired RetentionService retention;
    @Autowired JdbcClient jdbc;
    @Autowired MutableClock clock;

    @Test
    void thresholdClearsAllPiiAndBlocksUndeliveredInOneRun() {
        long leadId = insertLeadWithProcessingOutbox(Instant.parse("2026-01-01T00:00:00Z"));
        clock.setInstant(Instant.parse("2026-01-30T00:00:00Z"));
        retention.runOnce();
        var row = jdbc.sql("""
                select l.name,l.phone,l.comment,l.payload_fingerprint,l.anonymized_at,
                       o.state,o.lease_token,o.lease_until,o.last_error_code
                from leads l join telegram_outbox o on o.lead_id=l.id where l.id=:id
                """).param("id", leadId).query().singleRow();
        assertThat(row).containsEntry("state", "blocked")
                .containsEntry("last_error_code", "privacy_expired");
        assertThat(row.get("name")).isNull();
        assertThat(row.get("phone")).isNull();
        assertThat(row.get("comment")).isNull();
        assertThat(row.get("payload_fingerprint")).isNull();
        assertThat(row.get("lease_token")).isNull();
        assertThat(row.get("lease_until")).isNull();
    }

    private long insertLeadWithProcessingOutbox(Instant createdAt) {
        long leadId = jdbc.sql("""
                insert into leads(request_id,payload_fingerprint,name,phone,comment,source_path,intent,consented_at,created_at)
                values (:requestId,decode(repeat('00',32),'hex'),'Тест','79990000000','Проверка','/test/',
                        'repair',:createdAt,:createdAt) returning id
                """).param("requestId", java.util.UUID.randomUUID())
                .param("createdAt", createdAt).query(Long.class).single();
        jdbc.sql("""
                insert into telegram_outbox(lead_id,state,attempt_count,next_attempt_at,lease_token,lease_until,created_at,updated_at)
                values (:leadId,'processing',1,:createdAt,:leaseToken,:leaseUntil,:createdAt,:createdAt)
                """).param("leadId", leadId).param("createdAt", createdAt)
                .param("leaseToken", java.util.UUID.randomUUID())
                .param("leaseUntil", createdAt.plusSeconds(120)).update();
        return leadId;
    }
}
```

`RetentionTestConfiguration` is `@TestConfiguration(proxyBeanMethods = false)` and provides one `@Bean @Primary MutableClock retentionClock()` initialized to `2026-01-30T00:00:00Z` in UTC.

Tests also prove: one instant before 29 days remains; exactly 29 days anonymizes; delivered outbox remains delivered while lead is anonymized; at 30 days aggregate PII-bearing count is zero; a stale processing token cannot mark after privacy block; 12-month-minus-one-instant remains; exactly 12 months after `anonymized_at` deletes lead and cascaded outbox; repository failure rolls back block plus anonymization and does not advance heartbeat.

Modify the lead integration test: anonymize a retained lead, clear fingerprint through retention, replay its `requestId` with a changed payload, expect empty 202, and assert lead/outbox counts remain one.

Run: `./mvnw -B -Dtest=LeadRetentionIntegrationTest,RetentionBoundaryTest,LeadAcceptanceIntegrationTest test`

Expected: FAIL because retention types do not exist and the replay setup is not implemented.

- [ ] **Step 2: GREEN — implement configuration, repository, and complete-pass heartbeat**

```java
package ru.andrew.website.privacy;

import java.time.Duration;
import java.time.Period;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.privacy.retention")
public record RetentionProperties(
        Duration anonymizeAfter, Duration hardLimit, Period deleteAfter,
        Duration pollInterval, int batchSize) {
    public RetentionProperties {
        if (!Duration.ofDays(29).equals(anonymizeAfter)
                || !Duration.ofDays(30).equals(hardLimit)
                || !Period.ofMonths(12).equals(deleteAfter)
                || !Duration.ofHours(1).equals(pollInterval)
                || batchSize < 1 || batchSize > 1_000) {
            throw new IllegalArgumentException("retention settings violate the privacy contract");
        }
    }
}
```

```java
package ru.andrew.website.privacy;

public record RetentionBatchResult(int anonymized, int blocked) {
}
```

```java
package ru.andrew.website.privacy;

import java.time.Instant;

public interface RetentionRepository {
    RetentionBatchResult expireBatch(Instant cutoffInclusive, int limit);
    int deleteBatch(Instant anonymizedCutoffInclusive, int limit);
}
```

`JdbcRetentionRepository.expireBatch` is one `@Transactional` method. It selects at most `:limit` lead IDs where `anonymized_at is null and created_at <= :cutoff`, ordered `created_at,id`, `FOR UPDATE SKIP LOCKED`. It updates associated `pending|retry|processing` rows to `blocked`, clears lease/token, sets `privacy_expired`, then nulls all four PII/fingerprint columns and sets one `anonymized_at` instant. `deleteBatch` selects ordered eligible anonymized IDs with `FOR UPDATE SKIP LOCKED` and deletes them; foreign-key cascade removes outbox rows.

Use an array parameter or temporary CTE supported by `JdbcClient`; never construct an SQL `IN` list from text. The expiry SQL predicates repeat `anonymized_at is null` so concurrent passes are idempotent.

```java
package ru.andrew.website.privacy;

import java.time.Clock;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public final class RetentionService {
    private final RetentionRepository repository;
    private final RetentionProperties properties;
    private final RetentionHeartbeat heartbeat;
    private final Clock clock;

    public RetentionService(RetentionRepository repository, RetentionProperties properties,
            RetentionHeartbeat heartbeat, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.heartbeat = heartbeat;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.privacy.retention.poll-interval:1h}")
    public void runOnce() {
        Instant now = clock.instant();
        RetentionBatchResult result;
        do {
            result = repository.expireBatch(now.minus(properties.anonymizeAfter()), properties.batchSize());
        } while (result.anonymized() == properties.batchSize());
        Instant deleteCutoff = java.time.ZonedDateTime.ofInstant(now, java.time.ZoneOffset.UTC)
                .minus(properties.deleteAfter()).toInstant();
        int deleted;
        do {
            deleted = repository.deleteBatch(deleteCutoff, properties.batchSize());
        } while (deleted == properties.batchSize());
        heartbeat.success(now);
    }
}
```

Refactor the calendar-month cutoff into a private method after GREEN; keep the leap-day and month-boundary tests.

Run: `./mvnw -B -Dtest=LeadRetentionIntegrationTest,RetentionBoundaryTest,LeadAcceptanceIntegrationTest test`

Expected: PASS, including safe changed-payload replay after fingerprint removal.

- [ ] **Step 3: REFACTOR, verify the hard-limit query, and commit**

Add bounded Micrometer counters without PII tags and run:

```bash
./mvnw -B verify
```

Then run the aggregate assertion inside the integration test transaction:

```sql
select count(*)
from leads
where created_at <= :hardCutoff
  and (name is not null or phone is not null or comment is not null or payload_fingerprint is not null)
```

Expected: Maven PASS with at least 80% coverage and aggregate count `0` for `hardCutoff = now - 30 days`.

```bash
git add src
git commit -m "feat(lead-retention): enforce lead privacy lifecycle"
```

### Task 2: `task-backend-observability` — health composition, redaction, bounded metrics, OTLP

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-prod.yml`
- Modify: `src/test/resources/application-test.yml`
- Create: `src/main/java/ru/andrew/website/observability/DatabaseReadinessHealthIndicator.java`
- Create: `src/main/java/ru/andrew/website/observability/WorkerReadinessHealthIndicator.java`
- Create: `src/main/java/ru/andrew/website/observability/TelemetryConfiguration.java`
- Create: `src/test/java/ru/andrew/website/observability/HealthContractIntegrationTest.java`
- Create: `src/test/java/ru/andrew/website/observability/TelemetryPrivacyTest.java`

**Interfaces:**
- Consumes: `JdbcClient`, `WorkerHeartbeat`, `RetentionHeartbeat`, existing Micrometer counters/gauges, and production OTLP secret bindings.
- Produces: `dbReadiness` and `telegramWorkerReadiness` health contributors; minimal readiness group; PII-free structured console events; private OTLP meter export.

- [ ] **Step 1: RED — specify degraded health and telemetry privacy**

```java
package ru.andrew.website.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.andrew.website.testing.PostgresTestConfiguration;
import ru.andrew.website.telegram.WorkerHeartbeat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class HealthContractIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired WorkerHeartbeat workerHeartbeat;
    @Autowired JdbcClient jdbc;
    @Autowired Clock clock;

    @BeforeEach
    void postgresFixtureIsAvailableAndMigrated() {
        assertThat(jdbc.sql("select 1").query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void staleWorkerMakesOnlyReadinessDown() throws Exception {
        workerHeartbeat.success(clock.instant().minusSeconds(46));
        mvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(content().json("{\"status\":\"DOWN\"}", true));
    }

    @Test
    void migratedPostgresAndFreshWorkerMakeReadinessUpWithoutDetails() throws Exception {
        workerHeartbeat.success(clock.instant());
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(content().json("{\"status\":\"UP\"}", true))
                .andExpect(jsonPath("$.components").doesNotExist());
    }
}
```

The imported, Spring-owned `PostgresTestConfiguration` is the established PostgreSQL 18 Testcontainers fixture from the database plan; Flyway completes against it before these methods execute. Create focused test methods for PostgreSQL down while liveness remains UP, retention staleness not changing readiness, `/actuator/metrics|prometheus|env|configprops|heapdump|shutdown` forbidden, meter tag allowlist, finite queue gauges, and captured structured logs lacking fictional name/phone/comment/request ID/token/chat/database URL/OTLP authorization.

Run: `./mvnw -B -Dtest=HealthContractIntegrationTest,TelemetryPrivacyTest test`

Expected: FAIL because readiness contributors and telemetry configuration do not exist.

- [ ] **Step 2: GREEN — compose exact health groups**

Spring Boot 4 health contributor imports are `org.springframework.boot.health.contributor.Health` and `org.springframework.boot.health.contributor.HealthIndicator`.

```java
package ru.andrew.website.observability;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component("dbReadiness")
public final class DatabaseReadinessHealthIndicator implements HealthIndicator {
    private final JdbcClient jdbc;

    public DatabaseReadinessHealthIndicator(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Health health() {
        try {
            return jdbc.sql("select 1").query(Integer.class).single() == 1
                    ? Health.up().build() : Health.down().build();
        } catch (RuntimeException exception) {
            return Health.down().build();
        }
    }
}
```

`WorkerReadinessHealthIndicator` is named `telegramWorkerReadiness`, uses `Clock`, applies the 45-second startup grace and 45-second success age, and returns only up/down without detail. Retain the foundation `HealthCacheControlFilter` unchanged so it wraps both health paths at highest precedence and forces the exact `no-store` value for both healthy and degraded responses. Configure:

```yaml
management:
  health:
    db:
      enabled: false
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never
      probes:
        enabled: true
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState,dbReadiness,telegramWorkerReadiness
spring:
  datasource:
    hikari:
      connection-timeout: 2000
      validation-timeout: 1000
```

Run the health test and expect PASS with exact status-only JSON and exact `Cache-Control: no-store` on liveness `200` plus readiness `200` and `503`.

- [ ] **Step 3: GREEN — enable production-only private OTLP and structured output**

Add `io.micrometer:micrometer-registry-otlp`; keep the version Boot-managed. Common/test configuration sets `management.otlp.metrics.export.enabled: false`. Production configuration binds:

```yaml
management:
  otlp:
    metrics:
      export:
        enabled: true
        url: ${OTLP_METRICS_URL}
        headers:
          Authorization: ${OTLP_AUTHORIZATION}
        step: 30s
logging:
  structured:
    format:
      console: ecs
```

`TelemetryConfiguration` applies common tags only `application=andrew-website` and profile class `test|local|prod`; it registers no user-controlled tag. Meter names and tag values are allowlisted to those in `architecture.md`. Log statements use fixed messages plus bounded outcome/state/numeric outbox identifiers and never serialize domain records.

Run: `./mvnw -B -Dtest=HealthContractIntegrationTest,TelemetryPrivacyTest test`

Expected: PASS; no OTLP network call in test; telemetry privacy scan green.

- [ ] **Step 4: REFACTOR and run the release-quality gate**

Run:

```bash
./mvnw -B verify
```

Expected: PASS with at least 80% coverage, PostgreSQL degradation tests green, status-only public health with exact no-store headers, and no sensitive actuator/raw metrics exposure. In a production-like non-secret test environment, point OTLP at a local capture receiver and assert only allowlisted metric names/tags; do not store authorization or payload artifacts.

- [ ] **Step 5: Commit**

```bash
git add pom.xml src
git commit -m "feat(backend-observability): add safe health and OTLP telemetry"
```
