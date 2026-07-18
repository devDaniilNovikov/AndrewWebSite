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
- Liveness is dependency-free; readiness is PostgreSQL plus a full-batch worker success within 45 seconds with no detail; reload/send/state-write exceptions and false lease-token updates never advance the heartbeat; OTLP is deferred to `task-backend-observability`.
- Micrometer tags are bounded enums/status classes only; never name, phone, comment, source path, request ID, Telegram body, exception message, token, or chat ID.
- Follow the [canonical Git Flow](../../../.agents/workflows/GIT_FLOW.md): each approved product task waits for its prerequisite on `main`, then uses one dedicated external worktree and one lowercase `task-*` or `fix-*` branch from the latest `origin/main`, followed by one Draft PR; direct pushes to `main`, stacked PRs, reused worktrees, non-squash merges, and auto-merge are forbidden. Mark Ready only after required CI is green and Codex review is complete; squash-merge only after explicit user authorization; then confirm `main`, close the issue, allow automatic remote-branch deletion, remove only a worktree with no tracked or untracked work to preserve, and run `git fetch --prune`. Preserve strict RED → GREEN → REFACTOR.
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
import tools.jackson.databind.json.JsonMapper;

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
        gateway = new TelegramRestClientGateway(builder, properties, new TelegramMessageFormatter(),
                new TelegramRetryAfterParser(JsonMapper.builder().build()));
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
                    new TelegramDeliveryResult.PermanentFailure("telegram_permanent_400")),
            Arguments.of(HttpStatus.UNAUTHORIZED,
                    new TelegramDeliveryResult.PermanentFailure("telegram_permanent_401")),
            Arguments.of(HttpStatus.FORBIDDEN,
                    new TelegramDeliveryResult.PermanentFailure("telegram_permanent_403")),
            Arguments.of(HttpStatus.NOT_FOUND,
                    new TelegramDeliveryResult.PermanentFailure("telegram_permanent_404")),
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
            Arguments.of("{\"parameters\":{\"retry_after\":\"120\"}}", null),
            Arguments.of("{\"parameters\":{\"retry_after\":9223372036854775808}}", null),
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

Add a `@Profile("prod")` `TelegramProductionEndpointGuard` implementing
`InitializingBean`. It receives `TelegramClientProperties` and rejects startup
unless `baseUrl` has scheme exactly `https`, host exactly `api.telegram.org`, no
user info, no explicit port, and an empty path, query, and fragment. Production
binds `bot-token: ${TELEGRAM_BOT_TOKEN}`, `chat-id: ${TELEGRAM_CHAT_ID}`, and
fixed `base-url: https://api.telegram.org`; there is no production fallback or
override to another endpoint. Test uses visibly fictional values and a mock URL
outside the `prod` profile. Add focused context-runner tests showing that the
production profile accepts the exact fixed URI and rejects a different host,
HTTP, user info, explicit port, path, query, and fragment before any request can
carry the bot token or lead PII.

`TelegramMessageFormatter.format(TelegramLeadMessage)` returns deterministic Russian labels in this exact order: request ID, UTC time, intent, source, name, phone, optional comment. It escapes Telegram HTML special characters and the gateway sends `parse_mode=HTML`; it never logs input or output.

- [ ] **Step 3: GREEN — implement explicit exchange status handling**

```java
package ru.andrew.website.telegram;

import java.time.Duration;
import java.util.Map;
import java.util.OptionalLong;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public final class TelegramRestClientGateway implements TelegramGateway {
    private final RestClient client;
    private final TelegramClientProperties properties;
    private final TelegramMessageFormatter formatter;
    private final TelegramRetryAfterParser retryAfterParser;

    public TelegramRestClientGateway(RestClient.Builder builder, TelegramClientProperties properties,
            TelegramMessageFormatter formatter, TelegramRetryAfterParser retryAfterParser) {
        this.properties = properties;
        this.formatter = formatter;
        this.retryAfterParser = retryAfterParser;
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
        if (status >= 400 && status < 500) {
            return new TelegramDeliveryResult.PermanentFailure("telegram_permanent_" + status);
        }
        return new TelegramDeliveryResult.Retryable("telegram_5xx", null);
    }

    private Duration parseRetryAfter(String body) {
        OptionalLong parsed = retryAfterParser.parseSeconds(body);
        if (parsed.isEmpty() || parsed.getAsLong() <= 0) return null;
        return Duration.ofSeconds(Math.min(parsed.getAsLong(), 21_600L));
    }
}
```

Create the bounded parser. Spring Boot 4.1 injects its Jackson 3 `JsonMapper`; the
tree check rejects string coercion and integers outside the Java `long` range:

```java
package ru.andrew.website.telegram;

import java.util.OptionalLong;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class TelegramRetryAfterParser {
    private static final int MAX_BODY_CHARS = 4_096;
    private final JsonMapper json;

    public TelegramRetryAfterParser(JsonMapper json) {
        this.json = json;
    }

    OptionalLong parseSeconds(String body) {
        if (body == null || body.length() > MAX_BODY_CHARS) return OptionalLong.empty();
        try {
            JsonNode root = json.readTree(body);
            if (root == null) return OptionalLong.empty();
            JsonNode value = root.path("parameters").path("retry_after");
            if (!value.isIntegralNumber() || !value.canConvertToLong()) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(value.longValue());
        } catch (Exception invalidJson) {
            return OptionalLong.empty();
        }
    }
}
```

The primitive optional is never treated like boxed `Optional`: the client uses only
`isEmpty()`/`getAsLong()`, caps valid positive seconds to six hours, and never logs the
body or parse exception. The existing parameterized gateway tests exercise missing,
negative, zero, string, out-of-range, capped, and malformed values against the same
constructor and parser signature.

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
- Create: `src/test/java/ru/andrew/website/telegram/TelegramWorkerHeartbeatTest.java`
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

Create the heartbeat regression test with no database or wall-clock sleep. It proves that a gateway exception and a failed lease-token state write leave the heartbeat untouched, while a durably recorded expected result advances it only after the batch:

```java
package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelegramWorkerHeartbeatTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID LEASE = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final Instant PRIVACY_CUTOFF = NOW.minus(Duration.ofDays(29));

    @Mock OutboxRepository outbox;
    @Mock TelegramGateway gateway;
    @Mock WorkerHeartbeat heartbeat;
    private TelegramWorker worker;

    @BeforeEach
    void setUp() {
        worker = new TelegramWorker(outbox, gateway,
                new RetryPolicy(Duration.ofSeconds(30), Duration.ofHours(6)),
                heartbeat, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void gatewayExceptionDoesNotAdvanceHeartbeat() {
        arrangeOneClaim();
        when(gateway.send(any())).thenThrow(new IllegalStateException("fictional gateway failure"));
        assertThatThrownBy(worker::poll).hasMessage("fictional gateway failure");
        verify(heartbeat, never()).success(any());
    }

    @Test
    void unsuccessfulStateWriteDoesNotAdvanceHeartbeat() {
        arrangeOneClaim();
        when(gateway.send(any())).thenReturn(new TelegramDeliveryResult.Delivered());
        when(outbox.markDelivered(7L, LEASE, NOW)).thenReturn(false);
        assertThatThrownBy(worker::poll)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Outbox state transition was not persisted");
        verify(heartbeat, never()).success(any());
    }

    @Test
    void durablyCompletedBatchAdvancesHeartbeatAfterStateWrite() {
        arrangeOneClaim();
        when(gateway.send(any())).thenReturn(new TelegramDeliveryResult.Delivered());
        when(outbox.markDelivered(7L, LEASE, NOW)).thenReturn(true);
        worker.poll();
        var order = org.mockito.Mockito.inOrder(outbox, heartbeat);
        order.verify(outbox).markDelivered(7L, LEASE, NOW);
        order.verify(heartbeat).success(NOW);
    }

    @Test
    void emptySuccessfulPollAdvancesHeartbeat() {
        when(outbox.recoverExpiredAndClaimDue(
                NOW, PRIVACY_CUTOFF, 10, Duration.ofMinutes(2)))
                .thenReturn(List.of());
        worker.poll();
        verify(heartbeat).success(NOW);
        verify(gateway, never()).send(any());
    }

    @Test
    void retryableResultIsPersistedBeforeHeartbeat() {
        arrangeOneClaim();
        when(gateway.send(any())).thenReturn(
                new TelegramDeliveryResult.Retryable("telegram_429", Duration.ofMinutes(2)));
        when(outbox.markRetry(7L, LEASE, "telegram_429", NOW.plus(Duration.ofMinutes(2)), NOW))
                .thenReturn(true);
        worker.poll();
        var order = org.mockito.Mockito.inOrder(outbox, heartbeat);
        order.verify(outbox).markRetry(
                7L, LEASE, "telegram_429", NOW.plus(Duration.ofMinutes(2)), NOW);
        order.verify(heartbeat).success(NOW);
    }

    @Test
    void permanentResultIsBlockedBeforeHeartbeat() {
        arrangeOneClaim();
        when(gateway.send(any())).thenReturn(
                new TelegramDeliveryResult.PermanentFailure("telegram_permanent_403"));
        when(outbox.markBlocked(7L, LEASE, "telegram_permanent_403", NOW))
                .thenReturn(true);
        worker.poll();
        var order = org.mockito.Mockito.inOrder(outbox, heartbeat);
        order.verify(outbox).markBlocked(7L, LEASE, "telegram_permanent_403", NOW);
        order.verify(heartbeat).success(NOW);
    }

    @Test
    void privacyBlockedProjectionIsNeverSent() {
        arrangeOneClaim();
        when(outbox.reloadDeliverable(7L, LEASE, PRIVACY_CUTOFF))
                .thenReturn(Optional.empty());
        worker.poll();
        verify(gateway, never()).send(any());
        verify(heartbeat).success(NOW);
    }

    private void arrangeOneClaim() {
        TelegramLeadMessage message = new TelegramLeadMessage(9L,
                UUID.fromString("22222222-2222-4222-8222-222222222222"),
                "Тест", "79990000000", null, "/service/", "repair", NOW);
        ClaimedDelivery claim = new ClaimedDelivery(
                7L, 9L, LEASE, 1, NOW.plus(Duration.ofMinutes(2)), message);
        when(outbox.recoverExpiredAndClaimDue(
                NOW, PRIVACY_CUTOFF, 10, Duration.ofMinutes(2)))
                .thenReturn(List.of(claim));
        when(outbox.reloadDeliverable(7L, LEASE, PRIVACY_CUTOFF))
                .thenReturn(Optional.of(message));
    }
}
```

Integration tests seed deterministic due rows and assert: order by `next_attempt_at,id`; maximum 10; two simultaneous workers receive disjoint IDs; claim commits before a gateway fake observes `TransactionSynchronizationManager.isActualTransactionActive() == false`; attempt increments; two-minute lease; success to delivered; 429/retry-after, 5xx, timeout, and network to retry; non-429 4xx to blocked; expired lease to retry and reclaim; stale lease token cannot mark; application restart recovers; privacy-aged lead is not claimed; an empty successful poll advances heartbeat; retry/blocked decisions advance heartbeat only after their state writes return true; queue metrics have bounded tags and no PII.

Create the concurrency and recovery test with executable setup rather than
inventing a harness during implementation:

```java
package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.andrew.website.testing.PostgresTestConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class TwoWorkerClaimIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-01-30T00:00:00Z");
    @Autowired OutboxRepository outbox;
    @Autowired JdbcClient jdbc;

    @BeforeEach
    void clean() {
        jdbc.sql("delete from telegram_outbox").update();
        jdbc.sql("delete from leads").update();
    }

    @Test
    void twoWorkersClaimDisjointBatchesAndCommitBeforeReturning() throws Exception {
        for (int index = 0; index < 20; index++) seedDueLead(index, NOW.minusSeconds(60));
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> claimAfter(barrier));
            var second = executor.submit(() -> claimAfter(barrier));
            List<ClaimedDelivery> left = first.get();
            List<ClaimedDelivery> right = second.get();
            assertThat(left).hasSize(10);
            assertThat(right).hasSize(10);
            var ids = new HashSet<Long>();
            left.forEach(value -> assertThat(ids.add(value.outboxId())).isTrue());
            right.forEach(value -> assertThat(ids.add(value.outboxId())).isTrue());
            assertThat(ids).hasSize(20);
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        }
    }

    @Test
    void expiredLeaseIsRecoveredAndReclaimedAfterRestartBoundary() {
        long outboxId = seedDueLead(1, NOW.minusSeconds(60));
        ClaimedDelivery first = outbox.recoverExpiredAndClaimDue(
                NOW, NOW.minus(Duration.ofDays(29)), 10, Duration.ofMinutes(2)).getFirst();
        List<ClaimedDelivery> reclaimed = outbox.recoverExpiredAndClaimDue(
                NOW.plus(Duration.ofMinutes(3)), NOW.minus(Duration.ofDays(29)),
                10, Duration.ofMinutes(2));
        assertThat(reclaimed).extracting(ClaimedDelivery::outboxId).containsExactly(outboxId);
        assertThat(reclaimed.getFirst().leaseToken()).isNotEqualTo(first.leaseToken());
        assertThat(reclaimed.getFirst().attemptCount()).isEqualTo(2);
        assertThat(outbox.markDelivered(outboxId, first.leaseToken(), NOW.plusSeconds(181)))
                .isFalse();
    }

    private List<ClaimedDelivery> claimAfter(CyclicBarrier barrier) throws Exception {
        barrier.await();
        return outbox.recoverExpiredAndClaimDue(
                NOW, NOW.minus(Duration.ofDays(29)), 10, Duration.ofMinutes(2));
    }

    private long seedDueLead(int index, Instant nextAttemptAt) {
        long leadId = jdbc.sql("""
                insert into leads(request_id,payload_fingerprint,name,phone,source_path,
                                  intent,consented_at,created_at)
                values (:requestId,decode(repeat('00',32),'hex'),'Тест','79990000000',
                        '/service/','repair',:createdAt,:createdAt)
                returning id
                """)
                .param("requestId", new UUID(0L, index + 1L))
                .param("createdAt", NOW.minus(Duration.ofDays(1)))
                .query(Long.class).single();
        return jdbc.sql("""
                insert into telegram_outbox(lead_id,state,next_attempt_at,created_at,updated_at)
                values (:leadId,'pending',:nextAttemptAt,:createdAt,:createdAt)
                returning id
                """)
                .param("leadId", leadId)
                .param("nextAttemptAt", nextAttemptAt)
                .param("createdAt", NOW.minus(Duration.ofDays(1)))
                .query(Long.class).single();
    }
}
```

Add exact telemetry contract code:

```java
package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TelegramMetricsTest {
    @Test
    void deliveryTagsAreBoundedAndContainNoLeadDimensions() {
        var registry = new SimpleMeterRegistry();
        var metrics = new TelegramMetrics(registry);
        metrics.delivery("retry", "telegram_429");
        metrics.delivery("blocked", "privacy_expired");
        metrics.delivery("delivered", "success");
        var meters = registry.find("andrew.telegram.delivery").meters();
        assertThat(meters).hasSize(3);
        assertThat(meters).allSatisfy(meter -> {
            Set<String> keys = meter.getId().getTags().stream()
                    .map(tag -> tag.getKey()).collect(java.util.stream.Collectors.toSet());
            assertThat(keys).containsExactlyInAnyOrder("outcome", "reason");
            assertThat(keys).doesNotContain(
                    "requestId", "leadId", "name", "phone", "comment", "path");
        });
    }

    @Test
    void unsupportedTagsFailBeforeMeterCreation() {
        var registry = new SimpleMeterRegistry();
        var metrics = new TelegramMetrics(registry);
        org.assertj.core.api.Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> metrics.delivery("retry", "raw_remote_text"));
        assertThat(registry.find("andrew.telegram.delivery").meters()).isEmpty();
    }
}
```

`TelegramMetrics` has exact method `void delivery(String outcome, String reason)`
and immutable allowlists `delivered|retry|blocked` and
`success|network|telegram_429|telegram_4xx|telegram_5xx|lease_expired|privacy_expired`;
it throws `IllegalArgumentException("Unsupported Telegram metric tag")` before
calling the registry for any other value.
The worker persists the bounded detailed code `telegram_permanent_<status>` in
`last_error_code` but always maps every such permanent result to the fixed
metric reason `telegram_4xx`; raw status-specific values are never metric tags.

Add a `TelegramWorkerIntegrationTest` using the same PostgreSQL configuration,
`seedDueLead`, and `OutputCaptureExtension`. Its `privacyAgedLeadIsNeverSent`
inserts a lead with `created_at = NOW.minus(Duration.ofDays(29))`, calls `poll()`,
and asserts `verify(gateway, never()).send(any())`, state `pending` before the
retention task, and captured output does not contain `Тест` or `79990000000`.
Its parameterized `successAndEveryGatewayFailurePersistExpectedState` uses
`@MethodSource("deliveryOutcomes")`; the stream contains exact expected tuples:
`Delivered -> delivered/null`, `Retryable("telegram_429", 120s) -> retry/telegram_429`,
`Retryable("telegram_5xx", null) -> retry/telegram_5xx`,
`Retryable("network", null) -> retry/network`, and
`PermanentFailure("telegram_permanent_403") -> blocked/telegram_permanent_403`.
For every tuple, query by outbox ID and assert exact state/error, cleared lease,
attempt count `1`, and `next_attempt_at` from `RetryPolicy`; assert the captured
output contains neither lead field. These assertions are mandatory companions
to the executable unit/concurrency/metrics code above and may not be replaced by
unasserted mock calls.

Run: `./mvnw -B -Dtest=RetryPolicyTest,TelegramWorkerHeartbeatTest,TelegramWorkerIntegrationTest,TwoWorkerClaimIntegrationTest test`

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
    List<ClaimedDelivery> recoverExpiredAndClaimDue(
            Instant now, Instant privacyCutoff, int limit, Duration lease);
    Optional<TelegramLeadMessage> reloadDeliverable(long outboxId, UUID leaseToken, Instant privacyCutoff);
    boolean markDelivered(long outboxId, UUID leaseToken, Instant now);
    boolean markRetry(long outboxId, UUID leaseToken, String code, Instant nextAttemptAt, Instant now);
    boolean markBlocked(long outboxId, UUID leaseToken, String code, Instant now);
}
```

`JdbcOutboxRepository.recoverExpiredAndClaimDue` is `@Transactional`. In that
single transaction it first changes expired processing rows to due retry with
`lease_expired`, then selects `pending` and due `retry` rows joined to
non-anonymized leads with `created_at > :privacyCutoff`, orders
`o.next_attempt_at, o.id`, limits 10, and uses `FOR UPDATE OF o SKIP LOCKED`.
It assigns a different random UUID token per claimed row, increments
`attempt_count`, sets `processing`, lease expiry, and `updated_at`, then returns
the lead projections. Mark methods update only
`where id=:id and state='processing' and lease_token=:leaseToken`.

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
        var claims = outbox.recoverExpiredAndClaimDue(
                now, now.minus(PRIVACY_THRESHOLD), 10, Duration.ofMinutes(2));
        for (ClaimedDelivery claim : claims) {
            deliver(claim);
        }
        heartbeat.success(clock.instant());
    }

    private void deliver(ClaimedDelivery claim) {
        Instant now = clock.instant();
        outbox.reloadDeliverable(claim.outboxId(), claim.leaseToken(), now.minus(PRIVACY_THRESHOLD))
                .ifPresent(message -> apply(claim, gateway.send(message), clock.instant()));
    }

    private void apply(ClaimedDelivery claim, TelegramDeliveryResult result, Instant now) {
        boolean persisted;
        if (result instanceof TelegramDeliveryResult.Delivered) {
            persisted = outbox.markDelivered(claim.outboxId(), claim.leaseToken(), now);
        } else if (result instanceof TelegramDeliveryResult.Retryable retryable) {
            Duration delay = retryPolicy.delay(claim.attemptCount(), retryable.retryAfter());
            persisted = outbox.markRetry(
                    claim.outboxId(), claim.leaseToken(), retryable.code(), now.plus(delay), now);
        } else if (result instanceof TelegramDeliveryResult.PermanentFailure failure) {
            persisted = outbox.markBlocked(claim.outboxId(), claim.leaseToken(), failure.code(), now);
        } else {
            throw new IllegalStateException("Unknown Telegram delivery result");
        }
        if (!persisted) {
            throw new IllegalStateException("Outbox state transition was not persisted");
        }
    }
}
```

`heartbeat.success` is deliberately the final line of a successful `poll()`. Expected Telegram outcomes are not poll failures when their state transition returns true. An exception from recovery, claim, reload, gateway send, or state persistence propagates to the scheduler boundary; a false compare-and-set state update becomes the fixed PII-free exception shown above. Neither case reaches the heartbeat.

Record counters through a `TelegramMetrics` wrapper with meter `andrew.telegram.delivery`, tag `outcome=delivered|retry|blocked`, and bounded `reason=success|network|telegram_429|telegram_4xx|telegram_5xx|lease_expired|privacy_expired`. Queue depth uses only the five state values. Do not tag path, request ID, raw status text, exception, or message.

Run: `./mvnw -B -Dtest=RetryPolicyTest,TelegramWorkerHeartbeatTest,TelegramWorkerIntegrationTest,TwoWorkerClaimIntegrationTest test`

Expected: PASS for all state, ordering, concurrency, transaction-boundary, retry, recovery, privacy, and telemetry assertions.

- [ ] **Step 4: REFACTOR and complete verification**

Factor SQL row mapping and result application below 50 lines, add the accepted crash-after-send test showing a recovered duplicate with the same request ID, then run `./mvnw -B verify`. Expected: PASS with PostgreSQL 17, at least 80% coverage, no sleeping tests, and captured logs/metrics free of PII.

- [ ] **Step 5: Commit**

```bash
git add src
git commit -m "feat(telegram-worker): add leased outbox delivery"
```
