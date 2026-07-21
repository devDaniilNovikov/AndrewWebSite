package ru.andrew.website.web;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Objects;

public final class SlidingWindowRateLimiter {
    private static final long MILLIS_PER_SECOND = 1_000L;

    private final int limit;
    private final long windowMillis;
    private final Clock clock;
    private final ArrayDeque<Long> admittedAt = new ArrayDeque<>();
    private long lastObservedMillis;

    SlidingWindowRateLimiter(int limit, Duration window, Clock clock) {
        Objects.requireNonNull(window, "window");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (limit < 1 || window.isZero() || window.isNegative() || window.toMillis() < 1) {
            throw new IllegalArgumentException("limit and window must be positive");
        }
        this.limit = limit;
        this.windowMillis = window.toMillis();
        this.lastObservedMillis = clock.millis();
    }

    synchronized boolean tryAcquire() {
        long now = monotonicNow();
        evictExpired(now);
        if (admittedAt.size() >= limit) {
            return false;
        }
        admittedAt.addLast(now);
        return true;
    }

    synchronized Duration retryAfter() {
        long now = monotonicNow();
        evictExpired(now);
        if (admittedAt.size() < limit) {
            return Duration.ZERO;
        }
        long remainingMillis = windowMillis - (now - admittedAt.getFirst());
        return ceilToWholeSeconds(remainingMillis);
    }

    private long monotonicNow() {
        lastObservedMillis = Math.max(lastObservedMillis, clock.millis());
        return lastObservedMillis;
    }

    private void evictExpired(long now) {
        while (!admittedAt.isEmpty()
                && now - admittedAt.getFirst() >= windowMillis) {
            admittedAt.removeFirst();
        }
    }

    private static Duration ceilToWholeSeconds(long millis) {
        long seconds = Math.max(1L, Math.ceilDiv(millis, MILLIS_PER_SECOND));
        return Duration.ofSeconds(seconds);
    }
}
