package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.http.client.autoconfigure.HttpClientsProperties;

class TelegramDeliveryWindowTest {
    private static final Instant NOW =
            Instant.parse("2026-01-30T00:00:00Z");
    private static final Duration CALL_BUDGET =
            Duration.ofSeconds(13);

    @Test
    void derivesTheCallBudgetFromTheBootManagedHttpTimeouts() {
        TelegramDeliveryWindow window =
                TelegramDeliveryWindow.from(
                        worker(Duration.ofMinutes(2)),
                        http(Duration.ofSeconds(3), Duration.ofSeconds(10)));

        assertThat(window.callBudget()).isEqualTo(CALL_BUDGET);
        assertThat(window.privacyCutoff(NOW))
                .isEqualTo(NOW.plus(CALL_BUDGET)
                        .minus(Duration.ofDays(29)));
    }

    @Test
    void choosesTheEarlierLeaseOrPrivacyLatestStart() {
        TelegramDeliveryWindow window =
                new TelegramDeliveryWindow(CALL_BUDGET);
        ClaimedDelivery claim = claim(NOW.plus(Duration.ofMinutes(2)));
        TelegramLeadMessage leaseBound =
                message(NOW.minus(Duration.ofDays(1)));
        TelegramLeadMessage privacyBound =
                message(NOW.minus(Duration.ofDays(29)).plusSeconds(20));

        assertThat(window.leaseLatestStart(claim))
                .isEqualTo(NOW.plusSeconds(107));
        assertThat(window.privacyLatestStart(privacyBound))
                .isEqualTo(NOW.plusSeconds(7));
        assertThat(window.latestStart(claim, leaseBound))
                .isEqualTo(NOW.plusSeconds(107));
        assertThat(window.latestStart(claim, privacyBound))
                .isEqualTo(NOW.plusSeconds(7));
        assertThat(window.canStart(NOW, NOW.plusNanos(1))).isTrue();
        assertThat(window.canStart(NOW, NOW)).isFalse();
    }

    @Test
    void rejectsMissingOrNonPositiveBudgetsAndTimeouts() {
        assertInvalid(() -> new TelegramDeliveryWindow(null));
        assertInvalid(() -> new TelegramDeliveryWindow(Duration.ZERO));
        assertInvalid(() -> new TelegramDeliveryWindow(Duration.ofSeconds(-1)));
        assertInvalid(() -> TelegramDeliveryWindow.from(
                null,
                http(Duration.ofSeconds(3), Duration.ofSeconds(10))));
        assertInvalid(() -> TelegramDeliveryWindow.from(
                worker(Duration.ofMinutes(2)), null));
        assertInvalid(() -> TelegramDeliveryWindow.from(
                worker(Duration.ofMinutes(2)),
                http(null, Duration.ofSeconds(10))));
        assertInvalid(() -> TelegramDeliveryWindow.from(
                worker(Duration.ofMinutes(2)),
                http(Duration.ofSeconds(3), Duration.ZERO)));
        assertInvalid(() -> TelegramDeliveryWindow.from(
                worker(Duration.ofMinutes(2)),
                http(Duration.ofSeconds(3), Duration.ofSeconds(-1))));
    }

    @Test
    void rejectsOverflowAndLeaseWithoutACompleteCallBudget() {
        assertInvalid(() -> TelegramDeliveryWindow.from(
                worker(Duration.ofSeconds(30)),
                http(
                        Duration.ofSeconds(Long.MAX_VALUE),
                        Duration.ofSeconds(Long.MAX_VALUE))));
        assertInvalid(() -> TelegramDeliveryWindow.from(
                worker(CALL_BUDGET),
                http(Duration.ofSeconds(3), Duration.ofSeconds(10))));
        assertInvalid(() -> TelegramDeliveryWindow.from(
                worker(Duration.ofSeconds(12)),
                http(Duration.ofSeconds(3), Duration.ofSeconds(10))));
    }

    private static void assertInvalid(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Telegram lease must exceed the HTTP timeout budget");
    }

    private static TelegramWorkerProperties worker(Duration lease) {
        return new TelegramWorkerProperties(
                Duration.ofSeconds(15),
                10,
                lease,
                Duration.ofSeconds(30),
                Duration.ofHours(6));
    }

    private static HttpClientsProperties http(
            Duration connect, Duration read) {
        HttpClientsProperties properties =
                new HttpClientsProperties();
        properties.setConnectTimeout(connect);
        properties.setReadTimeout(read);
        return properties;
    }

    private static ClaimedDelivery claim(Instant leaseUntil) {
        return new ClaimedDelivery(
                7L,
                9L,
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                1,
                leaseUntil,
                message(NOW.minus(Duration.ofDays(1))));
    }

    private static TelegramLeadMessage message(Instant createdAt) {
        return new TelegramLeadMessage(
                9L,
                UUID.fromString("22222222-2222-4222-8222-222222222222"),
                "Fictional User",
                "70000000000",
                null,
                "/fictional/",
                "repair",
                createdAt);
    }
}
