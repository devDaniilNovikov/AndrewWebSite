package ru.andrew.website.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RetentionMetricsTest {
    private static final Instant NOW =
            Instant.parse("2026-01-30T00:00:30Z");

    @Test
    void publishesOnlyAggregateTaglessPrivacyMeters() {
        var registry = new SimpleMeterRegistry();
        var heartbeat = new RetentionHeartbeat(
                Clock.fixed(NOW.minusSeconds(30), ZoneOffset.UTC));
        var metrics = new RetentionMetrics(
                registry,
                heartbeat,
                Clock.fixed(NOW, ZoneOffset.UTC));

        metrics.anonymized(3);
        metrics.deleted(2);

        assertThat(registry.get("andrew.privacy.anonymized").counter().count())
                .isEqualTo(3.0);
        assertThat(registry.get("andrew.privacy.deleted").counter().count())
                .isEqualTo(2.0);
        assertThat(registry.get("andrew.privacy.last_success.age").gauge().value())
                .isEqualTo(30.0);
        assertThat(registry.getMeters())
                .allSatisfy(meter ->
                        assertThat(meter.getId().getTags()).isEmpty());

        heartbeat.success(NOW.minusSeconds(4));

        assertThat(registry.get("andrew.privacy.last_success.age").gauge().value())
                .isEqualTo(4.0);
    }
}
