package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class WorkerHeartbeatTest {
    private static final Instant STARTED =
            Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void startsEmptyAndRetainsLatestImmutableSuccessInstant() {
        WorkerHeartbeat heartbeat =
                new WorkerHeartbeat(Clock.fixed(STARTED, ZoneOffset.UTC));

        assertThat(heartbeat.startedAt()).isEqualTo(STARTED);
        assertThat(heartbeat.lastSuccess()).isEmpty();

        Instant first = STARTED.plusSeconds(1);
        Instant second = STARTED.plusSeconds(2);
        heartbeat.success(first);
        heartbeat.success(second);

        assertThat(heartbeat.lastSuccess()).contains(second);
    }
}
