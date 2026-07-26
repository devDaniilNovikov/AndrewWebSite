package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {
    private final RetryPolicy policy =
            new RetryPolicy(Duration.ofSeconds(30), Duration.ofHours(6));

    @Test
    void exponentialDelayStartsAtThirtySecondsAndCapsWithoutOverflow() {
        assertThat(policy.delay(1, null)).isEqualTo(Duration.ofSeconds(30));
        assertThat(policy.delay(2, null)).isEqualTo(Duration.ofMinutes(1));
        assertThat(policy.delay(10, null)).isEqualTo(Duration.ofMinutes(256));
        assertThat(policy.delay(20, null)).isEqualTo(Duration.ofHours(6));
        assertThat(policy.delay(Integer.MAX_VALUE, null))
                .isEqualTo(Duration.ofHours(6));
        assertThat(new RetryPolicy(Duration.ofHours(6), Duration.ofHours(6))
                        .delay(2, null))
                .isEqualTo(Duration.ofHours(6));
    }

    @Test
    void retryAfterUsesTheGreaterDelayAndStillHonorsCap() {
        assertThat(policy.delay(1, Duration.ofSeconds(120)))
                .isEqualTo(Duration.ofSeconds(120));
        assertThat(policy.delay(20, Duration.ofSeconds(1)))
                .isEqualTo(Duration.ofHours(6));
        assertThat(policy.delay(1, Duration.ofHours(8)))
                .isEqualTo(Duration.ofHours(6));
    }

    @Test
    void rejectsInvalidAttemptsAndConfiguration() {
        assertThatThrownBy(() -> policy.delay(0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("attempt must be positive");
        assertThatThrownBy(() -> policy.delay(1, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("retryAfter must be positive");
        assertThatThrownBy(() -> new RetryPolicy(Duration.ZERO, Duration.ofHours(6)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RetryPolicy(
                        Duration.ofSeconds(-1), Duration.ofHours(6)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RetryPolicy(
                        Duration.ofHours(7), Duration.ofHours(6)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
