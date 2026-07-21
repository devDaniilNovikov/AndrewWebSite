package ru.andrew.website.web;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

public final class TokenBucket {
    private static final long MILLIS_PER_SECOND = 1_000L;

    private final int capacity;
    private final long refillPeriodMillis;
    private final Clock clock;
    private int tokens;
    private long lastRefillMillis;
    private long lastObservedMillis;

    TokenBucket(int capacity, Duration refillPeriod, Clock clock) {
        this(capacity, refillPeriod, clock, initialMillis(clock));
    }

    TokenBucket(int capacity, Duration refillPeriod, Clock clock, long initialMillis) {
        Objects.requireNonNull(refillPeriod, "refillPeriod");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (capacity < 1
                || refillPeriod.isZero()
                || refillPeriod.isNegative()
                || refillPeriod.toMillis() < 1) {
            throw new IllegalArgumentException("capacity and refill period must be positive");
        }
        this.capacity = capacity;
        this.refillPeriodMillis = refillPeriod.toMillis();
        this.tokens = capacity;
        this.lastRefillMillis = initialMillis;
        this.lastObservedMillis = lastRefillMillis;
    }

    synchronized boolean tryAcquire() {
        refill(monotonicNow());
        if (tokens == 0) {
            return false;
        }
        tokens--;
        return true;
    }

    synchronized Duration retryAfter() {
        long now = monotonicNow();
        refill(now);
        if (tokens > 0) {
            return Duration.ZERO;
        }
        long remainingMillis = refillPeriodMillis - (now - lastRefillMillis);
        long seconds = Math.max(1L, Math.ceilDiv(remainingMillis, MILLIS_PER_SECOND));
        return Duration.ofSeconds(seconds);
    }

    private long monotonicNow() {
        lastObservedMillis = Math.max(lastObservedMillis, clock.millis());
        return lastObservedMillis;
    }

    private static long initialMillis(Clock clock) {
        return Objects.requireNonNull(clock, "clock").millis();
    }

    private void refill(long now) {
        long elapsedMillis = now - lastRefillMillis;
        if (elapsedMillis < refillPeriodMillis) {
            return;
        }

        long elapsedPeriods = elapsedMillis / refillPeriodMillis;
        int missingTokens = capacity - tokens;
        if (elapsedPeriods >= missingTokens) {
            tokens = capacity;
        } else {
            tokens += (int) elapsedPeriods;
        }
        lastRefillMillis = now - (elapsedMillis % refillPeriodMillis);
    }
}
