package ru.andrew.website.telegram;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public class JdbcOutboxRepository implements OutboxRepository {
    static final int MAX_CLAIM_SIZE = 10;
    private static final Duration PRIVACY_THRESHOLD =
            Duration.ofDays(29);

    private final JdbcClient jdbc;

    public JdbcOutboxRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<ClaimedDelivery> recoverExpiredAndClaimDue(
            Instant now,
            Instant privacyCutoff,
            int limit,
            Duration lease) {
        return recoverExpiredAndClaimDueWithStats(
                        now, privacyCutoff, limit, lease)
                .deliveries();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ClaimBatch recoverExpiredAndClaimDueWithStats(
            Instant now,
            Instant privacyCutoff,
            int limit,
            Duration lease) {
        requireClaimArguments(now, privacyCutoff, limit, lease);
        int recoveredLeaseCount = recoverExpired(now, limit);
        Instant leaseUntil = now.plus(lease);
        List<DueDelivery> due = lockDue(now, privacyCutoff, limit);
        List<ClaimedDelivery> deliveries = due.stream()
                .map(delivery -> claim(delivery, now, leaseUntil))
                .toList();
        return new ClaimBatch(deliveries, recoveredLeaseCount);
    }

    @Override
    public Optional<TelegramLeadMessage> reloadDeliverable(
            long outboxId,
            UUID leaseToken,
            Instant privacyCutoff) {
        return jdbc.sql("""
                        select
                            l.id,
                            l.request_id,
                            l.name,
                            l.phone,
                            l.comment,
                            l.source_path,
                            l.intent,
                            l.created_at
                        from telegram_outbox o
                        join leads l on l.id = o.lead_id
                        where o.id = :outboxId
                          and o.state = 'processing'
                          and o.lease_token = :leaseToken
                          and o.lease_until > :now
                          and l.anonymized_at is null
                          and l.created_at > :privacyCutoff
                        """)
                .param("outboxId", outboxId)
                .param("leaseToken", leaseToken)
                .param(
                        "now",
                        asUtcTimestamp(
                                privacyCutoff.plus(PRIVACY_THRESHOLD)))
                .param("privacyCutoff", asUtcTimestamp(privacyCutoff))
                .query((result, rowNumber) -> message(
                        result.getLong("id"),
                        result.getObject("request_id", UUID.class),
                        result.getString("name"),
                        result.getString("phone"),
                        result.getString("comment"),
                        result.getString("source_path"),
                        result.getString("intent"),
                        result.getObject("created_at", OffsetDateTime.class)
                                .toInstant()))
                .optional();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean resolvePrivacyInvalidation(
            long outboxId,
            UUID leaseToken,
            Instant privacyCutoff,
            Instant now) {
        int updated = jdbc.sql("""
                        update telegram_outbox o
                        set state = 'blocked',
                            lease_token = null,
                            lease_until = null,
                            last_error_code = 'privacy_expired',
                            updated_at = :now
                        from leads l
                        where o.id = :outboxId
                          and l.id = o.lead_id
                          and o.state = 'processing'
                          and o.lease_token = :leaseToken
                          and o.lease_until > :now
                          and (
                              l.anonymized_at is not null
                              or l.created_at <= :privacyCutoff
                          )
                        """)
                .param("outboxId", outboxId)
                .param("leaseToken", leaseToken)
                .param("privacyCutoff", asUtcTimestamp(privacyCutoff))
                .param("now", asUtcTimestamp(now))
                .update();
        if (updated == 1) {
            return true;
        }
        return jdbc.sql("""
                        select exists (
                            select 1
                            from telegram_outbox
                            where id = :outboxId
                              and state = 'blocked'
                              and last_error_code = 'privacy_expired'
                        )
                        """)
                .param("outboxId", outboxId)
                .query(Boolean.class)
                .single();
    }

    @Override
    public boolean markDelivered(
            long outboxId,
            UUID leaseToken,
            Instant now) {
        return jdbc.sql("""
                        update telegram_outbox
                        set state = 'delivered',
                            lease_token = null,
                            lease_until = null,
                            last_error_code = null,
                            delivered_at = :now,
                            updated_at = :now
                        where id = :outboxId
                          and state = 'processing'
                          and lease_token = :leaseToken
                          and lease_until > :now
                        """)
                .param("outboxId", outboxId)
                .param("leaseToken", leaseToken)
                .param("now", asUtcTimestamp(now))
                .update() == 1;
    }

    @Override
    public boolean markRetry(
            long outboxId,
            UUID leaseToken,
            String code,
            Instant nextAttemptAt,
            Instant now) {
        return jdbc.sql("""
                        update telegram_outbox
                        set state = 'retry',
                            lease_token = null,
                            lease_until = null,
                            last_error_code = :code,
                            next_attempt_at = :nextAttemptAt,
                            updated_at = :now
                        where id = :outboxId
                          and state = 'processing'
                          and lease_token = :leaseToken
                          and lease_until > :now
                        """)
                .param("outboxId", outboxId)
                .param("leaseToken", leaseToken)
                .param("code", code)
                .param("nextAttemptAt", asUtcTimestamp(nextAttemptAt))
                .param("now", asUtcTimestamp(now))
                .update() == 1;
    }

    @Override
    public boolean markBlocked(
            long outboxId,
            UUID leaseToken,
            String code,
            Instant now) {
        return jdbc.sql("""
                        update telegram_outbox
                        set state = 'blocked',
                            lease_token = null,
                            lease_until = null,
                            last_error_code = :code,
                            updated_at = :now
                        where id = :outboxId
                          and state = 'processing'
                          and lease_token = :leaseToken
                          and lease_until > :now
                        """)
                .param("outboxId", outboxId)
                .param("leaseToken", leaseToken)
                .param("code", code)
                .param("now", asUtcTimestamp(now))
                .update() == 1;
    }

    @Override
    public long countByState(OutboxState state) {
        return jdbc.sql("""
                        select count(*)
                        from telegram_outbox
                        where state = :state
                        """)
                .param("state", state.name())
                .query(Long.class)
                .single();
    }

    private int recoverExpired(Instant now, int limit) {
        return jdbc.sql("""
                        with expired as (
                            select id
                            from telegram_outbox
                            where state = 'processing'
                              and lease_until <= :now
                            order by lease_until, id
                            limit :limit
                            for update skip locked
                        )
                        update telegram_outbox o
                        set state = 'retry',
                            lease_token = null,
                            lease_until = null,
                            next_attempt_at = :now,
                            last_error_code = 'lease_expired',
                            updated_at = :now
                        from expired
                        where o.id = expired.id
                        """)
                .param("now", asUtcTimestamp(now))
                .param("limit", limit)
                .update();
    }

    private List<DueDelivery> lockDue(
            Instant now,
            Instant privacyCutoff,
            int limit) {
        return jdbc.sql("""
                        select
                            o.id as outbox_id,
                            o.lead_id,
                            o.attempt_count,
                            l.request_id,
                            l.name,
                            l.phone,
                            l.comment,
                            l.source_path,
                            l.intent,
                            l.created_at
                        from telegram_outbox o
                        join leads l on l.id = o.lead_id
                        where o.state in ('pending', 'retry')
                          and o.next_attempt_at <= :now
                          and l.anonymized_at is null
                          and l.created_at > :privacyCutoff
                        order by o.next_attempt_at, o.id
                        limit :limit
                        for update of o skip locked
                        """)
                .param("now", asUtcTimestamp(now))
                .param("privacyCutoff", asUtcTimestamp(privacyCutoff))
                .param("limit", limit)
                .query((result, rowNumber) -> new DueDelivery(
                        result.getLong("outbox_id"),
                        result.getLong("lead_id"),
                        result.getInt("attempt_count"),
                        result.getObject("request_id", UUID.class),
                        result.getString("name"),
                        result.getString("phone"),
                        result.getString("comment"),
                        result.getString("source_path"),
                        result.getString("intent"),
                        result.getObject("created_at", OffsetDateTime.class)
                                .toInstant()))
                .list();
    }

    private ClaimedDelivery claim(
            DueDelivery due,
            Instant now,
            Instant leaseUntil) {
        UUID leaseToken = UUID.randomUUID();
        int attemptCount =
                due.attemptCount() == Integer.MAX_VALUE
                        ? Integer.MAX_VALUE
                        : due.attemptCount() + 1;
        int updated = jdbc.sql("""
                        update telegram_outbox
                        set state = 'processing',
                            attempt_count = :attemptCount,
                            lease_token = :leaseToken,
                            lease_until = :leaseUntil,
                            updated_at = :now
                        where id = :outboxId
                          and state in ('pending', 'retry')
                        """)
                .param("outboxId", due.outboxId())
                .param("attemptCount", attemptCount)
                .param("leaseToken", leaseToken)
                .param("leaseUntil", asUtcTimestamp(leaseUntil))
                .param("now", asUtcTimestamp(now))
                .update();
        if (updated != 1) {
            throw new IllegalStateException(
                    "Locked outbox claim was not persisted");
        }
        TelegramLeadMessage message = message(
                due.leadId(),
                due.requestId(),
                due.name(),
                due.phone(),
                due.comment(),
                due.sourcePath(),
                due.intent(),
                due.createdAt());
        return new ClaimedDelivery(
                due.outboxId(),
                due.leadId(),
                leaseToken,
                attemptCount,
                leaseUntil,
                message);
    }

    private static TelegramLeadMessage message(
            long leadId,
            UUID requestId,
            String name,
            String phone,
            String comment,
            String sourcePath,
            String intent,
            Instant createdAt) {
        return new TelegramLeadMessage(
                leadId,
                requestId,
                name,
                phone,
                comment,
                sourcePath,
                intent,
                createdAt);
    }

    private static void requireClaimArguments(
            Instant now,
            Instant privacyCutoff,
            int limit,
            Duration lease) {
        if (now == null || privacyCutoff == null) {
            throw new IllegalArgumentException(
                    "claim timestamps must not be null");
        }
        if (limit < 1 || limit > MAX_CLAIM_SIZE) {
            throw new IllegalArgumentException(
                    "claim limit must be between 1 and 10");
        }
        if (lease == null || lease.isZero() || lease.isNegative()) {
            throw new IllegalArgumentException(
                    "claim lease must be positive");
        }
    }

    private static OffsetDateTime asUtcTimestamp(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    record DueDelivery(
            long outboxId,
            long leadId,
            int attemptCount,
            UUID requestId,
            String name,
            String phone,
            String comment,
            String sourcePath,
            String intent,
            Instant createdAt) {
        @Override
        public String toString() {
            return "DueDelivery[outboxId="
                    + outboxId
                    + ", leadId="
                    + leadId
                    + ", content=<redacted>]";
        }
    }
}
