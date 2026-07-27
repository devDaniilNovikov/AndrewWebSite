package ru.andrew.website.privacy;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public class JdbcRetentionRepository
        implements RetentionRepository {
    private static final String MISSING_CUTOFF =
            "retention cutoff must not be null";
    private static final String INVALID_BATCH =
            "retention batch size must be between 1 and 1000";

    private final JdbcClient jdbc;
    private final Clock clock;

    public JdbcRetentionRepository(JdbcClient jdbc, Clock clock) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RetentionBatchResult expireBatch(
            Instant cutoffInclusive, int limit) {
        validateArguments(cutoffInclusive, limit);
        OffsetDateTime cutoff = asUtcTimestamp(cutoffInclusive);
        OffsetDateTime anonymizedAt =
                asUtcTimestamp(clock.instant());
        return jdbc.sql("""
                        with candidates as materialized (
                            select id
                            from leads
                            where anonymized_at is null
                              and created_at <= :cutoff
                            order by created_at, id
                            limit :limit
                            for update skip locked
                        ),
                        blocked as (
                            update telegram_outbox o
                            set state = 'blocked',
                                lease_token = null,
                                lease_until = null,
                                last_error_code = 'privacy_expired',
                                updated_at = :anonymizedAt
                            from candidates c
                            where o.lead_id = c.id
                              and o.state <> 'delivered'
                            returning o.id
                        ),
                        anonymized as (
                            update leads l
                            set name = null,
                                phone = null,
                                comment = null,
                                source_path = '/',
                                payload_fingerprint = null,
                                anonymized_at = :anonymizedAt
                            from candidates c
                            where l.id = c.id
                              and l.anonymized_at is null
                            returning l.id
                        )
                        select
                            (select count(*) from anonymized)
                                as anonymized_count,
                            (select count(*) from blocked)
                                as blocked_count
                        """)
                .param("cutoff", cutoff)
                .param("limit", limit)
                .param("anonymizedAt", anonymizedAt)
                .query((result, rowNumber) ->
                        new RetentionBatchResult(
                                result.getInt("anonymized_count"),
                                result.getInt("blocked_count")))
                .single();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int deleteBatch(
            Instant anonymizedCutoffInclusive, int limit) {
        validateArguments(anonymizedCutoffInclusive, limit);
        return jdbc.sql("""
                        with candidates as materialized (
                            select id
                            from leads
                            where anonymized_at is not null
                              and anonymized_at <= :cutoff
                            order by anonymized_at, id
                            limit :limit
                            for update skip locked
                        ),
                        deleted as (
                            delete from leads l
                            using candidates c
                            where l.id = c.id
                              and l.anonymized_at is not null
                              and l.anonymized_at <= :cutoff
                            returning l.id
                        )
                        select count(*) from deleted
                        """)
                .param(
                        "cutoff",
                        asUtcTimestamp(anonymizedCutoffInclusive))
                .param("limit", limit)
                .query(Integer.class)
                .single();
    }

    @Override
    public boolean isComplete(
            Instant expireCutoffInclusive,
            Instant deleteCutoffInclusive) {
        requireCutoff(expireCutoffInclusive);
        requireCutoff(deleteCutoffInclusive);
        return jdbc.sql("""
                        select not (
                            exists (
                                select 1
                                from leads
                                where anonymized_at is null
                                  and created_at <= :expireCutoff
                            )
                            or exists (
                                select 1
                                from leads
                                where anonymized_at is not null
                                  and anonymized_at <= :deleteCutoff
                            )
                        )
                        """)
                .param(
                        "expireCutoff",
                        asUtcTimestamp(expireCutoffInclusive))
                .param(
                        "deleteCutoff",
                        asUtcTimestamp(deleteCutoffInclusive))
                .query(Boolean.class)
                .single();
    }

    private static void validateArguments(
            Instant cutoff, int limit) {
        requireCutoff(cutoff);
        if (limit < 1) {
            throw new IllegalArgumentException(INVALID_BATCH);
        }
        if (limit > RetentionProperties.MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(INVALID_BATCH);
        }
    }

    private static void requireCutoff(Instant cutoff) {
        if (cutoff == null) {
            throw new IllegalArgumentException(MISSING_CUTOFF);
        }
    }

    private static OffsetDateTime asUtcTimestamp(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
