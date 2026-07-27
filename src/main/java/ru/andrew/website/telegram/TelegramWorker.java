package ru.andrew.website.telegram;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;

public final class TelegramWorker {
    private static final Duration PRIVACY_THRESHOLD =
            Duration.ofDays(29);
    private static final String STATE_WRITE_FAILURE =
            "Outbox state transition was not persisted";
    private static final String UNSUPPORTED_RESULT =
            "Unsupported Telegram delivery result code";
    private static final String EXPIRED_LEASE =
            "Outbox delivery window expired before send";

    private final OutboxRepository outbox;
    private final TelegramGateway gateway;
    private final RetryPolicy retryPolicy;
    private final WorkerHeartbeat heartbeat;
    private final TelegramMetrics metrics;
    private final TelegramWorkerProperties properties;
    private final TelegramDeliveryWindow deliveryWindow;
    private final Clock clock;

    public TelegramWorker(
            OutboxRepository outbox,
            TelegramGateway gateway,
            RetryPolicy retryPolicy,
            WorkerHeartbeat heartbeat,
            TelegramMetrics metrics,
            TelegramWorkerProperties properties,
            TelegramDeliveryWindow deliveryWindow,
            Clock clock) {
        this.outbox = outbox;
        this.gateway = gateway;
        this.retryPolicy = retryPolicy;
        this.heartbeat = heartbeat;
        this.metrics = metrics;
        this.properties = properties;
        this.deliveryWindow = deliveryWindow;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.telegram.worker.poll-interval:15s}")
    public void poll() {
        Instant now = clock.instant();
        ClaimBatch batch = outbox.recoverExpiredAndClaimDueWithStats(
                now,
                now.minus(PRIVACY_THRESHOLD),
                properties.batchSize(),
                properties.lease());
        for (int index = 0;
                index < batch.recoveredLeaseCount();
                index++) {
            metrics.delivery("retry", "lease_expired");
        }
        for (ClaimedDelivery claim : batch.deliveries()) {
            deliver(claim);
        }
        heartbeat.success(clock.instant());
    }

    private void deliver(ClaimedDelivery claim) {
        Instant observedAt = clock.instant();
        requireCanStart(
                observedAt,
                deliveryWindow.leaseLatestStart(claim));
        Instant privacyCutoff =
                deliveryWindow.privacyCutoff(observedAt);
        var message = outbox.reloadDeliverable(
                claim.outboxId(),
                claim.leaseToken(),
                observedAt,
                privacyCutoff);
        if (message.isEmpty()) {
            resolvePrivacyInvalidation(
                    claim, privacyCutoff, observedAt);
            return;
        }
        TelegramLeadMessage reloaded = message.orElseThrow();
        Instant beforeSend = clock.instant();
        if (!deliveryWindow.canStart(
                beforeSend,
                deliveryWindow.privacyLatestStart(reloaded))) {
            resolvePrivacyInvalidation(
                    claim,
                    deliveryWindow.privacyCutoff(beforeSend),
                    beforeSend);
            return;
        }
        Instant latestStart =
                deliveryWindow.latestStart(claim, reloaded);
        requireCanStart(beforeSend, latestStart);
        apply(
                claim,
                gateway.send(reloaded, latestStart),
                clock.instant());
    }

    private void resolvePrivacyInvalidation(
            ClaimedDelivery claim,
            Instant privacyCutoff,
            Instant observedAt) {
        requirePersisted(outbox.resolvePrivacyInvalidation(
                claim.outboxId(),
                claim.leaseToken(),
                privacyCutoff,
                observedAt));
        metrics.delivery("blocked", "privacy_expired");
    }

    private void apply(
            ClaimedDelivery claim,
            TelegramDeliveryResult result,
            Instant now) {
        switch (result) {
            case TelegramDeliveryResult.Delivered ignored ->
                    markDelivered(claim, now);
            case TelegramDeliveryResult.Retryable retryable ->
                    markRetry(claim, retryable, now);
            case TelegramDeliveryResult.PermanentFailure failure ->
                    markBlocked(claim, failure, now);
        }
    }

    private void markDelivered(ClaimedDelivery claim, Instant now) {
        requirePersisted(outbox.markDelivered(
                claim.outboxId(), claim.leaseToken(), now));
        metrics.delivery("delivered", "success");
    }

    private void markRetry(
            ClaimedDelivery claim,
            TelegramDeliveryResult.Retryable retryable,
            Instant now) {
        String reason = retryReason(retryable.code());
        Duration retryAfter = "telegram_429".equals(reason)
                ? retryable.retryAfter()
                : null;
        Duration delay =
                retryPolicy.delay(claim.attemptCount(), retryAfter);
        requirePersisted(outbox.markRetry(
                claim.outboxId(),
                claim.leaseToken(),
                retryable.code(),
                now.plus(delay),
                now));
        metrics.delivery("retry", reason);
    }

    private void markBlocked(
            ClaimedDelivery claim,
            TelegramDeliveryResult.PermanentFailure failure,
            Instant now) {
        if (!failure.code().matches("telegram_permanent_4\\d\\d")) {
            throw new IllegalStateException(UNSUPPORTED_RESULT);
        }
        requirePersisted(outbox.markBlocked(
                claim.outboxId(),
                claim.leaseToken(),
                failure.code(),
                now));
        metrics.delivery("blocked", "telegram_4xx");
    }

    private static String retryReason(String code) {
        return switch (code) {
            case "network",
                    "telegram_429",
                    "telegram_5xx",
                    "telegram_unexpected" -> code;
            default -> throw new IllegalStateException(UNSUPPORTED_RESULT);
        };
    }

    private static void requirePersisted(boolean persisted) {
        if (!persisted) {
            throw new IllegalStateException(STATE_WRITE_FAILURE);
        }
    }

    private void requireCanStart(
            Instant observedAt, Instant latestStart) {
        if (!deliveryWindow.canStart(observedAt, latestStart)) {
            throw new IllegalStateException(EXPIRED_LEASE);
        }
    }
}
