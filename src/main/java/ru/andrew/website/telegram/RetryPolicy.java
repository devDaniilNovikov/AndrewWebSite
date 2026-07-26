package ru.andrew.website.telegram;

import java.time.Duration;
import java.util.Objects;

public final class RetryPolicy {
    private final Duration initialDelay;
    private final Duration maximumDelay;

    public RetryPolicy(Duration initialDelay, Duration maximumDelay) {
        this.initialDelay = requirePositive(initialDelay, "initialDelay");
        this.maximumDelay = requirePositive(maximumDelay, "maximumDelay");
        if (initialDelay.compareTo(maximumDelay) > 0) {
            throw new IllegalArgumentException(
                    "initialDelay must not exceed maximumDelay");
        }
    }

    public Duration delay(int attempt, Duration retryAfter) {
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be positive");
        }
        Duration exponential = exponentialDelay(attempt);
        if (retryAfter == null) {
            return exponential;
        }
        Duration positiveRetryAfter = requirePositive(retryAfter, "retryAfter");
        return min(max(exponential, positiveRetryAfter), maximumDelay);
    }

    private Duration exponentialDelay(int attempt) {
        Duration delay = initialDelay;
        int remainingDoublings = attempt - 1;
        Duration safeDoublingLimit = maximumDelay.dividedBy(2);
        while (remainingDoublings > 0 && delay.compareTo(maximumDelay) < 0) {
            if (delay.compareTo(safeDoublingLimit) > 0) {
                return maximumDelay;
            }
            delay = delay.multipliedBy(2);
            remainingDoublings--;
        }
        return min(delay, maximumDelay);
    }

    private static Duration requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static Duration min(Duration left, Duration right) {
        return left.compareTo(right) <= 0 ? left : right;
    }

    private static Duration max(Duration left, Duration right) {
        return left.compareTo(right) >= 0 ? left : right;
    }
}
