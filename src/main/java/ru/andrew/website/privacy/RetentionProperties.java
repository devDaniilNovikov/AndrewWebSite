package ru.andrew.website.privacy;

import java.time.Duration;
import java.time.Period;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.privacy.retention")
public record RetentionProperties(
        Duration anonymizeAfter,
        Duration hardLimit,
        Period deleteAfter,
        Duration pollInterval,
        int batchSize) {
    static final int MAX_BATCH_SIZE = 1_000;
    private static final String INVALID_SETTINGS =
            "retention settings violate the privacy contract";

    public RetentionProperties {
        if (!Duration.ofDays(29).equals(anonymizeAfter)) {
            throw invalidSettings();
        }
        if (!Duration.ofDays(30).equals(hardLimit)) {
            throw invalidSettings();
        }
        if (!Period.ofMonths(12).equals(deleteAfter)) {
            throw invalidSettings();
        }
        if (!Duration.ofHours(1).equals(pollInterval)) {
            throw invalidSettings();
        }
        if (batchSize < 1) {
            throw invalidSettings();
        }
        if (batchSize > MAX_BATCH_SIZE) {
            throw invalidSettings();
        }
    }

    private static IllegalArgumentException invalidSettings() {
        return new IllegalArgumentException(INVALID_SETTINGS);
    }
}
