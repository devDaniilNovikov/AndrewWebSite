package ru.andrew.website.telegram;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.telegram.worker")
public record TelegramWorkerProperties(
        Duration pollInterval,
        int batchSize,
        Duration lease,
        Duration retryInitial,
        Duration retryMaximum) {

    public TelegramWorkerProperties {
        requirePositive(pollInterval, "pollInterval");
        if (batchSize < 1 || batchSize > JdbcOutboxRepository.MAX_CLAIM_SIZE) {
            throw new IllegalArgumentException(
                    "batchSize must be between 1 and 10");
        }
        requirePositive(lease, "lease");
        requirePositive(retryInitial, "retryInitial");
        requirePositive(retryMaximum, "retryMaximum");
        if (retryInitial.compareTo(retryMaximum) > 0) {
            throw new IllegalArgumentException(
                    "retryInitial must not exceed retryMaximum");
        }
    }

    private static void requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
