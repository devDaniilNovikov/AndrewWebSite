package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TelegramWorkerPropertiesTest {
    @Test
    void acceptsFixedWorkerDefaults() {
        TelegramWorkerProperties properties = defaults();

        assertThat(properties.pollInterval()).isEqualTo(Duration.ofSeconds(15));
        assertThat(properties.batchSize()).isEqualTo(10);
        assertThat(properties.lease()).isEqualTo(Duration.ofMinutes(2));
        assertThat(properties.retryInitial()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.retryMaximum()).isEqualTo(Duration.ofHours(6));
    }

    @Test
    void rejectsUnsafeDurationsAndBatchBounds() {
        assertThatThrownBy(() -> properties(Duration.ZERO, 10, Duration.ofMinutes(2)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> properties(Duration.ofSeconds(15), 0, Duration.ofMinutes(2)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> properties(Duration.ofSeconds(15), 11, Duration.ofMinutes(2)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> properties(Duration.ofSeconds(15), 10, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> properties(
                        Duration.ofSeconds(-1), 10, Duration.ofMinutes(2)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TelegramWorkerProperties(
                        Duration.ofSeconds(15),
                        10,
                        Duration.ofMinutes(2),
                        Duration.ofHours(7),
                        Duration.ofHours(6)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static TelegramWorkerProperties defaults() {
        return properties(
                Duration.ofSeconds(15), 10, Duration.ofMinutes(2));
    }

    private static TelegramWorkerProperties properties(
            Duration pollInterval, int batchSize, Duration lease) {
        return new TelegramWorkerProperties(
                pollInterval,
                batchSize,
                lease,
                Duration.ofSeconds(30),
                Duration.ofHours(6));
    }
}
