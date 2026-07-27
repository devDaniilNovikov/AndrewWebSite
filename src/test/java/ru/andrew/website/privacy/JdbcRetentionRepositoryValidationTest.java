package ru.andrew.website.privacy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class JdbcRetentionRepositoryValidationTest {
    private static final Instant NOW =
            Instant.parse("2026-01-30T00:00:00Z");
    private final JdbcRetentionRepository repository =
            new JdbcRetentionRepository(
                    mock(JdbcClient.class),
                    Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void rejectsMissingCutoffs() {
        assertThatThrownBy(() -> repository.expireBatch(null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("retention cutoff must not be null");
        assertThatThrownBy(() -> repository.deleteBatch(null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("retention cutoff must not be null");
        assertThatThrownBy(() -> repository.isComplete(null, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("retention cutoff must not be null");
        assertThatThrownBy(() -> repository.isComplete(NOW, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("retention cutoff must not be null");
    }

    @Test
    void rejectsUnboundedBatchSizes() {
        assertThatThrownBy(() -> repository.expireBatch(NOW, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("retention batch size must be between 1 and 1000");
        assertThatThrownBy(() -> repository.expireBatch(NOW, 1_001))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("retention batch size must be between 1 and 1000");
    }
}
