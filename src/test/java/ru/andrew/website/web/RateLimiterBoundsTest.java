package ru.andrew.website.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.andrew.website.testing.MutableClock;

class RateLimiterBoundsTest {
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @ParameterizedTest
    @MethodSource("invalidTokenBucketBounds")
    void tokenBucketRejectsEveryNonPositiveBound(int capacity, Duration refillPeriod) {
        assertThatThrownBy(() -> new TokenBucket(capacity, refillPeriod, clock()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("capacity and refill period must be positive");
    }

    @Test
    void tokenBucketHasNoRetryDelayWhileCapacityRemains() {
        TokenBucket bucket = new TokenBucket(2, Duration.ofMinutes(1), clock());

        assertThat(bucket.tryAcquire()).isTrue();
        assertThat(bucket.retryAfter()).isZero();
    }

    @ParameterizedTest
    @MethodSource("invalidSlidingWindowBounds")
    void slidingWindowRejectsEveryNonPositiveBound(int limit, Duration window) {
        assertThatThrownBy(() -> new SlidingWindowRateLimiter(limit, window, clock()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit and window must be positive");
    }

    @Test
    void slidingWindowHasNoRetryDelayWhileCapacityRemains() {
        SlidingWindowRateLimiter limiter =
                new SlidingWindowRateLimiter(2, Duration.ofMinutes(1), clock());

        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.retryAfter()).isZero();
    }

    @ParameterizedTest
    @MethodSource("invalidClientLimiterBounds")
    void clientLimiterRejectsEveryNonPositiveBound(
            int maxClients,
            Duration idleTtl,
            int clientCapacity,
            Duration clientRefill) {
        MutableClock clock = clock();
        SlidingWindowRateLimiter global =
                new SlidingWindowRateLimiter(60, Duration.ofMinutes(1), clock);

        assertThatThrownBy(() -> new ClientRateLimiter(
                        clock,
                        global,
                        maxClients,
                        idleTtl,
                        clientCapacity,
                        clientRefill))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("rate-limit bounds must be positive");
    }

    private static Stream<Arguments> invalidTokenBucketBounds() {
        return Stream.of(
                Arguments.of(0, Duration.ofMinutes(1)),
                Arguments.of(1, Duration.ZERO),
                Arguments.of(1, Duration.ofSeconds(-1)),
                Arguments.of(1, Duration.ofNanos(1)));
    }

    private static Stream<Arguments> invalidSlidingWindowBounds() {
        return Stream.of(
                Arguments.of(0, Duration.ofMinutes(1)),
                Arguments.of(1, Duration.ZERO),
                Arguments.of(1, Duration.ofSeconds(-1)),
                Arguments.of(1, Duration.ofNanos(1)));
    }

    private static Stream<Arguments> invalidClientLimiterBounds() {
        Duration validIdle = Duration.ofHours(1);
        Duration validRefill = Duration.ofMinutes(1);
        return Stream.of(
                Arguments.of(0, validIdle, 5, validRefill),
                Arguments.of(10, validIdle, 0, validRefill),
                Arguments.of(10, Duration.ZERO, 5, validRefill),
                Arguments.of(10, Duration.ofSeconds(-1), 5, validRefill),
                Arguments.of(10, Duration.ofNanos(1), 5, validRefill),
                Arguments.of(10, validIdle, 5, Duration.ZERO),
                Arguments.of(10, validIdle, 5, Duration.ofSeconds(-1)),
                Arguments.of(10, validIdle, 5, Duration.ofNanos(1)));
    }

    private static MutableClock clock() {
        return new MutableClock(START, ZoneOffset.UTC);
    }
}
