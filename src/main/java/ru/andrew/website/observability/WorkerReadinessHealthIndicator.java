package ru.andrew.website.observability;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import ru.andrew.website.telegram.WorkerHeartbeat;

@Component("telegramWorkerReadiness")
public final class WorkerReadinessHealthIndicator
        implements HealthIndicator {
    private static final Duration FRESHNESS = Duration.ofSeconds(45);

    private final WorkerHeartbeat heartbeat;
    private final Clock clock;

    public WorkerReadinessHealthIndicator(
            WorkerHeartbeat heartbeat, Clock clock) {
        this.heartbeat = heartbeat;
        this.clock = clock;
    }

    @Override
    public Health health() {
        try {
            Instant reference = heartbeat.lastSuccess()
                    .orElse(heartbeat.startedAt());
            Duration age = Duration.between(reference, clock.instant());
            return age.compareTo(FRESHNESS) <= 0
                    ? Health.up().build()
                    : Health.down().build();
        } catch (RuntimeException unavailable) {
            return Health.down().build();
        }
    }
}
