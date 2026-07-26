package ru.andrew.website.telegram;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxRepository {
    List<ClaimedDelivery> recoverExpiredAndClaimDue(
            Instant now,
            Instant privacyCutoff,
            int limit,
            Duration lease);

    ClaimBatch recoverExpiredAndClaimDueWithStats(
            Instant now,
            Instant privacyCutoff,
            int limit,
            Duration lease);

    Optional<TelegramLeadMessage> reloadDeliverable(
            long outboxId,
            UUID leaseToken,
            Instant privacyCutoff);

    boolean resolvePrivacyInvalidation(
            long outboxId,
            UUID leaseToken,
            Instant privacyCutoff,
            Instant now);

    boolean markDelivered(
            long outboxId,
            UUID leaseToken,
            Instant now);

    boolean markRetry(
            long outboxId,
            UUID leaseToken,
            String code,
            Instant nextAttemptAt,
            Instant now);

    boolean markBlocked(
            long outboxId,
            UUID leaseToken,
            String code,
            Instant now);

    long countByState(OutboxState state);
}
