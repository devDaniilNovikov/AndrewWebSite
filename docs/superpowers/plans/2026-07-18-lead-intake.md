# Lead Intake Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the exact public JSON lead contract with stateless security, PostgreSQL 17 persistence, HMAC idempotency, and atomic lead/outbox acceptance.

**Architecture:** The web boundary enforces route, media type, body, validation, and bounded rate-limit rules before an application service normalizes the lead. One PostgreSQL transaction uses the unique request ID and keyed fingerprint to create the lead/outbox pair or resolve duplicate/conflict/retained replay. The outbox references the lead and contains no duplicate PII.

**Tech Stack:** Spring Boot 4.1.0, Java 25, Spring Security 7, Spring MVC `ProblemDetail`, Bean Validation, `JdbcClient`, Flyway, PostgreSQL 17, Testcontainers 2.0.x, MockMvc, JUnit Jupiter

## Global Constraints

- One root Maven module; Java 25 LTS; Spring Boot 4.1.0; package `ru.andrew.website`; managed PostgreSQL 17.
- Frontend remains under `frontend/` with Next.js 16.2.9, React 19.2.x, strict TypeScript, Tailwind CSS 4, Motion, and Node 24 only at build time.
- `POST /api/leads` accepts only `application/json` up to 16 KiB. Its mutually exclusive OpenAPI `oneOf` permits either a fully validated legitimate lead with empty/absent `website` or a synthetic request requiring only a non-empty `website`; both reject unknown properties and retain typed JSON deserialization. Every accepted branch returns the same empty `202`.
- Problem responses are RFC 9457 `application/problem+json`: exactly `400`, `409`, `413`, `415`, `429`, and `503` where applicable; rejected values and internals are never echoed.
- Exact fields: `requestId`, `name`, `phone`, optional `comment`, `sourcePath`, `intent`, `consent`, optional `website`; intents are exactly `repair` and `maintenance`.
- Name trims/NFC to 2–100 characters; phone input is at most 32 characters and normalizes to 7–15 digits; comment is at most 1000; source path is local-only and at most 2048; consent is exactly true.
- Production HMAC comes only from `LEAD_FINGERPRINT_HMAC_KEY`, has at least 32 UTF-8 bytes, and has no default; only `test` may use fixed `test-only-key-material-not-for-production-0001`.
- The global limiter admits at most 60 requests in every rolling half-open `(t - 60 seconds, t]` interval; the separate per-connection token bucket has capacity 5/refill 1 token per minute; the client bucket map is bounded at 10,000 entries with one-hour idle eviction. Ignore forwarded headers until trusted Timeweb CIDRs are verified.
- Outbox states remain exactly `pending|processing|retry|blocked|delivered`; the later worker polls 15 seconds, claims 10 with a two-minute lease and deterministic `FOR UPDATE SKIP LOCKED`, sends HTTP after claim commit, and retries 30 seconds through six hours while honoring Telegram `retry_after` seconds.
- PII hard limit remains 30 days; anonymize at 29 days, remove fingerprint, block undelivered work as `privacy_expired`, delete technical rows after 12 months, and require database backup/Telegram auto-delete of at most 30 days.
- Liveness remains dependency-free; final readiness is PostgreSQL plus worker heartbeat with no detail; Micrometer tags are bounded/PII-free and OTLP begins only in `task-backend-observability`.
- No login, sessions, form login, HTTP Basic, credentialed cross-origin state, public raw metrics, environment, configuration, shutdown, heap, or arbitrary actuator endpoints.
- PII never appears in logs, metric tags, errors, fixtures beyond clearly fictional examples, HMAC diagnostics, or outbox payload copies.
- Follow the [canonical Git Flow](../../../.agents/workflows/GIT_FLOW.md): each approved product task uses one dedicated external worktree and one lowercase `task-*` or `fix-*` branch from the latest `origin/main`, then one Draft PR; direct pushes to `main`, stacked PRs, reused worktrees, non-squash merges, and auto-merge are forbidden. Mark Ready only after required CI is green and Codex review is complete; squash-merge only after explicit user authorization; then confirm `main`, close the issue, allow automatic remote-branch deletion, remove only a worktree with no tracked or untracked work to preserve, and run `git fetch --prune`. Preserve strict RED → GREEN → REFACTOR.
- Every AI-authored commit adds the executing agent's own `Co-Authored-By` attribution footer and never attributes a human identity.

---

### Task 1: `task-backend-http-security` — public boundary, RFC 9457, and bounded limiting

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.yml`
- Create: `src/main/java/ru/andrew/website/web/SecurityConfiguration.java`
- Create: `src/main/java/ru/andrew/website/web/LocalCorsConfiguration.java`
- Create: `src/main/java/ru/andrew/website/web/ProblemResponseAdvice.java`
- Create: `src/main/java/ru/andrew/website/web/RequestBodyLimitFilter.java`
- Create: `src/main/java/ru/andrew/website/web/RateLimitFilter.java`
- Create: `src/main/java/ru/andrew/website/web/ClientRateLimiter.java`
- Create: `src/main/java/ru/andrew/website/web/SlidingWindowRateLimiter.java`
- Create: `src/main/java/ru/andrew/website/web/TokenBucket.java`
- Create: `src/main/java/ru/andrew/website/web/RateDecision.java`
- Create: `src/main/java/ru/andrew/website/web/WebProperties.java`
- Create: `src/test/java/ru/andrew/website/web/SecurityContractTest.java`
- Create: `src/test/java/ru/andrew/website/web/LeadBoundaryStubController.java`
- Create: `src/test/java/ru/andrew/website/web/ClientRateLimiterTest.java`
- Create: `src/test/java/ru/andrew/website/testing/MutableClock.java`

**Interfaces:**
- Consumes: approved routes from `docs/backend/openapi.yaml`; connection address from `HttpServletRequest.getRemoteAddr()` only.
- Produces: `SecurityFilterChain securityFilterChain(HttpSecurity)`; `record RateDecision(boolean allowed, Duration retryAfter)`; `ClientRateLimiter.tryAcquire(String)`; filters ordered body-limit then rate-limit before MVC; stable problem URNs from OpenAPI.

- [ ] **Step 1: RED — specify security, media, payload, CORS, and limiter behavior**

Add `spring-boot-starter-security` and test-scoped `spring-security-test` to `pom.xml`, then create focused tests. The security test uses a test-only controller mapped to `/api/leads` so it tests the boundary before the real controller exists:

```java
package ru.andrew.website.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.web.rate-limit.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityContractTest {
    @Autowired MockMvc mvc;

    @Test
    void forbiddenActuatorAndLoginRoutesStayClosed() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/metrics")).andExpect(status().isForbidden());
        mvc.perform(get("/login")).andExpect(status().isNotFound());
    }

    @Test
    void leadRequiresJsonAndRejectsCrossOriginProductionRequest() throws Exception {
        mvc.perform(post("/api/leads").content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/api/leads").contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", "https://cross-origin.invalid").content("{}"))
                .andExpect(status().isAccepted())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mvc.perform(options("/api/leads")
                        .header("Origin", "https://cross-origin.invalid")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void leadRejectsBodyOverSixteenKibibytes() throws Exception {
        String body = "{\"comment\":\"" + "x".repeat(16_384) + "\"}";
        mvc.perform(post("/api/leads").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isPayloadTooLarge());
    }
}
```

Create the test-only target used by that boundary test:

```java
package ru.andrew.website.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class LeadBoundaryStubController {
    @PostMapping(path = "/api/leads", consumes = "application/json")
    ResponseEntity<Void> accept() {
        return ResponseEntity.accepted().build();
    }
}
```

```java
package ru.andrew.website.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import ru.andrew.website.testing.MutableClock;

class ClientRateLimiterTest {
    @Test
    void sixthImmediateClientRequestIsRejectedForOneMinute() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        ClientRateLimiter limiter = ClientRateLimiter.defaults(clock);
        for (int index = 0; index < 5; index++) {
            assertThat(limiter.tryAcquire("192.0.2.10").allowed()).isTrue();
        }
        RateDecision rejected = limiter.tryAcquire("192.0.2.10");
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfter()).isEqualTo(Duration.ofMinutes(1));
        clock.advance(Duration.ofMinutes(1));
        assertThat(limiter.tryAcquire("192.0.2.10").allowed()).isTrue();
    }

    @Test
    void globalWindowNeverAdmitsSixtyFirstRequestWithinRollingMinute() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        ClientRateLimiter limiter = ClientRateLimiter.defaults(clock);
        for (int index = 0; index < 60; index++) {
            assertThat(limiter.tryAcquire("192.0.2." + index).allowed()).isTrue();
        }

        clock.advance(Duration.ofSeconds(59));
        RateDecision rejected = limiter.tryAcquire("198.51.100.1");
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfter()).isEqualTo(Duration.ofSeconds(1));

        clock.advance(Duration.ofSeconds(1));
        assertThat(limiter.tryAcquire("198.51.100.2").allowed()).isTrue();
    }
}
```

Create the focused test clock; it never uses wall-clock sleeps:

```java
package ru.andrew.website.testing;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

public final class MutableClock extends Clock {
    private Instant instant;
    private final ZoneId zone;

    public MutableClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    @Override public ZoneId getZone() { return zone; }
    @Override public Clock withZone(ZoneId newZone) { return new MutableClock(instant, newZone); }
    @Override public Instant instant() { return instant; }
    public void advance(Duration duration) { instant = instant.plus(duration); }
    public void setInstant(Instant newInstant) { instant = newInstant; }
}
```

Run: `./mvnw -B -Dtest=SecurityContractTest,ClientRateLimiterTest test`

Expected: FAIL because security/filter/limiter types and behavior do not exist.

- [ ] **Step 2: GREEN — implement the stateless allowlist and two distinct bounded limiters**

Implement immutable public interfaces:

```java
package ru.andrew.website.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.web")
public record WebProperties(
        @Min(16_384) @Max(16_384) int maxRequestBytes,
        @Valid @NotNull RateLimit rateLimit,
        List<URI> localCorsOrigins) {
    public record RateLimit(
            boolean enabled,
            @Min(10_000) @Max(10_000) int maxClients,
            @NotNull Duration clientIdleTtl,
            @Min(60) @Max(60) int globalLimit,
            @NotNull Duration globalWindow,
            @Min(5) @Max(5) int clientCapacity,
            @NotNull Duration clientRefill) {
    }
}
```

Bind exact common values `max-request-bytes: 16384`, `max-clients: 10000`, `client-idle-ttl: 1h`, `global-limit: 60`, `global-window: 1m`, `client-capacity: 5`, and `client-refill: 1m`. Production has no `local-cors-origins`; only the local profile may bind explicit loopback origins. `global-window` is a rolling window and must never be interpreted as a token-refill period.

```java
package ru.andrew.website.web;

import java.time.Duration;

public record RateDecision(boolean allowed, Duration retryAfter) {
    public static RateDecision allowedDecision() {
        return new RateDecision(true, Duration.ZERO);
    }

    public static RateDecision rejected(Duration retryAfter) {
        return new RateDecision(false, retryAfter);
    }
}
```

```java
package ru.andrew.website.web;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;

public final class SlidingWindowRateLimiter {
    private final int limit;
    private final long windowMillis;
    private final Clock clock;
    private final ArrayDeque<Long> admittedAt = new ArrayDeque<>();
    private long lastObservedMillis;

    SlidingWindowRateLimiter(int limit, Duration window, Clock clock) {
        if (limit < 1 || window.isZero() || window.isNegative() || window.toMillis() < 1) {
            throw new IllegalArgumentException("limit and window must be positive");
        }
        this.limit = limit;
        this.windowMillis = window.toMillis();
        this.clock = clock;
        this.lastObservedMillis = clock.millis();
    }

    synchronized boolean tryAcquire() {
        long now = monotonicNow();
        evictExpired(now);
        if (admittedAt.size() >= limit) {
            return false;
        }
        admittedAt.addLast(now);
        return true;
    }

    synchronized Duration retryAfter() {
        long now = monotonicNow();
        evictExpired(now);
        if (admittedAt.size() < limit) {
            return Duration.ZERO;
        }
        return Duration.ofMillis(Math.max(1_000L,
                admittedAt.getFirst() + windowMillis - now));
    }

    private long monotonicNow() {
        lastObservedMillis = Math.max(lastObservedMillis, clock.millis());
        return lastObservedMillis;
    }

    private void evictExpired(long now) {
        long cutoffInclusive = now - windowMillis;
        while (!admittedAt.isEmpty() && admittedAt.getFirst() <= cutoffInclusive) {
            admittedAt.removeFirst();
        }
    }
}
```

The deque represents the precise half-open rolling interval `(t - globalWindow, t]`. It can never contain more than `globalLimit` timestamps, entries at exactly `t - globalWindow` expire before the decision, and a wall clock moving backward cannot reopen capacity. This differs intentionally from the per-connection token bucket below.

```java
package ru.andrew.website.web;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ClientRateLimiter {
    private static final int MAX_CLIENTS = 10_000;
    private static final Duration IDLE_TTL = Duration.ofHours(1);
    private final Clock clock;
    private final SlidingWindowRateLimiter global;
    private final Map<String, ClientBucket> clients = new LinkedHashMap<>(128, 0.75f, true);

    ClientRateLimiter(Clock clock, SlidingWindowRateLimiter global) {
        this.clock = clock;
        this.global = global;
    }

    public static ClientRateLimiter defaults(Clock clock) {
        return new ClientRateLimiter(clock,
                new SlidingWindowRateLimiter(60, Duration.ofMinutes(1), clock));
    }

    public synchronized RateDecision tryAcquire(String connectionAddress) {
        long now = clock.millis();
        clients.entrySet().removeIf(entry -> now - entry.getValue().lastSeenMillis() > IDLE_TTL.toMillis());
        if (!global.tryAcquire()) {
            return RateDecision.rejected(global.retryAfter());
        }
        ClientBucket client = clients.computeIfAbsent(connectionAddress,
                ignored -> new ClientBucket(new TokenBucket(5, Duration.ofMinutes(1), clock), now));
        clients.put(connectionAddress, new ClientBucket(client.bucket(), now));
        if (!client.bucket().tryAcquire()) {
            return RateDecision.rejected(client.bucket().retryAfter());
        }
        while (clients.size() > MAX_CLIENTS) {
            clients.remove(clients.keySet().iterator().next());
        }
        return RateDecision.allowedDecision();
    }

    private record ClientBucket(TokenBucket bucket, long lastSeenMillis) {
    }
}
```

`TokenBucket` is used only for connection-address buckets. It has exact constructor `TokenBucket(int capacity, Duration refillPeriod, Clock clock)`, synchronized `boolean tryAcquire()`, and synchronized `Duration retryAfter()`. It stores no address, refills monotonically from `Clock.millis()`, caps tokens at capacity 5, refills one token per minute, and returns at least one second for rejection. The global rolling-window decision runs first; a request rejected by the later client gate may conservatively consume a global admission slot, but no request passing both gates can become a 61st global admission inside any rolling minute.

Create the security chain:

```java
package ru.andrew.website.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.requestCache(cache -> cache.disable());
        http.formLogin(form -> form.disable());
        http.httpBasic(basic -> basic.disable());
        http.logout(logout -> logout.disable());
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/leads"));
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/leads").permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                .requestMatchers("/api/**", "/actuator/**").denyAll()
                .requestMatchers(HttpMethod.GET, "/", "/**").permitAll()
                .requestMatchers(HttpMethod.HEAD, "/", "/**").permitAll()
                .anyRequest().denyAll());
        return http.build();
    }
}
```

CSRF is ignored only for the unauthenticated JSON command because there are no cookies, sessions, or browser credentials; production is same-origin, CORS has no allowed origin, content type is JSON-only, and rate/honeypot defenses remain active. `RateLimitFilter` uses only `getRemoteAddr()`, returns `Retry-After` whole seconds with the 429 problem, and never reads forwarded headers. `RequestBodyLimitFilter` rejects declared or streamed bodies above 16,384 bytes before MVC and emits the exact 413 problem. Both filters write only the five RFC 9457 fields from OpenAPI.

`LocalCorsConfiguration` is annotated `@Configuration` and `@Profile("local")`. It creates a `UrlBasedCorsConfigurationSource` for `/api/leads` with only `WebProperties.localCorsOrigins()` string values, method `POST`, header `Content-Type`, `allowCredentials=false`, and `maxAge=Duration.ofMinutes(10)`, then exposes a `CorsFilter` ordered before Spring Security. No CORS bean exists in `prod` or `test`, so browser JSON cross-origin preflights are denied and actual forged Origin headers receive no allow-origin response header.

Run: `./mvnw -B -Dtest=SecurityContractTest,ClientRateLimiterTest test`

Expected: PASS.

- [ ] **Step 3: REFACTOR and run boundary verification**

Add parameterized tests for `400/413/415/429`, no session cookie, local-only explicit CORS, map eviction at 10,000 entries, rolling-global exhaustion at staggered timestamps, exact expiry at 60 seconds, per-client one-token-per-minute refill, and forged `X-Forwarded-For`. Never use a 60-capacity token bucket in a global-limit test. Run:

```bash
./mvnw -B verify
```

Expected: PASS, line coverage at least 80%, no public sensitive actuator response, and no PII in captured logs.

- [ ] **Step 4: Commit**

```bash
git add pom.xml src
git commit -m "feat(backend-http-security): secure the public HTTP boundary"
```

### Task 2: `task-db-flyway-baseline` — PostgreSQL 17 schema and constraints

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.yml`
- Create: `src/main/resources/db/migration/V1__lead_outbox_baseline.sql`
- Create: `src/test/java/ru/andrew/website/testing/PostgresTestConfiguration.java`
- Create: `src/test/java/ru/andrew/website/leads/LeadOutboxMigrationTest.java`

**Interfaces:**
- Consumes: PostgreSQL 17 and Spring transaction management.
- Produces: exact `leads` and `telegram_outbox` tables, `uk_leads_request_id`, `uk_telegram_outbox_lead_id`, queue/retention indexes, and Boot-auto-configured `org.springframework.jdbc.core.simple.JdbcClient`.

- [ ] **Step 1: Add current PostgreSQL/Flyway/Testcontainers coordinates**

Add `spring-boot-starter-jdbc`, `org.flywaydb:flyway-core`, `org.flywaydb:flyway-database-postgresql`, runtime `org.postgresql:postgresql`, test `spring-boot-testcontainers`, `org.testcontainers:testcontainers-postgresql`, and `org.testcontainers:testcontainers-junit-jupiter`. Do not pin versions managed by Spring Boot 4.1.0.

- [ ] **Step 2: RED — write a PostgreSQL 17 migration contract test**

```java
package ru.andrew.website.leads;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import ru.andrew.website.testing.PostgresTestConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class LeadOutboxMigrationTest {
    @Autowired JdbcClient jdbc;

    @Test
    void requestIdAndOutboxLeadAreUnique() {
        UUID requestId = UUID.fromString("11111111-1111-4111-8111-111111111111");
        long leadId = insertLead(requestId);
        insertOutbox(leadId);
        assertThatThrownBy(() -> insertLead(requestId)).isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> insertOutbox(leadId)).isInstanceOf(DuplicateKeyException.class);
    }

    private long insertLead(UUID requestId) {
        return jdbc.sql("""
                insert into leads(request_id,payload_fingerprint,name,phone,source_path,intent,consented_at,created_at)
                values (:requestId,decode(repeat('00',32),'hex'),'Тест','79990000000','/test/','repair',now(),now())
                returning id
                """).param("requestId", requestId).query(Long.class).single();
    }

    private void insertOutbox(long leadId) {
        jdbc.sql("""
                insert into telegram_outbox(lead_id,state,next_attempt_at,created_at,updated_at)
                values (:leadId,'pending',now(),now(),now())
                """).param("leadId", leadId).update();
    }
}
```

`PostgresTestConfiguration` is a `@TestConfiguration(proxyBeanMethods = false)` with a `@Bean @ServiceConnection PostgreSQLContainer<?> postgres()` returning `new PostgreSQLContainer<>("postgres:17-alpine")`. Spring owns the bean lifecycle so cached test contexts do not outlive a JUnit-managed container.

Run: `./mvnw -B -Dtest=LeadOutboxMigrationTest test`

Expected: FAIL because the migration and tables do not exist.

- [ ] **Step 3: GREEN — create the exact baseline migration**

```sql
create table leads (
    id bigint generated by default as identity primary key,
    request_id uuid not null,
    payload_fingerprint bytea,
    name varchar(100),
    phone varchar(15),
    comment varchar(1000),
    source_path varchar(2048) not null,
    intent varchar(16) not null,
    consented_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    anonymized_at timestamp with time zone,
    constraint uk_leads_request_id unique (request_id),
    constraint ck_leads_intent check (intent in ('repair','maintenance')),
    constraint ck_leads_fingerprint check (payload_fingerprint is null or octet_length(payload_fingerprint) = 32),
    constraint ck_leads_phone check (phone is null or length(phone) between 7 and 15),
    constraint ck_leads_privacy check (
        (anonymized_at is null and payload_fingerprint is not null and name is not null and phone is not null)
        or
        (anonymized_at is not null and payload_fingerprint is null and name is null and phone is null and comment is null)
    )
);

create table telegram_outbox (
    id bigint generated by default as identity primary key,
    lead_id bigint not null,
    state varchar(16) not null,
    attempt_count integer not null default 0,
    next_attempt_at timestamp with time zone not null,
    lease_token uuid,
    lease_until timestamp with time zone,
    last_error_code varchar(64),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    delivered_at timestamp with time zone,
    constraint uk_telegram_outbox_lead_id unique (lead_id),
    constraint fk_telegram_outbox_lead foreign key (lead_id) references leads(id) on delete cascade,
    constraint ck_telegram_outbox_state check (state in ('pending','processing','retry','blocked','delivered')),
    constraint ck_telegram_outbox_attempt_count check (attempt_count >= 0),
    constraint ck_telegram_outbox_shape check (
        (state = 'processing' and lease_token is not null and lease_until is not null and delivered_at is null)
        or (state in ('pending','retry','blocked') and lease_token is null and lease_until is null and delivered_at is null)
        or (state = 'delivered' and lease_token is null and lease_until is null and delivered_at is not null)
    )
);

create index idx_leads_retention on leads(created_at, id) where anonymized_at is null;
create index idx_leads_anonymized_cleanup on leads(anonymized_at, id) where anonymized_at is not null;
create index idx_telegram_outbox_claim on telegram_outbox(next_attempt_at, id) where state in ('pending','retry');
create index idx_telegram_outbox_expired_lease on telegram_outbox(lease_until, id) where state = 'processing';
```

Run: `./mvnw -B -Dtest=LeadOutboxMigrationTest test`

Expected: PASS against `postgres:17-alpine`.

- [ ] **Step 4: REFACTOR and verify every constraint/index**

Extend the test with exact invalid intent, 31-byte fingerprint, illegal lease shape, legal anonymized shape, cascade delete, and `pg_indexes` assertions. Run `./mvnw -B verify` and expect PASS with Flyway validation green and at least 80% line coverage.

- [ ] **Step 5: Commit**

```bash
git add pom.xml src
git commit -m "feat(db-flyway-baseline): add lead and outbox schema"
```

### Task 3: `task-leads-api` — validation, HMAC idempotency, and atomic acceptance

**Files:**
- Modify: `src/main/resources/application-prod.yml`
- Modify: `src/test/resources/application-test.yml`
- Create: `src/main/java/ru/andrew/website/leads/LeadIntent.java`
- Create: `src/main/java/ru/andrew/website/leads/LeadRequest.java`
- Create: `src/main/java/ru/andrew/website/leads/NormalizedLead.java`
- Create: `src/main/java/ru/andrew/website/leads/LeadNormalizer.java`
- Create: `src/main/java/ru/andrew/website/leads/LeadFingerprint.java`
- Create: `src/main/java/ru/andrew/website/leads/LeadFingerprintService.java`
- Create: `src/main/java/ru/andrew/website/leads/LeadProperties.java`
- Create: `src/main/java/ru/andrew/website/leads/AcceptanceOutcome.java`
- Create: `src/main/java/ru/andrew/website/leads/LeadAcceptanceService.java`
- Create: `src/main/java/ru/andrew/website/leads/LeadController.java`
- Create: `src/main/java/ru/andrew/website/leads/IdempotencyConflictException.java`
- Create: `src/test/java/ru/andrew/website/leads/LeadControllerContractTest.java`
- Create: `src/test/java/ru/andrew/website/leads/LeadAcceptanceIntegrationTest.java`
- Create: `src/test/java/ru/andrew/website/leads/LeadUnavailableContractTest.java`
- Delete: `src/test/java/ru/andrew/website/web/LeadBoundaryStubController.java` after the real controller replaces it

**Interfaces:**
- Consumes: `JdbcClient`, `Clock`, `app.leads.fingerprint-key`, the two tables, and web problem mapping.
- Produces: `LeadIntent { repair, maintenance }`; `LeadRequest`; `NormalizedLead`; `AcceptanceOutcome { CREATED, DUPLICATE, RETAINED, HONEYPOT }`; `LeadAcceptanceService.accept(LeadRequest)`; empty `ResponseEntity<Void>` 202.

- [ ] **Step 1: RED — specify the complete HTTP and transactional matrix**

Create MockMvc parameterized tests for every OpenAPI field/bound/status plus this acceptance test:

```java
package ru.andrew.website.leads;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.web.rate-limit.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeadControllerContractTest {
    @Autowired MockMvc mvc;

    @Test
    void acceptedResponseHasNoBodyOrAcceptanceDiscriminator() throws Exception {
        mvc.perform(post("/api/leads").contentType(MediaType.APPLICATION_JSON).content("""
                {"requestId":"11111111-1111-4111-8111-111111111111","name":"Иван",
                 "phone":"+7 999 123-45-67","comment":"Не охлаждает витрина",
                 "sourcePath":"/service/","intent":"repair","consent":true,"website":""}
                """))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
    }

    @Test
    void unknownPropertyIsRejectedEvenWhenHoneypotIsFilled() throws Exception {
        mvc.perform(post("/api/leads").contentType(MediaType.APPLICATION_JSON).content("""
                {"website":"filled-by-bot","unexpected":"rejected"}
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedKnownTypedFieldIsRejectedAtDeserializationBoundary() throws Exception {
        mvc.perform(post("/api/leads").contentType(MediaType.APPLICATION_JSON).content("""
                {"website":"filled-by-bot","requestId":"not-a-uuid"}
                """))
                .andExpect(status().isBadRequest());
    }
}
```

Create the PostgreSQL integration body below. It exercises first/duplicate/conflict/honeypot/retained paths, database-triggered rollback after the lead insert, and both required races without wall-clock sleeps:

```java
package ru.andrew.website.leads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.andrew.website.testing.PostgresTestConfiguration;

@SpringBootTest(properties = "app.web.rate-limit.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class LeadAcceptanceIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;

    @BeforeEach
    void cleanTables() {
        dropFailureTrigger();
        jdbc.sql("delete from telegram_outbox").update();
        jdbc.sql("delete from leads").update();
    }

    @AfterEach
    void removeFailureTrigger() {
        dropFailureTrigger();
    }

    @Test
    void firstAndEquivalentReplayCreateExactlyOnePair() throws Exception {
        UUID id = UUID.fromString("11111111-1111-4111-8111-111111111111");
        assertThat(submit(id, "Не охлаждает витрина", "")).isEqualTo(202);
        assertThat(submit(id, "  Не охлаждает витрина  ", "")).isEqualTo(202);
        assertCounts(1, 1);
    }

    @Test
    void changedReplayConflictsWhileRetainedReplayIsAcceptedWithoutInsert() throws Exception {
        UUID id = UUID.fromString("22222222-2222-4222-8222-222222222222");
        assertThat(submit(id, "Первое описание", "")).isEqualTo(202);
        assertThat(submit(id, "Другое описание", "")).isEqualTo(409);
        jdbc.sql("update leads set payload_fingerprint=null, name=null, phone=null, comment=null, anonymized_at=now() where request_id=:id")
                .param("id", id).update();
        assertThat(submit(id, "Изменено после хранения", "")).isEqualTo(202);
        assertCounts(1, 1);
    }

    @Test
    void websiteOnlyHoneypotReturnsSameEmptyAcceptanceWithoutRows() throws Exception {
        var response = mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"website\":\"filled-by-bot\"}"))
                .andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(202);
        assertThat(response.getContentAsByteArray()).isEmpty();
        assertCounts(0, 0);
    }

    @Test
    void outboxFailureRollsBackLeadAndReturnsGenericUnavailable() throws Exception {
        jdbc.sql("""
                create function test_fail_outbox() returns trigger language plpgsql as $$
                begin raise exception 'test-only forced outbox failure'; end
                $$
                """).update();
        jdbc.sql("create trigger test_fail_outbox before insert on telegram_outbox for each row execute function test_fail_outbox()")
                .update();
        UUID id = UUID.fromString("44444444-4444-4444-8444-444444444444");
        assertThat(submit(id, "Откат транзакции", "")).isEqualTo(503);
        assertCounts(0, 0);
    }

    @Test
    void simultaneousEquivalentRequestsCreateOnePair() throws Exception {
        UUID id = UUID.fromString("55555555-5555-4555-8555-555555555555");
        assertThat(concurrently(
                () -> submit(id, "Одинаково", ""),
                () -> submit(id, "Одинаково", "")))
                .containsExactlyInAnyOrder(202, 202);
        assertCounts(1, 1);
    }

    @Test
    void simultaneousDifferentRequestsYieldAcceptedAndConflict() throws Exception {
        UUID id = UUID.fromString("66666666-6666-4666-8666-666666666666");
        assertThat(concurrently(
                () -> submit(id, "Вариант один", ""),
                () -> submit(id, "Вариант два", "")))
                .containsExactlyInAnyOrder(202, 409);
        assertCounts(1, 1);
    }

    private List<Integer> concurrently(ThrowingRequest first, ThrowingRequest second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var firstResult = executor.submit(() -> { start.await(); return first.run(); });
            var secondResult = executor.submit(() -> { start.await(); return second.run(); });
            start.countDown();
            return List.of(firstResult.get(), secondResult.get());
        }
    }

    private int submit(UUID id, String comment, String website) throws Exception {
        return mvc.perform(post("/api/leads").contentType(MediaType.APPLICATION_JSON)
                        .content(body(id, comment, website)))
                .andReturn().getResponse().getStatus();
    }

    private String body(UUID id, String comment, String website) {
        return """
                {"requestId":"%s","name":"Иван","phone":"+7 999 123-45-67",
                 "comment":"%s","sourcePath":"/service/","intent":"repair",
                 "consent":true,"website":"%s"}
                """.formatted(id, comment, website);
    }

    private void assertCounts(int leads, int outbox) {
        assertThat(jdbc.sql("select count(*) from leads").query(Integer.class).single()).isEqualTo(leads);
        assertThat(jdbc.sql("select count(*) from telegram_outbox").query(Integer.class).single()).isEqualTo(outbox);
    }

    private void dropFailureTrigger() {
        jdbc.sql("drop trigger if exists test_fail_outbox on telegram_outbox").update();
        jdbc.sql("drop function if exists test_fail_outbox()").update();
    }

    @FunctionalInterface
    private interface ThrowingRequest {
        int run() throws Exception;
    }
}
```

Use a separate context override to make database unavailability deterministic without stopping the shared Testcontainer. The mocked exception text must not appear in the RFC 9457 body:

```java
package ru.andrew.website.leads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.andrew.website.testing.PostgresTestConfiguration;

@SpringBootTest(properties = "app.web.rate-limit.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class LeadUnavailableContractTest {
    @Autowired MockMvc mvc;
    @MockitoBean JdbcClient jdbc;

    @Test
    void databaseFailureReturnsGenericProblemWithoutCauseText() throws Exception {
        when(jdbc.sql(anyString())).thenThrow(
                new DataAccessResourceFailureException("fictional-database-detail"));
        var response = mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"requestId":"77777777-7777-4777-8777-777777777777","name":"Иван",
                         "phone":"+7 999 123-45-67","sourcePath":"/service/",
                         "intent":"repair","consent":true,"website":""}
                        """))
                .andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString()).doesNotContain("fictional-database-detail");
    }
}
```

Run: `./mvnw -B -Dtest=LeadControllerContractTest,LeadAcceptanceIntegrationTest,LeadUnavailableContractTest test`

Expected: FAIL because the lead API types and controller do not exist.

- [ ] **Step 2: GREEN — implement exact immutable DTOs and canonical HMAC**

```java
package ru.andrew.website.leads;

public enum LeadIntent {
    repair,
    maintenance
}
```

```java
package ru.andrew.website.leads;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record LeadRequest(
        @NotNull UUID requestId,
        @NotBlank @Size(min = 2, max = 100) String name,
        @NotBlank @Size(max = 32) @Pattern(regexp = "^[+0-9(). -]+$") String phone,
        @Size(max = 1000) String comment,
        @NotBlank @Size(max = 2048) String sourcePath,
        @NotNull LeadIntent intent,
        @NotNull @AssertTrue Boolean consent,
        String website) {
}
```

The record deliberately uses nullable wrapper/reference components and the controller
does not apply `@Valid`, so `{"website":"filled-by-bot"}` deserializes before the
service classifies it. Jackson still converts any supplied `requestId`, `intent`, and
`consent` to `UUID`, `LeadIntent`, and `Boolean`, and
`spring.jackson.deserialization.fail-on-unknown-properties: true` applies to both
request shapes. Thus honeypots skip Bean Validation and normalization, not the common
JSON object/type/unknown-property boundary.

```java
package ru.andrew.website.leads;

import java.time.Instant;
import java.util.UUID;

public record NormalizedLead(
        UUID requestId, String name, String phoneDigits, String comment,
        String sourcePath, LeadIntent intent, Instant consentedAt) {
}
```

`LeadNormalizer.normalize(LeadRequest, Instant)` uses `Normalizer.Form.NFC`, trims name/comment/path, turns blank comment into null, extracts Unicode decimal digits into ASCII, requires 7–15 digits, and rejects scheme/authority/query/fragment/backslash/control/`..` path segments. It never modifies the input record.

```java
package ru.andrew.website.leads;

import java.security.MessageDigest;
import java.util.Objects;

public final class LeadFingerprint {
    private final byte[] bytes;

    public LeadFingerprint(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length != 32) throw new IllegalArgumentException("HMAC-SHA-256 must be 32 bytes");
        this.bytes = bytes.clone();
    }

    public byte[] bytes() {
        return bytes.clone();
    }

    public boolean matches(byte[] candidate) {
        return candidate != null && MessageDigest.isEqual(bytes, candidate);
    }
}
```

```java
package ru.andrew.website.leads;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.leads")
public record LeadProperties(@NotBlank String fingerprintKey) {
    public LeadProperties {
        if (fingerprintKey == null || fingerprintKey.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("app.leads.fingerprint-key must contain at least 32 UTF-8 bytes");
        }
    }
}
```

`LeadFingerprintService.fingerprint(NormalizedLead)` returns a defensive-copy `LeadFingerprint` containing HMAC-SHA-256 of UTF-8 JSON keys in exact order `name`, `phone`, `comment`, `sourcePath`, `intent`, `consent`; it excludes request ID, honeypot, and time. Use a locally created `Mac` per call, `MessageDigest.isEqual` for comparison, and never log key, canonical bytes, or digest.

Bind production `app.leads.fingerprint-key: ${LEAD_FINGERPRINT_HMAC_KEY}` with no fallback. Bind test only to `test-only-key-material-not-for-production-0001`.

- [ ] **Step 3: GREEN — implement the single transaction and controller**

```java
package ru.andrew.website.leads;

public enum AcceptanceOutcome {
    CREATED,
    DUPLICATE,
    RETAINED,
    HONEYPOT
}
```

```java
package ru.andrew.website.leads;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeadAcceptanceService {
    private final JdbcClient jdbc;
    private final LeadNormalizer normalizer;
    private final LeadFingerprintService fingerprints;
    private final Clock clock;
    private final jakarta.validation.Validator validator;

    public LeadAcceptanceService(JdbcClient jdbc, LeadNormalizer normalizer,
            LeadFingerprintService fingerprints, Clock clock, jakarta.validation.Validator validator) {
        this.jdbc = jdbc;
        this.normalizer = normalizer;
        this.fingerprints = fingerprints;
        this.clock = clock;
        this.validator = validator;
    }

    @Transactional
    public AcceptanceOutcome accept(LeadRequest request) {
        if (request.website() != null && !request.website().isEmpty()) {
            return AcceptanceOutcome.HONEYPOT;
        }
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new jakarta.validation.ConstraintViolationException(violations);
        }
        Instant now = clock.instant();
        NormalizedLead lead = normalizer.normalize(request, now);
        byte[] fingerprint = fingerprints.fingerprint(lead).bytes();
        return insertLead(lead, fingerprint, now)
                .map(leadId -> {
                    insertOutbox(leadId, now);
                    return AcceptanceOutcome.CREATED;
                })
                .orElseGet(() -> resolveExisting(lead.requestId(), fingerprint));
    }

    private java.util.Optional<Long> insertLead(NormalizedLead lead, byte[] fingerprint, Instant now) {
        return jdbc.sql("""
                insert into leads(request_id,payload_fingerprint,name,phone,comment,source_path,intent,consented_at,created_at)
                values (:requestId,:fingerprint,:name,:phone,:comment,:sourcePath,:intent,:consentedAt,:createdAt)
                on conflict (request_id) do nothing returning id
                """).params(java.util.Map.of(
                        "requestId", lead.requestId(), "fingerprint", fingerprint, "name", lead.name(),
                        "phone", lead.phoneDigits(), "sourcePath", lead.sourcePath(),
                        "intent", lead.intent().name(), "consentedAt", lead.consentedAt(), "createdAt", now))
                .param("comment", lead.comment()).query(Long.class).optional();
    }

    private AcceptanceOutcome resolveExisting(java.util.UUID requestId, byte[] candidate) {
        byte[] retained = jdbc.sql("select payload_fingerprint from leads where request_id=:requestId")
                .param("requestId", requestId).query(byte[].class).optional().orElse(null);
        if (retained == null) return AcceptanceOutcome.RETAINED;
        if (java.security.MessageDigest.isEqual(retained, candidate)) return AcceptanceOutcome.DUPLICATE;
        throw new IdempotencyConflictException();
    }

    private void insertOutbox(long leadId, Instant now) {
        jdbc.sql("""
                insert into telegram_outbox(lead_id,state,next_attempt_at,created_at,updated_at)
                values (:leadId,'pending',:now,:now,:now)
                """).param("leadId", leadId).param("now", now).update();
    }
}
```

`LeadController.submit(@RequestBody LeadRequest)` deliberately performs no declarative `@Valid` call: the service checks a non-empty honeypot first, then invokes the injected validator for legitimate requests. It returns `ResponseEntity.accepted().build()` for every acceptance outcome. Set `spring.jackson.deserialization.fail-on-unknown-properties: true`. `ProblemResponseAdvice` maps JSON binding failures, unknown properties, constraint failures, and normalization failures to the exact PII-free 400; it maps `IdempotencyConflictException` to the exact 409 OpenAPI problem and `DataAccessException` to the exact generic 503.

Run: `./mvnw -B -Dtest=LeadControllerContractTest,LeadAcceptanceIntegrationTest,LeadUnavailableContractTest test`

Expected: PASS for the website-only/no-row honeypot, typed/unknown JSON boundaries, all legitimate acceptance branches, rollback, races, and unavailable database.

- [ ] **Step 4: REFACTOR and verify contract parity**

Extract SQL parameter construction, keep all methods below 50 lines, create log-capture assertions for fictional name/phone/comment absence, and run:

```bash
./mvnw -B verify
```

Expected: PASS with at least 80% coverage. Compare the mutually exclusive legitimate/honeypot schemas, required fields, JSON types, unknown-property rejection, bounds, examples, status/media types, empty 202 behavior, and no-row website-only assertion field-by-field with `docs/backend/openapi.yaml`.

- [ ] **Step 5: Commit**

```bash
git add src
git commit -m "feat(leads-api): add durable idempotent lead acceptance"
```
