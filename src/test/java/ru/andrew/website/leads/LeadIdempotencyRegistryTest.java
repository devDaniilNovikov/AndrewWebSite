package ru.andrew.website.leads;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.andrew.website.testing.MutableClock;

class LeadIdempotencyRegistryTest {
    private final MutableClock clock = new MutableClock(
            Instant.parse("2026-09-23T00:00:00Z"), ZoneOffset.UTC);
    private final LeadIdempotencyRegistry registry = new LeadIdempotencyRegistry(clock);

    @Test
    void unknownRequestHasNoDeliveredFingerprint() {
        assertThat(registry.find(UUID.randomUUID())).isEmpty();
    }

    @Test
    void rememberedFingerprintIsFoundUntilItsRetentionEnds() {
        UUID requestId = UUID.randomUUID();
        LeadFingerprint fingerprint = fingerprint(1);

        registry.remember(requestId, fingerprint);
        clock.advance(LeadIdempotencyRegistry.RETENTION.minusNanos(1));

        assertThat(registry.find(requestId)).containsSame(fingerprint);

        clock.advance(Duration.ofNanos(1));

        assertThat(registry.find(requestId)).isEmpty();
    }

    @Test
    void rememberingAgainReplacesTheFingerprintAndRestartsItsRetention() {
        UUID requestId = UUID.randomUUID();
        registry.remember(requestId, fingerprint(1));
        clock.advance(Duration.ofMinutes(30));
        LeadFingerprint replacement = fingerprint(2);

        registry.remember(requestId, replacement);
        clock.advance(Duration.ofMinutes(45));

        assertThat(registry.find(requestId)).containsSame(replacement);
        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    void expiredEntriesArePurgedOnTheNextWrite() {
        registry.remember(UUID.randomUUID(), fingerprint(1));
        registry.remember(UUID.randomUUID(), fingerprint(2));
        clock.advance(LeadIdempotencyRegistry.RETENTION);

        registry.remember(UUID.randomUUID(), fingerprint(3));

        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    void fullRegistryEvictsTheOldestEntry() {
        UUID oldest = UUID.randomUUID();
        registry.remember(oldest, fingerprint(1));
        for (int index = 1; index < LeadIdempotencyRegistry.CAPACITY; index++) {
            registry.remember(UUID.randomUUID(), fingerprint(2));
        }
        UUID newest = UUID.randomUUID();

        registry.remember(newest, fingerprint(3));

        assertThat(registry.size()).isEqualTo(LeadIdempotencyRegistry.CAPACITY);
        assertThat(registry.find(oldest)).isEmpty();
        assertThat(registry.find(newest)).isPresent();
    }

    private static LeadFingerprint fingerprint(int seed) {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) seed);
        return new LeadFingerprint(bytes);
    }
}
