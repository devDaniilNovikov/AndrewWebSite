package ru.andrew.website.privacy;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public final class RetentionMetrics {
    private final Counter anonymized;
    private final Counter deleted;
    private final RetentionHeartbeat heartbeat;
    private final Clock clock;

    public RetentionMetrics(
            MeterRegistry registry,
            RetentionHeartbeat heartbeat,
            Clock clock) {
        this.anonymized =
                Counter.builder("andrew.privacy.anonymized")
                        .register(registry);
        this.deleted =
                Counter.builder("andrew.privacy.deleted")
                        .register(registry);
        this.heartbeat = heartbeat;
        this.clock = clock;
        Gauge.builder(
                        "andrew.privacy.last_success.age",
                        this,
                        RetentionMetrics::lastSuccessAgeSeconds)
                .register(registry);
    }

    public void anonymized(int count) {
        anonymized.increment(count);
    }

    public void deleted(int count) {
        deleted.increment(count);
    }

    private double lastSuccessAgeSeconds() {
        Instant reference =
                heartbeat.lastSuccess().orElse(heartbeat.startedAt());
        Duration age = Duration.between(reference, clock.instant());
        return Math.max(0.0, age.toMillis() / 1_000.0);
    }
}
