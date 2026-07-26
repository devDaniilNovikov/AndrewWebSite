package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.andrew.website.testing.MutableClock;
import ru.andrew.website.testing.PostgresTestConfiguration;

@Tag("database")
@SpringBootTest
@ActiveProfiles("test")
@Import({
        PostgresTestConfiguration.class,
        TelegramWorkerIntegrationTest.ClockConfiguration.class
})
@ExtendWith(OutputCaptureExtension.class)
class TelegramWorkerIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-01-30T00:00:00Z");
    private static final Instant CREATED_AT =
            NOW.minus(Duration.ofDays(1));

    @Autowired
    TelegramWorker worker;

    @Autowired
    OutboxRepository outbox;

    @Autowired
    WorkerHeartbeat heartbeat;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    MeterRegistry meterRegistry;

    @Autowired
    MutableClock clock;

    @MockitoBean
    TelegramGateway gateway;

    @BeforeEach
    void clean() {
        jdbc.sql("delete from telegram_outbox").update();
        jdbc.sql("delete from leads").update();
        clock.setInstant(NOW);
        reset(gateway);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("deliveryOutcomes")
    void gatewayOutcomesPersistExpectedStateAfterCommittedClaim(
            String description,
            TelegramDeliveryResult result,
            OutboxState expectedState,
            String expectedCode,
            Instant expectedNextAttemptAt,
            String metricOutcome,
            String metricReason,
            CapturedOutput output) {
        long outboxId = seedDueLead(1);
        AtomicBoolean transactionActive = new AtomicBoolean(true);
        double before = counterValue(metricOutcome, metricReason);
        when(gateway.send(any())).thenAnswer(invocation -> {
            transactionActive.set(
                    TransactionSynchronizationManager
                            .isActualTransactionActive());
            return result;
        });

        worker.poll();

        assertThat(description).isNotBlank();
        assertThat(transactionActive).isFalse();
        PersistedOutbox persisted = persisted(outboxId);
        assertThat(persisted.state()).isEqualTo(expectedState);
        assertThat(persisted.attemptCount()).isEqualTo(1);
        assertThat(persisted.leaseToken()).isNull();
        assertThat(persisted.leaseUntil()).isNull();
        assertThat(persisted.lastErrorCode()).isEqualTo(expectedCode);
        if (expectedNextAttemptAt != null) {
            assertThat(persisted.nextAttemptAt())
                    .isEqualTo(expectedNextAttemptAt);
        }
        assertThat(counterValue(metricOutcome, metricReason))
                .isEqualTo(before + 1.0);
        assertThat(heartbeat.lastSuccess()).contains(NOW);
        assertThat(output.getAll()).doesNotContain(
                "Fictional Worker User",
                "70000000000",
                "fictional-worker-comment",
                "/fictional-worker/",
                requestId(1).toString());
    }

    static Stream<Arguments> deliveryOutcomes() {
        return Stream.of(
                Arguments.of(
                        "delivered",
                        new TelegramDeliveryResult.Delivered(),
                        OutboxState.delivered,
                        null,
                        null,
                        "delivered",
                        "success"),
                Arguments.of(
                        "429",
                        new TelegramDeliveryResult.Retryable(
                                "telegram_429", Duration.ofMinutes(2)),
                        OutboxState.retry,
                        "telegram_429",
                        NOW.plus(Duration.ofMinutes(2)),
                        "retry",
                        "telegram_429"),
                Arguments.of(
                        "5xx",
                        new TelegramDeliveryResult.Retryable(
                                "telegram_5xx", null),
                        OutboxState.retry,
                        "telegram_5xx",
                        NOW.plusSeconds(30),
                        "retry",
                        "telegram_5xx"),
                Arguments.of(
                        "network or timeout",
                        new TelegramDeliveryResult.Retryable("network", null),
                        OutboxState.retry,
                        "network",
                        NOW.plusSeconds(30),
                        "retry",
                        "network"),
                Arguments.of(
                        "unexpected status",
                        new TelegramDeliveryResult.Retryable(
                                "telegram_unexpected", null),
                        OutboxState.retry,
                        "telegram_unexpected",
                        NOW.plusSeconds(30),
                        "retry",
                        "telegram_unexpected"),
                Arguments.of(
                        "permanent 4xx",
                        new TelegramDeliveryResult.PermanentFailure(
                                "telegram_permanent_403"),
                        OutboxState.blocked,
                        "telegram_permanent_403",
                        null,
                        "blocked",
                        "telegram_4xx"));
    }

    @Test
    void crashAfterSendIsRecoveredAndDuplicatesSameRequestId() {
        long outboxId = seedDueLead(20);
        ClaimedDelivery first = outbox.recoverExpiredAndClaimDue(
                        NOW,
                        NOW.minus(Duration.ofDays(29)),
                        10,
                        Duration.ofMinutes(2))
                .getFirst();
        when(gateway.send(any()))
                .thenReturn(new TelegramDeliveryResult.Delivered());

        gateway.send(first.message());
        clock.advance(Duration.ofMinutes(3));
        worker.poll();

        ArgumentCaptor<TelegramLeadMessage> messages =
                ArgumentCaptor.forClass(TelegramLeadMessage.class);
        verify(gateway, org.mockito.Mockito.times(2)).send(messages.capture());
        List<TelegramLeadMessage> sent = messages.getAllValues();
        assertThat(sent).extracting(TelegramLeadMessage::requestId)
                .containsExactly(first.message().requestId(), first.message().requestId());
        assertThat(persisted(outboxId).state()).isEqualTo(OutboxState.delivered);
        assertThat(persisted(outboxId).attemptCount()).isEqualTo(2);
        assertThat(heartbeat.lastSuccess()).contains(NOW.plus(Duration.ofMinutes(3)));
    }

    private long seedDueLead(int index) {
        long leadId = jdbc.sql("""
                        insert into leads(
                            request_id, payload_fingerprint, name, phone, comment,
                            source_path, intent, consented_at, created_at
                        )
                        values (
                            :requestId, decode(repeat('00', 32), 'hex'),
                            'Fictional Worker User', '70000000000',
                            'fictional-worker-comment', '/fictional-worker/',
                            'repair', :createdAt, :createdAt
                        )
                        returning id
                        """)
                .param("requestId", requestId(index))
                .param("createdAt", CREATED_AT.atOffset(ZoneOffset.UTC))
                .query(Long.class)
                .single();
        return jdbc.sql("""
                        insert into telegram_outbox(
                            lead_id, state, next_attempt_at, created_at, updated_at
                        )
                        values (:leadId, 'pending', :now, :createdAt, :createdAt)
                        returning id
                        """)
                .param("leadId", leadId)
                .param("now", NOW.atOffset(ZoneOffset.UTC))
                .param("createdAt", CREATED_AT.atOffset(ZoneOffset.UTC))
                .query(Long.class)
                .single();
    }

    private PersistedOutbox persisted(long outboxId) {
        return jdbc.sql("""
                        select
                            state,
                            attempt_count,
                            next_attempt_at,
                            lease_token,
                            lease_until,
                            last_error_code
                        from telegram_outbox
                        where id = :id
                        """)
                .param("id", outboxId)
                .query((result, rowNumber) -> new PersistedOutbox(
                        OutboxState.valueOf(result.getString("state")),
                        result.getInt("attempt_count"),
                        result.getObject(
                                        "next_attempt_at",
                                        java.time.OffsetDateTime.class)
                                .toInstant(),
                        result.getObject("lease_token", UUID.class),
                        result.getObject(
                                "lease_until",
                                java.time.OffsetDateTime.class),
                        result.getString("last_error_code")))
                .single();
    }

    private double counterValue(String outcome, String reason) {
        var counter = meterRegistry.find("andrew.telegram.delivery")
                .tag("outcome", outcome)
                .tag("reason", reason)
                .counter();
        return counter == null ? 0.0 : counter.count();
    }

    private static UUID requestId(int index) {
        return new UUID(0L, index + 1L);
    }

    private record PersistedOutbox(
            OutboxState state,
            int attemptCount,
            Instant nextAttemptAt,
            UUID leaseToken,
            Object leaseUntil,
            String lastErrorCode) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfiguration {
        @Bean
        @Primary
        MutableClock workerTestClock() {
            return new MutableClock(NOW, ZoneOffset.UTC);
        }
    }
}
