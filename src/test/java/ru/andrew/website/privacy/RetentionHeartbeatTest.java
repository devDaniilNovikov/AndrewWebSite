package ru.andrew.website.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RetentionHeartbeatTest {
    @Test
    void recordsOnlyExplicitSuccessfulPasses() {
        Instant startedAt = Instant.parse("2026-01-30T00:00:00Z");
        Instant succeededAt = startedAt.plusSeconds(5);
        var heartbeat = new RetentionHeartbeat(
                Clock.fixed(startedAt, ZoneOffset.UTC));

        assertThat(heartbeat.startedAt()).isEqualTo(startedAt);
        assertThat(heartbeat.lastSuccess()).isEmpty();

        heartbeat.success(succeededAt);

        assertThat(heartbeat.lastSuccess()).contains(succeededAt);
    }
}
