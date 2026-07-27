package ru.andrew.website.telegram;

import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.http.client.autoconfigure.HttpClientsProperties;

record TelegramDeliveryWindow(Duration callBudget) {
    private static final Duration PRIVACY_THRESHOLD =
            Duration.ofDays(29);
    private static final String INVALID_WINDOW =
            "Telegram lease must exceed the HTTP timeout budget";

    TelegramDeliveryWindow {
        if (!positive(callBudget)) {
            throw new IllegalStateException(INVALID_WINDOW);
        }
    }

    static TelegramDeliveryWindow from(
            TelegramWorkerProperties worker,
            HttpClientsProperties http) {
        if (worker == null || http == null) {
            throw new IllegalStateException(INVALID_WINDOW);
        }
        Duration connect = http.getConnectTimeout();
        Duration read = http.getReadTimeout();
        if (!positive(connect) || !positive(read)) {
            throw new IllegalStateException(INVALID_WINDOW);
        }
        Duration budget;
        try {
            budget = connect.plus(read);
        } catch (ArithmeticException invalidBudget) {
            throw new IllegalStateException(INVALID_WINDOW);
        }
        if (worker.lease().compareTo(budget) <= 0) {
            throw new IllegalStateException(INVALID_WINDOW);
        }
        return new TelegramDeliveryWindow(budget);
    }

    Instant privacyCutoff(Instant observedAt) {
        return observedAt
                .plus(callBudget)
                .minus(PRIVACY_THRESHOLD);
    }

    Instant privacyLatestStart(TelegramLeadMessage message) {
        return message.createdAt()
                .plus(PRIVACY_THRESHOLD)
                .minus(callBudget);
    }

    Instant leaseLatestStart(ClaimedDelivery claim) {
        return claim.leaseUntil().minus(callBudget);
    }

    Instant latestStart(
            ClaimedDelivery claim, TelegramLeadMessage message) {
        Instant leaseDeadline = leaseLatestStart(claim);
        Instant privacyDeadline = privacyLatestStart(message);
        return leaseDeadline.isBefore(privacyDeadline)
                ? leaseDeadline
                : privacyDeadline;
    }

    boolean canStart(Instant observedAt, Instant latestStart) {
        return observedAt.isBefore(latestStart);
    }

    private static boolean positive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }
}
