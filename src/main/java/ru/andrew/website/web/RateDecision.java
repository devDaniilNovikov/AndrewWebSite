package ru.andrew.website.web;

import java.time.Duration;

public record RateDecision(boolean allowed, Duration retryAfter) {
    public static RateDecision allowedDecision() {
        return new RateDecision(true, Duration.ZERO);
    }

    public static RateDecision rejected(Duration retryAfter) {
        return new RateDecision(false, retryAfter);
    }
}
