package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
class TelegramWorkerTest {
    private static final Instant NOW = Instant.parse("2026-01-30T00:00:00Z");
    private static final Instant PRIVACY_CUTOFF =
            NOW.minus(Duration.ofDays(29));
    private static final UUID LEASE =
            UUID.fromString("11111111-1111-4111-8111-111111111111");

    @Mock
    OutboxRepository outbox;

    @Mock
    TelegramGateway gateway;

    @Mock
    WorkerHeartbeat heartbeat;

    @Mock
    TelegramMetrics metrics;

    private TelegramWorker worker;

    @BeforeEach
    void setUp() {
        TelegramWorkerProperties properties = new TelegramWorkerProperties(
                Duration.ofSeconds(15),
                10,
                Duration.ofMinutes(2),
                Duration.ofSeconds(30),
                Duration.ofHours(6));
        worker = new TelegramWorker(
                outbox,
                gateway,
                new RetryPolicy(properties.retryInitial(), properties.retryMaximum()),
                heartbeat,
                metrics,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void emptySuccessfulPollAdvancesHeartbeat() {
        when(outbox.recoverExpiredAndClaimDueWithStats(
                        NOW, PRIVACY_CUTOFF, 10, Duration.ofMinutes(2)))
                .thenReturn(new ClaimBatch(List.of(), 0));

        worker.poll();

        verify(heartbeat).success(NOW);
        verify(gateway, never()).send(any());
    }

    @Test
    void privacyInvalidatedReloadIsSkippedAndPollSucceeds() {
        arrangeClaim();
        when(outbox.reloadDeliverable(7L, LEASE, PRIVACY_CUTOFF))
                .thenReturn(Optional.empty());
        when(outbox.resolvePrivacyInvalidation(
                        7L, LEASE, PRIVACY_CUTOFF, NOW))
                .thenReturn(true);

        worker.poll();

        verify(gateway, never()).send(any());
        var order = inOrder(outbox, metrics, heartbeat);
        order.verify(outbox).resolvePrivacyInvalidation(
                7L, LEASE, PRIVACY_CUTOFF, NOW);
        order.verify(metrics).delivery("blocked", "privacy_expired");
        order.verify(heartbeat).success(NOW);
    }

    @Test
    void staleReloadDoesNotAdvanceHeartbeat() {
        arrangeClaim();
        when(outbox.reloadDeliverable(7L, LEASE, PRIVACY_CUTOFF))
                .thenReturn(Optional.empty());
        when(outbox.resolvePrivacyInvalidation(
                        7L, LEASE, PRIVACY_CUTOFF, NOW))
                .thenReturn(false);

        assertFailureWithoutHeartbeat();

        verify(gateway, never()).send(any());
        verify(metrics, never()).delivery(any(), any());
    }

    @Test
    void expiredClaimIsRejectedBeforeReloadOrGateway() {
        when(outbox.recoverExpiredAndClaimDueWithStats(
                        NOW, PRIVACY_CUTOFF, 10, Duration.ofMinutes(2)))
                .thenReturn(new ClaimBatch(
                        List.of(new ClaimedDelivery(
                                7L,
                                9L,
                                LEASE,
                                1,
                                NOW,
                                message())),
                        0));

        assertFailureWithoutHeartbeat();

        verify(outbox, never()).reloadDeliverable(any(Long.class), any(), any());
        verify(gateway, never()).send(any());
        verify(metrics, never()).delivery(any(), any());
    }

    @Test
    void recoveredLeasesEmitBoundedMetricsBeforeHeartbeat() {
        when(outbox.recoverExpiredAndClaimDueWithStats(
                        NOW, PRIVACY_CUTOFF, 10, Duration.ofMinutes(2)))
                .thenReturn(new ClaimBatch(List.of(), 2));

        worker.poll();

        var order = inOrder(metrics, heartbeat);
        order.verify(metrics, times(2))
                .delivery("retry", "lease_expired");
        verify(heartbeat).success(NOW);
    }

    @Test
    void deliveredStateAndMetricPrecedeHeartbeat() {
        arrangeDeliverable();
        when(gateway.send(any())).thenReturn(new TelegramDeliveryResult.Delivered());
        when(outbox.markDelivered(7L, LEASE, NOW)).thenReturn(true);

        worker.poll();

        var order = inOrder(outbox, metrics, heartbeat);
        order.verify(outbox).markDelivered(7L, LEASE, NOW);
        order.verify(metrics).delivery("delivered", "success");
        order.verify(heartbeat).success(NOW);
    }

    @Test
    void retryableOutcomesPersistAndUseFixedMetricReasons() {
        assertRetry(
                new TelegramDeliveryResult.Retryable(
                        "telegram_429", Duration.ofMinutes(2)),
                "telegram_429",
                NOW.plus(Duration.ofMinutes(2)));
        assertRetry(
                new TelegramDeliveryResult.Retryable("telegram_5xx", null),
                "telegram_5xx",
                NOW.plusSeconds(30));
        assertRetry(
                new TelegramDeliveryResult.Retryable("network", null),
                "network",
                NOW.plusSeconds(30));
        assertRetry(
                new TelegramDeliveryResult.Retryable(
                        "telegram_unexpected", null),
                "telegram_unexpected",
                NOW.plusSeconds(30));
    }

    @Test
    void permanentStatusPersistsDetailedCodeButUsesFixedMetricReason() {
        arrangeDeliverable();
        when(gateway.send(any())).thenReturn(
                new TelegramDeliveryResult.PermanentFailure(
                        "telegram_permanent_403"));
        when(outbox.markBlocked(
                        7L, LEASE, "telegram_permanent_403", NOW))
                .thenReturn(true);

        worker.poll();

        var order = inOrder(outbox, metrics, heartbeat);
        order.verify(outbox).markBlocked(
                7L, LEASE, "telegram_permanent_403", NOW);
        order.verify(metrics).delivery("blocked", "telegram_4xx");
        order.verify(heartbeat).success(NOW);
    }

    @Test
    void claimReloadGatewayAndStateWriteFailuresNeverAdvanceHeartbeat() {
        when(outbox.recoverExpiredAndClaimDueWithStats(
                        any(), any(), any(Integer.class), any()))
                .thenThrow(new IllegalStateException("fictional-database-failure"));
        assertFailureWithoutHeartbeat();

        org.mockito.Mockito.reset(outbox, gateway, heartbeat, metrics);
        arrangeClaim();
        when(outbox.reloadDeliverable(any(Long.class), any(), any()))
                .thenThrow(new IllegalStateException("fictional-reload-failure"));
        assertFailureWithoutHeartbeat();

        org.mockito.Mockito.reset(outbox, gateway, heartbeat, metrics);
        arrangeDeliverable();
        when(gateway.send(any()))
                .thenThrow(new IllegalStateException("fictional-gateway-failure"));
        assertFailureWithoutHeartbeat();

        org.mockito.Mockito.reset(outbox, gateway, heartbeat, metrics);
        arrangeDeliverable();
        when(gateway.send(any())).thenReturn(new TelegramDeliveryResult.Delivered());
        when(outbox.markDelivered(7L, LEASE, NOW)).thenReturn(false);
        assertFailureWithoutHeartbeat();
    }

    @Test
    void unsupportedGatewayCodeFailsWithoutWritingOrEmittingMetric() {
        arrangeDeliverable();
        when(gateway.send(any())).thenReturn(
                new TelegramDeliveryResult.Retryable(
                        "fictional_dynamic_code", null));

        assertFailureWithoutHeartbeat();

        verify(outbox, never()).markRetry(any(Long.class), any(), any(), any(), any());
        verify(metrics, never()).delivery(any(), any());
    }

    @Test
    void unsupportedPermanentCodeFailsWithoutWritingOrEmittingMetric() {
        arrangeDeliverable();
        when(gateway.send(any())).thenReturn(
                new TelegramDeliveryResult.PermanentFailure(
                        "fictional_dynamic_code"));

        assertFailureWithoutHeartbeat();

        verify(outbox, never()).markBlocked(any(Long.class), any(), any(), any());
        verify(metrics, never()).delivery(any(), any());
    }

    @Test
    void failedRetryAndBlockedCompareAndSetNeverAdvanceHeartbeatOrMetrics() {
        arrangeDeliverable();
        when(gateway.send(any())).thenReturn(
                new TelegramDeliveryResult.Retryable("network", null));
        when(outbox.markRetry(
                        7L, LEASE, "network", NOW.plusSeconds(30), NOW))
                .thenReturn(false);
        assertFailureWithoutHeartbeat();
        verify(metrics, never()).delivery(any(), any());

        org.mockito.Mockito.reset(outbox, gateway, heartbeat, metrics);
        arrangeDeliverable();
        when(gateway.send(any())).thenReturn(
                new TelegramDeliveryResult.PermanentFailure(
                        "telegram_permanent_403"));
        when(outbox.markBlocked(
                        7L, LEASE, "telegram_permanent_403", NOW))
                .thenReturn(false);
        assertFailureWithoutHeartbeat();
        verify(metrics, never()).delivery(any(), any());
    }

    private void assertRetry(
            TelegramDeliveryResult result,
            String reason,
            Instant nextAttemptAt) {
        org.mockito.Mockito.reset(outbox, gateway, heartbeat, metrics);
        arrangeDeliverable();
        when(gateway.send(any())).thenReturn(result);
        when(outbox.markRetry(
                        7L, LEASE, reason, nextAttemptAt, NOW))
                .thenReturn(true);

        worker.poll();

        var order = inOrder(outbox, metrics, heartbeat);
        order.verify(outbox).markRetry(
                7L, LEASE, reason, nextAttemptAt, NOW);
        order.verify(metrics).delivery("retry", reason);
        order.verify(heartbeat).success(NOW);
    }

    private void assertFailureWithoutHeartbeat() {
        assertThatThrownBy(worker::poll).isInstanceOf(RuntimeException.class);
        verify(heartbeat, never()).success(any());
    }

    private void arrangeClaim() {
        when(outbox.recoverExpiredAndClaimDueWithStats(
                        NOW, PRIVACY_CUTOFF, 10, Duration.ofMinutes(2)))
                .thenReturn(new ClaimBatch(List.of(claim()), 0));
    }

    private void arrangeDeliverable() {
        arrangeClaim();
        when(outbox.reloadDeliverable(7L, LEASE, PRIVACY_CUTOFF))
                .thenReturn(Optional.of(message()));
    }

    private static ClaimedDelivery claim() {
        return new ClaimedDelivery(
                7L,
                9L,
                LEASE,
                1,
                NOW.plus(Duration.ofMinutes(2)),
                message());
    }

    private static TelegramLeadMessage message() {
        return new TelegramLeadMessage(
                9L,
                UUID.fromString("22222222-2222-4222-8222-222222222222"),
                "Fictional User",
                "70000000000",
                null,
                "/fictional/",
                "repair",
                NOW.minus(Duration.ofDays(1)));
    }
}
