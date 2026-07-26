package ru.andrew.website.telegram;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public final class TelegramMetrics {
    private static final Set<String> OUTCOMES =
            Set.of("delivered", "retry", "blocked");
    private static final Set<String> REASONS = Set.of(
            "success",
            "network",
            "telegram_429",
            "telegram_4xx",
            "telegram_5xx",
            "telegram_unexpected",
            "lease_expired",
            "privacy_expired");

    private final MeterRegistry registry;
    private final WorkerHeartbeat heartbeat;
    private final Clock clock;

    public TelegramMetrics(
            MeterRegistry registry,
            OutboxRepository outbox,
            WorkerHeartbeat heartbeat,
            Clock clock) {
        this.registry = registry;
        this.heartbeat = heartbeat;
        this.clock = clock;
        registerQueueDepth(outbox);
        Gauge.builder(
                        "andrew.telegram.worker.last_success.age",
                        this,
                        TelegramMetrics::lastSuccessAgeSeconds)
                .register(registry);
    }

    public void delivery(String outcome, String reason) {
        if (!OUTCOMES.contains(outcome) || !REASONS.contains(reason)) {
            throw new IllegalArgumentException(
                    "Unsupported Telegram metric tag");
        }
        Counter.builder("andrew.telegram.delivery")
                .tag("outcome", outcome)
                .tag("reason", reason)
                .register(registry)
                .increment();
    }

    private void registerQueueDepth(OutboxRepository outbox) {
        for (OutboxState state : OutboxState.values()) {
            Gauge.builder(
                            "andrew.telegram.queue.depth",
                            outbox,
                            repository -> repository.countByState(state))
                    .tag("state", state.name())
                    .strongReference(true)
                    .register(registry);
        }
    }

    private double lastSuccessAgeSeconds() {
        Instant reference =
                heartbeat.lastSuccess().orElse(heartbeat.startedAt());
        Duration age = Duration.between(reference, clock.instant());
        return Math.max(0.0, age.toMillis() / 1_000.0);
    }
}
