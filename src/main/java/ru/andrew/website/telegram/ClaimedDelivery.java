package ru.andrew.website.telegram;

import java.time.Instant;
import java.util.UUID;

public record ClaimedDelivery(
        long outboxId,
        long leadId,
        UUID leaseToken,
        int attemptCount,
        Instant leaseUntil,
        TelegramLeadMessage message) {
}
