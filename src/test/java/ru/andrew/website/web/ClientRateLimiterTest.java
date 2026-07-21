package ru.andrew.website.web;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;
import ru.andrew.website.testing.MutableClock;

class ClientRateLimiterTest {
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void sixthImmediateClientRequestWaitsForTheExactRefillBoundary() {
        MutableClock clock = clock();
        ClientRateLimiter limiter = ClientRateLimiter.defaults(clock);

        for (int index = 0; index < 5; index++) {
            assertThat(limiter.tryAcquire("192.0.2.10").allowed()).isTrue();
        }

        assertThat(limiter.tryAcquire("192.0.2.10"))
                .isEqualTo(RateDecision.rejected(Duration.ofMinutes(1)));
        clock.advance(Duration.ofMillis(59_001));
        assertThat(limiter.tryAcquire("192.0.2.10"))
                .isEqualTo(RateDecision.rejected(Duration.ofSeconds(1)));
        clock.advance(Duration.ofMillis(999));
        assertThat(limiter.tryAcquire("192.0.2.10").allowed()).isTrue();
    }

    @Test
    void clientRejectedTrafficDoesNotConsumeGlobalAdmissions() {
        MutableClock clock = clock();
        ClientRateLimiter limiter = ClientRateLimiter.defaults(clock);

        for (int index = 0; index < 5; index++) {
            assertThat(limiter.tryAcquire("192.0.2.10").allowed()).isTrue();
        }
        for (int index = 0; index < 100; index++) {
            assertThat(limiter.tryAcquire("192.0.2.10").allowed()).isFalse();
        }
        for (int index = 0; index < 55; index++) {
            assertThat(limiter.tryAcquire("198.51.100." + index).allowed()).isTrue();
        }

        assertThat(limiter.tryAcquire("203.0.113.1"))
                .isEqualTo(RateDecision.rejected(Duration.ofMinutes(1)));
    }

    @Test
    void globalWindowUsesExactHalfOpenBoundaryAndStaggeredExpirations() {
        MutableClock clock = clock();
        SlidingWindowRateLimiter limiter =
                new SlidingWindowRateLimiter(3, Duration.ofMinutes(1), clock);

        assertThat(limiter.tryAcquire()).isTrue();
        clock.advance(Duration.ofSeconds(30));
        assertThat(limiter.tryAcquire()).isTrue();
        clock.advance(Duration.ofSeconds(29));
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isFalse();
        assertThat(limiter.retryAfter()).isEqualTo(Duration.ofSeconds(1));

        clock.advance(Duration.ofSeconds(1));
        assertThat(limiter.tryAcquire()).isTrue();
        clock.advance(Duration.ofSeconds(29));
        assertThat(limiter.tryAcquire()).isFalse();
        assertThat(limiter.retryAfter()).isEqualTo(Duration.ofSeconds(1));
        clock.advance(Duration.ofSeconds(1));
        assertThat(limiter.tryAcquire()).isTrue();
    }

    @Test
    void clockRollbackDoesNotReopenGlobalOrClientCapacity() {
        MutableClock clock = new MutableClock(
                Instant.parse("2026-01-01T00:01:40Z"), ZoneOffset.UTC);
        SlidingWindowRateLimiter global =
                new SlidingWindowRateLimiter(1, Duration.ofMinutes(1), clock);
        TokenBucket client = new TokenBucket(1, Duration.ofMinutes(1), clock);

        assertThat(global.tryAcquire()).isTrue();
        assertThat(client.tryAcquire()).isTrue();
        clock.setInstant(START);

        assertThat(global.tryAcquire()).isFalse();
        assertThat(global.retryAfter()).isEqualTo(Duration.ofMinutes(1));
        assertThat(client.tryAcquire()).isFalse();
        assertThat(client.retryAfter()).isEqualTo(Duration.ofMinutes(1));

        clock.setInstant(Instant.parse("2026-01-01T00:02:39Z"));
        assertThat(global.retryAfter()).isEqualTo(Duration.ofSeconds(1));
        assertThat(client.retryAfter()).isEqualTo(Duration.ofSeconds(1));
        clock.advance(Duration.ofSeconds(1));
        assertThat(global.tryAcquire()).isTrue();
        assertThat(client.tryAcquire()).isTrue();
    }

    @Test
    void clientCreatedDuringClockRollbackUsesTheLastObservedTimeForRefill() {
        MutableClock clock = new MutableClock(
                Instant.parse("2026-01-01T00:01:40Z"), ZoneOffset.UTC);
        ClientRateLimiter limiter = limiter(
                clock, 10, Duration.ofHours(1), 1, Duration.ofMinutes(1), 10, Duration.ofHours(1));

        clock.setInstant(START);
        assertThat(limiter.tryAcquire("192.0.2.10").allowed()).isTrue();
        clock.advance(Duration.ofMinutes(1));

        assertThat(limiter.tryAcquire("192.0.2.10"))
                .isEqualTo(RateDecision.rejected(Duration.ofMinutes(1)));
        clock.setInstant(Instant.parse("2026-01-01T00:02:39Z"));
        assertThat(limiter.tryAcquire("192.0.2.10"))
                .isEqualTo(RateDecision.rejected(Duration.ofSeconds(1)));
        clock.advance(Duration.ofSeconds(1));
        assertThat(limiter.tryAcquire("192.0.2.10").allowed()).isTrue();
    }

    @Test
    void globalRejectionConsumesTheClientToken() {
        MutableClock clock = clock();
        ClientRateLimiter limiter = limiter(
                clock, 10, Duration.ofHours(1), 1, Duration.ofHours(2), 1, Duration.ofHours(1));

        assertThat(limiter.tryAcquire("192.0.2.1").allowed()).isTrue();
        assertThat(limiter.tryAcquire("192.0.2.2"))
                .isEqualTo(RateDecision.rejected(Duration.ofHours(1)));
        assertThat(limiter.tryAcquire("192.0.2.2"))
                .isEqualTo(RateDecision.rejected(Duration.ofHours(2)));
    }

    @Test
    void idleClientIsEvictedAtTheExactTtlBoundary() {
        MutableClock clock = clock();
        ClientRateLimiter limiter = limiter(
                clock, 2, Duration.ofHours(1), 1, Duration.ofHours(2), 10, Duration.ofSeconds(1));

        assertThat(limiter.tryAcquire("192.0.2.1").allowed()).isTrue();
        clock.advance(Duration.ofHours(1));

        assertThat(limiter.tryAcquire("192.0.2.1").allowed()).isTrue();
    }

    @Test
    void globalRejectionPathStillEnforcesTheLruHardCap() {
        MutableClock clock = clock();
        ClientRateLimiter limiter = limiter(
                clock, 2, Duration.ofDays(1), 1, Duration.ofHours(2), 1, Duration.ofHours(1));

        assertThat(limiter.tryAcquire("192.0.2.1").allowed()).isTrue();
        assertThat(limiter.tryAcquire("192.0.2.2").allowed()).isFalse();
        assertThat(limiter.tryAcquire("192.0.2.3").allowed()).isFalse();

        assertThat(limiter.tryAcquire("192.0.2.1"))
                .isEqualTo(RateDecision.rejected(Duration.ofHours(1)));
    }

    @Test
    void retryDurationsAreCeiledToWholeSeconds() {
        MutableClock globalClock = clock();
        SlidingWindowRateLimiter global =
                new SlidingWindowRateLimiter(1, Duration.ofMillis(1_500), globalClock);
        assertThat(global.tryAcquire()).isTrue();
        assertThat(global.retryAfter()).isEqualTo(Duration.ofSeconds(2));
        globalClock.advance(Duration.ofMillis(501));
        assertThat(global.retryAfter()).isEqualTo(Duration.ofSeconds(1));

        MutableClock clientClock = clock();
        TokenBucket client = new TokenBucket(1, Duration.ofMillis(1_500), clientClock);
        assertThat(client.tryAcquire()).isTrue();
        assertThat(client.retryAfter()).isEqualTo(Duration.ofSeconds(2));
        clientClock.advance(Duration.ofMillis(501));
        assertThat(client.retryAfter()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void concurrentAdmissionsRespectClientAndGlobalCaps() throws Exception {
        MutableClock clock = clock();
        ClientRateLimiter clientLimited = limiter(
                clock, 100, Duration.ofHours(1), 5, Duration.ofMinutes(1), 100, Duration.ofMinutes(1));
        assertThat(concurrentAllowed(clientLimited, 40, ignored -> "192.0.2.1")).isEqualTo(5);

        ClientRateLimiter globallyLimited = limiter(
                clock, 200, Duration.ofHours(1), 1, Duration.ofMinutes(1), 60, Duration.ofMinutes(1));
        assertThat(concurrentAllowed(
                        globallyLimited, 100, index -> "198.51.100." + index))
                .isEqualTo(60);
    }

    private static MutableClock clock() {
        return new MutableClock(START, ZoneOffset.UTC);
    }

    private static ClientRateLimiter limiter(
            MutableClock clock,
            int maxClients,
            Duration idleTtl,
            int clientCapacity,
            Duration clientRefill,
            int globalLimit,
            Duration globalWindow) {
        return new ClientRateLimiter(
                clock,
                new SlidingWindowRateLimiter(globalLimit, globalWindow, clock),
                maxClients,
                idleTtl,
                clientCapacity,
                clientRefill);
    }

    private static long concurrentAllowed(
            ClientRateLimiter limiter, int requestCount, IntFunction<String> address) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>(requestCount);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int index = 0; index < requestCount; index++) {
                int requestIndex = index;
                results.add(executor.submit(() -> {
                    start.await();
                    return limiter.tryAcquire(address.apply(requestIndex)).allowed();
                }));
            }
            start.countDown();

            long allowed = 0;
            for (Future<Boolean> result : results) {
                if (result.get(5, SECONDS)) {
                    allowed++;
                }
            }
            return allowed;
        }
    }
}
