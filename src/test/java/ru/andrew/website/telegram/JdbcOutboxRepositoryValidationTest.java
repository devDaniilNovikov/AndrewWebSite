package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class JdbcOutboxRepositoryValidationTest {
    private static final Instant NOW = Instant.parse("2026-01-30T00:00:00Z");
    private static final Instant PRIVACY_CUTOFF = NOW.minus(Duration.ofDays(29));
    private final JdbcOutboxRepository repository =
            new JdbcOutboxRepository(mock(JdbcClient.class));

    @Test
    void rejectsEveryInvalidClaimArgumentBeforeDatabaseAccess() {
        assertInvalid(null, PRIVACY_CUTOFF, 10, Duration.ofMinutes(2));
        assertInvalid(NOW, null, 10, Duration.ofMinutes(2));
        assertInvalid(NOW, PRIVACY_CUTOFF, 0, Duration.ofMinutes(2));
        assertInvalid(NOW, PRIVACY_CUTOFF, 11, Duration.ofMinutes(2));
        assertInvalid(NOW, PRIVACY_CUTOFF, 10, null);
        assertInvalid(NOW, PRIVACY_CUTOFF, 10, Duration.ZERO);
        assertInvalid(NOW, PRIVACY_CUTOFF, 10, Duration.ofSeconds(-1));
    }

    @Test
    void dueDeliveryStringRedactsAllMessageContent() {
        var due = new JdbcOutboxRepository.DueDelivery(
                7L,
                9L,
                1,
                UUID.fromString("22222222-2222-4222-8222-222222222222"),
                "Sensitive Name",
                "70000000000",
                "Sensitive comment",
                "/sensitive/",
                "repair",
                NOW);

        assertThat(due.toString())
                .isEqualTo(
                        "DueDelivery[outboxId=7, leadId=9, content=<redacted>]")
                .doesNotContain(
                        "Sensitive Name",
                        "70000000000",
                        "Sensitive comment",
                        "/sensitive/",
                        "22222222-2222-4222-8222-222222222222");
    }

    private void assertInvalid(
            Instant now,
            Instant privacyCutoff,
            int limit,
            Duration lease) {
        assertThatThrownBy(() -> repository.recoverExpiredAndClaimDue(
                        now, privacyCutoff, limit, lease))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
