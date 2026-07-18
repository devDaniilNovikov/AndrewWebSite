# Telegram Delivery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver committed leads asynchronously to Telegram through a tested gateway and a recoverable PostgreSQL leased worker.

**Architecture:** A Boot-managed `RestClient.Builder` implements a narrow Telegram gateway with explicit status classification and safe 429 parsing. A separate scheduled worker claims bounded rows transactionally with `FOR UPDATE SKIP LOCKED`, commits, sends HTTP without a transaction, and marks outcomes through lease-token compare-and-set updates. Delivery is intentionally at least once.

**Tech Stack:** Java 25, Spring Boot 4.1.0, Spring RestClient, JdbcClient, PostgreSQL 17, Spring Scheduling, Micrometer, MockRestServiceServer, JUnit Jupiter, Testcontainers 2.0.x

## Global Constraints

- One root Maven module; Java 25 LTS; Spring Boot 4.1.0; package `ru.andrew.website`; managed PostgreSQL 17. Frontend remains `frontend/` with Next.js 16.2.9, React 19.2.x, strict TypeScript, Tailwind CSS 4, Motion, and Node 24 only at build time.
- Public surface remains static content, JSON-only 16 KiB `POST /api/leads`, dependency-free liveness, and minimal readiness; no login/session/form login/HTTP Basic or sensitive/raw actuator endpoint.
- Lead validation remains name 2–100, phone input 32 and normalized digits 7–15, optional comment 1000, local source path, intents exactly `repair|maintenance`, consent true, empty indistinguishable `202`, RFC 9457 errors, and HMAC key only from `LEAD_FINGERPRINT_HMAC_KEY`.
- Bounded limits remain a rolling global maximum of 60 admissions in every `(t - 60 seconds, t]` interval and a separate per-connection burst 5/refill 1 token per minute; forwarded headers remain untrusted until Timeweb CIDRs are verified.
- Telegram bot token and chat ID bind only from `TELEGRAM_BOT_TOKEN` and `TELEGRAM_CHAT_ID`; production fails fast when either is absent; production values never appear in source, URLs in logs, health, metrics, fixtures, or errors, and test values are visibly fictional.
- The message contains name, normalized phone, optional comment, source path, exact `repair|maintenance` intent, UTC creation time, and `requestId`; the outbox stores none of that duplicate PII and references `leads` only.
- Exact states are `pending`, `processing`, `retry`, `blocked`, `delivered`; poll 15 seconds; claim maximum 10; lease two minutes; deterministic order `next_attempt_at, id`; `FOR UPDATE SKIP LOCKED` is queue coordination, not exactly-once delivery.
- Telegram HTTP occurs outside the claim transaction. Every completion update matches `id`, `processing`, and random `lease_token`.
- Retry delay for attempt `n >= 1` is `min(30 seconds * 2^(n - 1), 6 hours)` without overflow. Telegram 429 `parameters.retry_after` is integer seconds and produces `min(6 hours, max(exponential, retry_after))`.
- Expired leases recover to due `retry`; a crash after Telegram accepts but before `delivered` commits may duplicate the message, and `requestId` supports human recognition.
- Claim and pre-send reload exclude leads at or beyond the 29-day operational privacy threshold. `privacy_expired` is terminal and cannot be delivered.
- PII hard limit is 30 days; fingerprint/name/phone/comment clear at 29 days; technical rows delete after 12 months; PostgreSQL backup and Telegram auto-delete are each at most 30 days.
- Liveness is dependency-free; readiness is PostgreSQL plus a successful worker poll within 45 seconds with no detail; OTLP is deferred to `task-backend-observability`.
- Micrometer tags are bounded enums/status classes only; never name, phone, comment, source path, request ID, Telegram body, exception message, token, or chat ID.
- No stacked PRs; each task starts after the prior PR merges, follows strict RED → GREEN → REFACTOR, produces one reviewable PR, and is never auto-merged.
- Every AI-authored commit adds the executing agent's own `Co-Authored-By` attribution footer and never attributes a human identity.

---

### Task 1: `task-telegram-client` — safe RestClient gateway and status model

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application-prod.yml`
- Modify: `src/test/resources/application-test.yml`
- Create: `src/main/java/ru/andrew/website/telegram/TelegramClientProperties.java`
- Create: `src/main/java/ru/andrew/website/telegram/TelegramLeadMessage.java`
- Create: `src/main/java/ru/andrew/website/telegram/TelegramDeliveryResult.java`
- Create: `src/main/java/ru/andrew/website/telegram/TelegramGateway.java`
- Create: `src/main/java/ru/andrew/website/telegram/TelegramRestClientGateway.java`
- Create: `src/main/java/ru/andrew/website/telegram/TelegramRetryAfterParser.java`
- Create: `src/main/java/ru/andrew/website/telegram/TelegramMessageFormatter.java`
- Create: `src/test/java/ru/andrew/website/telegram/TelegramRestClientGatewayTest.java`
- Create: `src/test/java/ru/andrew/website/telegram/TelegramMessageFormatterTest.java`

**Interfaces:**
- Consumes: Boot-auto-configured `RestClient.Builder`; normalized lead values; fixed Telegram host; validated configuration.
- Produces: `TelegramGateway.send(TelegramLeadMessage)`; sealed `TelegramDeliveryResult`; a deterministic plain-text message; no queue state mutation.

- [ ] **Step 1: RED — specify formatting, statuses, flood control, and failures**

Add `spring-boot-starter-restclient` to `pom.xml`. Use Spring's `MockRestServiceServer.bindTo(RestClient.Builder)` so no real Telegram request occurs. Create the core test matrix:

```java
package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;

@ExtendWith(OutputCaptureExtension.class)
class TelegramRestClientGatewayTest {
    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private TelegramRestClientGateway gateway;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        TelegramClientProperties properties = new TelegramClientProperties(
                "test-only-bot-token", "test-only-chat", java.net.URI.create("https://api.telegram.org"));
        gateway = new TelegramRestClientGateway(builder, properties, new TelegramMessageFormatter());
    }

    @Test
    void parsesRetryAfterAsSeconds() {
        server.expect(once(), requestTo("https://api.telegram.org/bottest-only-bot-token/sendMessage"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"ok\":false,\"parameters\":{\"retry_after\":120}}"));
        TelegramDeliveryResult result = gateway.send(message());
        assertThat(result).isEqualTo(new TelegramDeliveryResult.Retryable("telegram_429", Duration.ofSeconds(120)));
        server.verify();
    }

    @Test
    void networkFailureIsRetryableWithoutLeakingExceptionText() {
        server.expect(once(), requestTo("https://api.telegram.org/bottest-only-bot-token/sendMessage"))
                .andRespond(withException(new IOException("fictional-network-failure")));
        assertThat(gateway.send(message()))
                .isEqualTo(new TelegramDeliveryResult.Retryable("network", null));
    }

    private TelegramLeadMessage message() {
        return new TelegramLeadMessage(7L,
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                "Иван", "79991234567", "Не охлаждает витрина", "/service/",
                "repair", Instant.parse("2026-01-01T00:00:00Z"));
    }
}
```

Add these executable methods to the same class. Together with `networkFailureIsRetryableWithoutLeakingExceptionText`, they cover 200, 400, 401, 403, another 4xx, 500, 503, timeout/IO, every required 429 edge, malformed JSON, and captured-log redaction:

```java
@ParameterizedTest
@MethodSource("statusClassifications")
void classifiesEveryHttpStatus(HttpStatus status, TelegramDeliveryResult expected) {
    server.expect(once(), requestTo("https://api.telegram.org/bottest-only-bot-token/sendMessage"))
            .andRespond(withStatus(status).contentType(MediaType.APPLICATION_JSON).body("{}"));
    assertThat(gateway.send(message())).isEqualTo(expected);
    server.verify();
}

static Stream<Arguments> statusClassifications() {
    return Stream.of(
            Arguments.of(HttpStatus.OK, new TelegramDeliveryResult.Delivered()),
            Arguments.of(HttpStatus.BAD_REQUEST,
                    new TelegramDeliveryResult.PermanentFailure("telegram_400")),
            Arguments.of(HttpStatus.UNAUTHORIZED,
                    new TelegramDeliveryResult.PermanentFailure("telegram_401")),
            Arguments.of(HttpStatus.FORBIDDEN,
                    new TelegramDeliveryResult.PermanentFailure("telegram_403")),
            Arguments.of(HttpStatus.NOT_FOUND,
                    new TelegramDeliveryResult.PermanentFailure("telegram_404")),
            Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR,
                    new TelegramDeliveryResult.Retryable("telegram_5xx", null)),
            Arguments.of(HttpStatus.SERVICE_UNAVAILABLE,
                    new TelegramDeliveryResult.Retryable("telegram_5xx", null)));
}

@ParameterizedTest
@MethodSource("retryAfterBodies")
void rejectsInvalidRetryAfterAndCapsLargeValues(String body, Duration expected) {
    server.expect(once(), requestTo("https://api.telegram.org/bottest-only-bot-token/sendMessage"))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON).body(body));
    assertThat(gateway.send(message())).isEqualTo(
            new TelegramDeliveryResult.Retryable("telegram_429", expected));
    server.verify();
}

static Stream<Arguments> retryAfterBodies() {
    return Stream.of(
            Arguments.of("{\"ok\":false}", null),
            Arguments.of("{\"parameters\":{\"retry_after\":-1}}", null),
            Arguments.of("{\"parameters\":{\"retry_after\":0}}", null),
            Arguments.of("{\"parameters\":{\"retry_after\":21601}}", Duration.ofHours(6)),
            Arguments.of("not-json", null));
}

@Test
void timeoutIsRetryable() {
    server.expect(once(), requestTo("https://api.telegram.org/bottest-only-bot-token/sendMessage"))
            .andRespond(withException(new SocketTimeoutException("fictional-timeout")));
    assertThat(gateway.send(message()))
            .isEqualTo(new TelegramDeliveryResult.Retryable("network", null));
}

@Test
void capturedLogsContainNoSecretOrMessageValues(CapturedOutput output) {
    server.expect(once(), requestTo("https://api.telegram.org/bottest-only-bot-token/sendMessage"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
    gateway.send(message());
    assertThat(output.getAll()).doesNotContain(
            "test-only-bot-token", "test-only-chat", "Иван", "79991234567",
            "Не охлаждает витрина", "/service/", "repair",
            "11111111-1111-4111-8111-111111111111", "2026-01-01T00:00:00Z",
            "botToken", "chatId", "text");
    server.verify();
}
```

Run: `./mvnw -B -Dtest=TelegramRestClientGatewayTest,TelegramMessageFormatterTest test`

Expected: FAIL because gateway types do not exist.

- [ ] **Step 2: GREEN — create immutable gateway contracts and validated properties**

```java
package ru.andrew.website.telegram;

import java.time.Instant;
import java.util.UUID;

public record TelegramLeadMessage(
        long leadId, UUID requestId, String name, String phone, String comment,
        String sourcePath, String intent, Instant createdAt) {
}
```

```java
package ru.andrew.website.telegram;

import java.time.Duration;

public sealed interface TelegramDeliveryResult {
    record Delivered() implements TelegramDeliveryResult {}
    record Retryable(String code, Duration retryAfter) implements TelegramDeliveryResult {}
    record PermanentFailure(String code) implements TelegramDeliveryResult {}
}
```

```java
package ru.andrew.website.telegram;

public interface TelegramGateway {
    TelegramDeliveryResult send(TelegramLeadMessage message);
}
```

```java
package ru.andrew.website.telegram;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.telegram")
public record TelegramClientProperties(
        @NotBlank String botToken,
        @NotBlank String chatId,
        @NotNull URI baseUrl) {
    public TelegramClientProperties {
        if (baseUrl != null && !baseUrl.isAbsolute()) {
            throw new IllegalArgumentException("app.telegram.base-url must be absolute");
        }
    }
}
```

Production binds `bot-token: ${TELEGRAM_BOT_TOKEN}`, `chat-id: ${TELEGRAM_CHAT_ID}`, and fixed `base-url: https://api.telegram.org`; test uses visibly fictional values and a mock URL. No production fallback exists.

`TelegramMessageFormatter.format(TelegramLeadMessage)` returns deterministic Russian labels in this exact order: request ID, UTC time, intent, source, name, phone, optional comment. It escapes Telegram HTML special characters and the gateway sends `parse_mode=HTML`; it never logs input or output.

- [ ] **Step 3: GREEN — implement explicit exchange status handling**

```java
package ru.andrew.website.telegram;

import java.time.Duration;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public final class TelegramRestClientGateway implements TelegramGateway {
    private final RestClient client;
    private final TelegramClientProperties properties;
    private final TelegramMessageFormatter formatter;

    public TelegramRestClientGateway(RestClient.Builder builder, TelegramClientProperties properties,
            TelegramMessageFormatter formatter) {
        this.properties = properties;
        this.formatter = formatter;
        this.client = builder.baseUrl(properties.baseUrl().toString()).build();
    }

    @Override
    public TelegramDeliveryResult send(TelegramLeadMessage message) {
        try {
            return client.post()
                    .uri("/bot{token}/sendMessage", properties.botToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("chat_id", properties.chatId(), "text", formatter.format(message),
                            "parse_mode", "HTML"))
                    .exchange((request, response) -> classify(response.getStatusCode().value(), response.bodyTo(String.class)));
        } catch (RestClientException exception) {
            return new TelegramDeliveryResult.Retryable("network", null);
        }
    }

    TelegramDeliveryResult classify(int status, String body) {
        if (status >= 200 && status < 300) return new TelegramDeliveryResult.Delivered();
        if (status == 429) return new TelegramDeliveryResult.Retryable("telegram_429", parseRetryAfter(body));
        if (status >= 400 && status < 500) return new TelegramDeliveryResult.PermanentFailure("telegram_" + status);
        return new TelegramDeliveryResult.Retryable("telegram_5xx", null);
    }

    private Duration parseRetryAfter(String body) {
        return TelegramRetryAfterParser.parseSeconds(body)
                .filter(seconds -> seconds > 0)
                .map(seconds -> Duration.ofSeconds(Math.min(seconds, 21_600L)))
                .orElse(null);
    }
}
```

`TelegramRetryAfterParser.parseSeconds(String)` uses a private Jackson record matching `parameters.retry_after`, caps input before conversion, returns `OptionalLong`, and turns malformed/unexpected bodies into empty without logging the body.

Run: `./mvnw -B -Dtest=TelegramRestClientGatewayTest,TelegramMessageFormatterTest test`

Expected: PASS.

- [ ] **Step 4: REFACTOR and verify**

Use the Boot-injected builder in production (the direct builder above is test-only), ensure URI metrics retain the `{token}` template rather than a token value, bound response parsing, and run `./mvnw -B verify`. Expected: PASS, at least 80% coverage, all fake-server cases green, and log capture contains no PII/secret text.

- [ ] **Step 5: Commit**

```bash
git add pom.xml src
git commit -m "feat(telegram-client): add safe Telegram gateway"
```

### Task 2: `task-telegram-worker` — claim, lease, retry, recovery, and telemetry

**Files:**
- Create: `src/main/java/ru/andrew/website/telegram/OutboxState.java`
- Create: `src/main/java/ru/andrew/website/telegram/ClaimedDelivery.java`
- Create: `src/main/java/ru/andrew/website/telegram/OutboxRepository.java`
- Create: `src/main/java/ru/andrew/website/telegram/JdbcOutboxRepository.java`
- Create: `src/main/java/ru/andrew/website/telegram/RetryPolicy.java`
- Create: `src/main/java/ru/andrew/website/telegram/TelegramWorker.java`
- Create: `src/main/java/ru/andrew/website/telegram/WorkerHeartbeat.java`
- Create: `src/main/java/ru/andrew/website/telegram/TelegramMetrics.java`
- Create: `src/main/java/ru/andrew/website/telegram/TelegramSchedulingConfiguration.java`
- Create: `src/main/java/ru/andrew/website/telegram/TelegramWorkerProperties.java`
- Modify: `src/main/resources/application.yml`
- Create: `src/test/java/ru/andrew/website/telegram/RetryPolicyTest.java`
- Create: `src/test/java/ru/andrew/website/telegram/TelegramWorkerIntegrationTest.java`
- Create: `src/test/java/ru/andrew/website/telegram/TwoWorkerClaimIntegrationTest.java`

**Interfaces:**
- Consumes: `TelegramGateway`, `JdbcClient`, `Clock`, `MeterRegistry`, `leads`, and `telegram_outbox`.
- Produces: claim/recovery repository API, scheduled `TelegramWorker.poll()`, worker last-success heartbeat, and bounded `andrew.telegram.*` meters.

- [ ] **Step 1: RED — specify retry math and two-worker state transitions**

```java
package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {
    private final RetryPolicy policy = new RetryPolicy(Duration.ofSeconds(30), Duration.ofHours(6));

    @Test
    void exponentialDelayStartsAtThirtySecondsAndCapsAtSixHours() {
        assertThat(policy.delay(1, null)).isEqualTo(Duration.ofSeconds(30));
        assertThat(policy.delay(2, null)).isEqualTo(Duration.ofMinutes(1));
        assertThat(policy.delay(20, null)).isEqualTo(Duration.ofHours(6));
    }

    @Test
    void retryAfterUsesSecondsAndStillHonorsCap() {
        assertThat(policy.delay(1, Duration.ofSeconds(120))).isEqualTo(Duration.ofSeconds(120));
        assertThat(policy.delay(1, Duration.ofHours(8))).isEqualTo(Duration.ofHours(6));
    }
}
```

Integration tests seed deterministic due rows and assert: order by `next_attempt_at,id`; maximum 10; two simultaneous workers receive disjoint IDs; claim commits before a gateway fake observes `TransactionSynchronizationManager.isActualTransactionActive() == false`; attempt increments; two-minute lease; success to delivered; 429/retry-after, 5xx, timeout, and network to retry; non-429 4xx to blocked; expired lease to retry and reclaim; stale lease token cannot mark; application restart recovers; privacy-aged lead is not claimed; queue metrics have bounded tags and no PII.

Run: `./mvnw -B -Dtest=RetryPolicyTest,TelegramWorkerIntegrationTest,TwoWorkerClaimIntegrationTest test`

Expected: FAIL because worker types do not exist.

- [ ] **Step 2: GREEN — implement exact state and repository ports**

```java
package ru.andrew.website.telegram;

public enum OutboxState {
    pending,
    processing,
    retry,
    blocked,
    delivered
}
```

```java
package ru.andrew.website.telegram;

import java.time.Instant;
import java.util.UUID;

public record ClaimedDelivery(
        long outboxId, long leadId, UUID leaseToken, int attemptCount,
        Instant leaseUntil, TelegramLeadMessage message) {
}
```

```java
package ru.andrew.website.telegram;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxRepository {
    int recoverExpiredLeases(Instant now);
    List<ClaimedDelivery> claimDue(Instant now, Instant privacyCutoff, int limit, Duration lease);
    Optional<TelegramLeadMessage> reloadDeliverable(long outboxId, UUID leaseToken, Instant privacyCutoff);
    boolean markDelivered(long outboxId, UUID leaseToken, Instant now);
    boolean markRetry(long outboxId, UUID leaseToken, String code, Instant nextAttemptAt, Instant now);
    boolean markBlocked(long outboxId, UUID leaseToken, String code, Instant now);
}
```

`JdbcOutboxRepository.claimDue` is `@Transactional`. It selects `pending` and due `retry` rows joined to non-anonymized leads with `created_at > :privacyCutoff`, orders `o.next_attempt_at, o.id`, limits 10, and uses `FOR UPDATE OF o SKIP LOCKED`. In the same transaction it assigns a different random UUID token per row, increments `attempt_count`, sets `processing`, lease expiry, and `updated_at`, then returns the lead projection. `recoverExpiredLeases` changes expired processing rows to due retry with `lease_expired`. Mark methods update only `where id=:id and state='processing' and lease_token=:leaseToken`.

`WorkerHeartbeat` exposes `void success(Instant instant)`, `Optional<Instant> lastSuccess()`, and `Instant startedAt()` using atomic immutable `Instant` values. `TelegramSchedulingConfiguration` is `@Configuration`, `@EnableScheduling`, and `@Profile("!test")`; focused tests invoke `poll()` directly with a supplied clock.

Use this exact claim predicate:

```sql
select o.id, o.lead_id, o.attempt_count,
       l.request_id, l.name, l.phone, l.comment, l.source_path, l.intent, l.created_at
from telegram_outbox o
join leads l on l.id = o.lead_id
where o.state in ('pending','retry')
  and o.next_attempt_at <= :now
  and l.anonymized_at is null
  and l.created_at > :privacyCutoff
order by o.next_attempt_at, o.id
limit :limit
for update of o skip locked
```

`reloadDeliverable` repeats the lease/state/anonymized/privacy predicates immediately before send and returns no message after a retention block.

- [ ] **Step 3: GREEN — implement scheduler, result application, and bounded meters**

```java
package ru.andrew.website.telegram;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public final class TelegramWorker {
    private static final Duration PRIVACY_THRESHOLD = Duration.ofDays(29);
    private final OutboxRepository outbox;
    private final TelegramGateway gateway;
    private final RetryPolicy retryPolicy;
    private final WorkerHeartbeat heartbeat;
    private final Clock clock;

    public TelegramWorker(OutboxRepository outbox, TelegramGateway gateway,
            RetryPolicy retryPolicy, WorkerHeartbeat heartbeat, Clock clock) {
        this.outbox = outbox;
        this.gateway = gateway;
        this.retryPolicy = retryPolicy;
        this.heartbeat = heartbeat;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.telegram.worker.poll-interval:15s}")
    public void poll() {
        Instant now = clock.instant();
        outbox.recoverExpiredLeases(now);
        var claims = outbox.claimDue(now, now.minus(PRIVACY_THRESHOLD), 10, Duration.ofMinutes(2));
        heartbeat.success(now);
        claims.forEach(this::deliver);
    }

    private void deliver(ClaimedDelivery claim) {
        Instant now = clock.instant();
        outbox.reloadDeliverable(claim.outboxId(), claim.leaseToken(), now.minus(PRIVACY_THRESHOLD))
                .ifPresent(message -> apply(claim, gateway.send(message), clock.instant()));
    }

    private void apply(ClaimedDelivery claim, TelegramDeliveryResult result, Instant now) {
        if (result instanceof TelegramDeliveryResult.Delivered) {
            outbox.markDelivered(claim.outboxId(), claim.leaseToken(), now);
        } else if (result instanceof TelegramDeliveryResult.Retryable retryable) {
            Duration delay = retryPolicy.delay(claim.attemptCount(), retryable.retryAfter());
            outbox.markRetry(claim.outboxId(), claim.leaseToken(), retryable.code(), now.plus(delay), now);
        } else if (result instanceof TelegramDeliveryResult.PermanentFailure failure) {
            outbox.markBlocked(claim.outboxId(), claim.leaseToken(), failure.code(), now);
        }
    }
}
```

Record counters through a `TelegramMetrics` wrapper with meter `andrew.telegram.delivery`, tag `outcome=delivered|retry|blocked`, and bounded `reason=success|network|telegram_429|telegram_4xx|telegram_5xx|lease_expired|privacy_expired`. Queue depth uses only the five state values. Do not tag path, request ID, raw status text, exception, or message.

Run: `./mvnw -B -Dtest=RetryPolicyTest,TelegramWorkerIntegrationTest,TwoWorkerClaimIntegrationTest test`

Expected: PASS for all state, ordering, concurrency, transaction-boundary, retry, recovery, privacy, and telemetry assertions.

- [ ] **Step 4: REFACTOR and complete verification**

Factor SQL row mapping and result application below 50 lines, add the accepted crash-after-send test showing a recovered duplicate with the same request ID, then run `./mvnw -B verify`. Expected: PASS with PostgreSQL 17, at least 80% coverage, no sleeping tests, and captured logs/metrics free of PII.

- [ ] **Step 5: Commit**

```bash
git add src
git commit -m "feat(telegram-worker): add leased outbox delivery"
```
