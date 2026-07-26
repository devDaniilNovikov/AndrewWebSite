package ru.andrew.website.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import ru.andrew.website.telegram.JdbcOutboxRepository;
import ru.andrew.website.testing.MutableClock;
import ru.andrew.website.testing.PostgresTestConfiguration;

@Tag("database")
@SpringBootTest
@ActiveProfiles("test")
@Import({PostgresTestConfiguration.class, RetentionTestConfiguration.class})
class RetentionBoundaryTest {
    private static final Instant NOW =
            Instant.parse("2026-01-30T00:00:00Z");

    @Autowired
    RetentionService retention;

    @Autowired
    JdbcRetentionRepository repository;

    @Autowired
    JdbcOutboxRepository outbox;

    @Autowired
    RetentionHeartbeat heartbeat;

    @Autowired
    MeterRegistry meterRegistry;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    MutableClock clock;

    @BeforeEach
    void clean() {
        jdbc.sql("delete from telegram_outbox").update();
        jdbc.sql("delete from leads").update();
        clock.setInstant(NOW);
    }

    @Test
    void thresholdAnonymizesPiiAndTerminalizesEveryUndeliveredState() {
        Instant cutoff = NOW.minus(Duration.ofDays(29));
        Seed early = insertLead(cutoff.plusNanos(1_000), "pending");
        List<Seed> undelivered = List.of(
                insertLead(cutoff, "pending"),
                insertLead(cutoff, "retry"),
                insertLead(cutoff, "processing"),
                insertLead(cutoff, "blocked"),
                insertLead(NOW.minus(Duration.ofDays(30)), "processing"));
        Seed delivered = insertLead(cutoff, "delivered");
        UUID staleToken = undelivered.get(2).leaseToken();
        double anonymizedBefore =
                meterRegistry.get("andrew.privacy.anonymized").counter().count();

        retention.runOnce();

        assertPiiPresent(early.leadId());
        assertThat(outboxRow(early.outboxId()))
                .containsEntry("state", "pending");
        undelivered.forEach(seed -> {
            assertPiiCleared(seed.leadId());
            assertThat(outboxRow(seed.outboxId()))
                    .containsEntry("state", "blocked")
                    .containsEntry("last_error_code", "privacy_expired")
                    .containsEntry("lease_token", null)
                    .containsEntry("lease_until", null);
        });
        assertPiiCleared(delivered.leadId());
        assertThat(outboxRow(delivered.outboxId()))
                .containsEntry("state", "delivered")
                .containsEntry("last_error_code", null);
        assertThat(outbox.markDelivered(
                        undelivered.get(2).outboxId(),
                        staleToken,
                        NOW.plusSeconds(1)))
                .isFalse();
        assertThat(hardLimitPiiCount()).isZero();
        assertThat(heartbeat.lastSuccess()).contains(NOW);
        assertThat(meterRegistry
                        .get("andrew.privacy.anonymized")
                        .counter()
                        .count())
                .isEqualTo(anonymizedBefore + undelivered.size() + 1);
    }

    @Test
    void deletionUsesInclusiveCalendarBoundaryAndCascadesOutbox() {
        Instant deletionCutoff =
                Instant.parse("2025-01-30T00:00:00Z");
        Seed exact = insertAnonymizedLead(deletionCutoff);
        Seed newer =
                insertAnonymizedLead(deletionCutoff.plusNanos(1_000));

        retention.runOnce();

        assertThat(leadExists(exact.leadId())).isFalse();
        assertThat(outboxExists(exact.outboxId())).isFalse();
        assertThat(leadExists(newer.leadId())).isTrue();
        assertThat(outboxExists(newer.outboxId())).isTrue();
    }

    @Test
    void repositoryBoundaryIsInclusiveAndBatchIsBounded() {
        Instant cutoff = NOW.minus(Duration.ofDays(29));
        Seed oldest = insertLead(cutoff.minusSeconds(2), "pending");
        Seed middle = insertLead(cutoff.minusSeconds(1), "pending");
        Seed newest = insertLead(cutoff, "pending");

        RetentionBatchResult first = repository.expireBatch(cutoff, 2);

        assertThat(first).isEqualTo(new RetentionBatchResult(2, 2));
        assertPiiCleared(oldest.leadId());
        assertPiiCleared(middle.leadId());
        assertPiiPresent(newest.leadId());

        assertThat(repository.expireBatch(cutoff, 2))
                .isEqualTo(new RetentionBatchResult(1, 1));
        assertPiiCleared(newest.leadId());
    }

    private Seed insertLead(Instant createdAt, String state) {
        long leadId = jdbc.sql("""
                        insert into leads(
                            request_id, payload_fingerprint, name, phone, comment,
                            source_path, intent, consented_at, created_at
                        )
                        values (
                            :requestId, decode(repeat('00', 32), 'hex'),
                            'Fictional Retention User', '70000000000',
                            'fictional-retention-comment', '/fictional-retention/',
                            'repair', :createdAt, :createdAt
                        )
                        returning id
                        """)
                .param("requestId", UUID.randomUUID())
                .param("createdAt", createdAt.atOffset(ZoneOffset.UTC))
                .query(Long.class)
                .single();
        return insertOutbox(leadId, state, createdAt);
    }

    private Seed insertAnonymizedLead(Instant anonymizedAt) {
        long leadId = jdbc.sql("""
                        insert into leads(
                            request_id, payload_fingerprint, name, phone, comment,
                            source_path, intent, consented_at, created_at,
                            anonymized_at
                        )
                        values (
                            :requestId, null, null, null, null,
                            '/fictional-retention/', 'repair',
                            :createdAt, :createdAt, :anonymizedAt
                        )
                        returning id
                        """)
                .param("requestId", UUID.randomUUID())
                .param(
                        "createdAt",
                        anonymizedAt
                                .minus(Duration.ofDays(29))
                                .atOffset(ZoneOffset.UTC))
                .param("anonymizedAt", anonymizedAt.atOffset(ZoneOffset.UTC))
                .query(Long.class)
                .single();
        return insertOutbox(leadId, "blocked", anonymizedAt);
    }

    private Seed insertOutbox(long leadId, String state, Instant createdAt) {
        if ("processing".equals(state)) {
            UUID leaseToken = UUID.randomUUID();
            long outboxId = jdbc.sql("""
                            insert into telegram_outbox(
                                lead_id, state, attempt_count, next_attempt_at,
                                lease_token, lease_until, created_at, updated_at
                            )
                            values (
                                :leadId, 'processing', 1, :createdAt,
                                :leaseToken, :leaseUntil, :createdAt, :createdAt
                            )
                            returning id
                            """)
                    .param("leadId", leadId)
                    .param("createdAt", createdAt.atOffset(ZoneOffset.UTC))
                    .param("leaseToken", leaseToken)
                    .param(
                            "leaseUntil",
                            NOW.plus(Duration.ofHours(1))
                                    .atOffset(ZoneOffset.UTC))
                    .query(Long.class)
                    .single();
            return new Seed(leadId, outboxId, leaseToken);
        }
        if ("delivered".equals(state)) {
            long outboxId = jdbc.sql("""
                            insert into telegram_outbox(
                                lead_id, state, next_attempt_at, created_at,
                                updated_at, delivered_at
                            )
                            values (
                                :leadId, 'delivered', :createdAt, :createdAt,
                                :createdAt, :createdAt
                            )
                            returning id
                            """)
                    .param("leadId", leadId)
                    .param("createdAt", createdAt.atOffset(ZoneOffset.UTC))
                    .query(Long.class)
                    .single();
            return new Seed(leadId, outboxId, null);
        }
        String errorCode =
                "blocked".equals(state) ? "telegram_permanent_403" : null;
        long outboxId = jdbc.sql("""
                        insert into telegram_outbox(
                            lead_id, state, next_attempt_at, last_error_code,
                            created_at, updated_at
                        )
                        values (
                            :leadId, :state, :createdAt, :errorCode,
                            :createdAt, :createdAt
                        )
                        returning id
                        """)
                .param("leadId", leadId)
                .param("state", state)
                .param("createdAt", createdAt.atOffset(ZoneOffset.UTC))
                .param("errorCode", errorCode, java.sql.Types.VARCHAR)
                .query(Long.class)
                .single();
        return new Seed(leadId, outboxId, null);
    }

    private void assertPiiPresent(long leadId) {
        Map<String, Object> row = leadRow(leadId);
        assertThat(row.get("name")).isNotNull();
        assertThat(row.get("phone")).isNotNull();
        assertThat(row.get("comment")).isNotNull();
        assertThat(row.get("payload_fingerprint")).isNotNull();
        assertThat(row.get("anonymized_at")).isNull();
    }

    private void assertPiiCleared(long leadId) {
        Map<String, Object> row = leadRow(leadId);
        assertThat(row.get("name")).isNull();
        assertThat(row.get("phone")).isNull();
        assertThat(row.get("comment")).isNull();
        assertThat(row.get("payload_fingerprint")).isNull();
        assertThat(row.get("anonymized_at")).isNotNull();
    }

    private Map<String, Object> leadRow(long leadId) {
        return jdbc.sql("""
                        select
                            name, phone, comment, payload_fingerprint,
                            anonymized_at
                        from leads
                        where id = :id
                        """)
                .param("id", leadId)
                .query()
                .singleRow();
    }

    private Map<String, Object> outboxRow(long outboxId) {
        return jdbc.sql("""
                        select
                            state, lease_token, lease_until, last_error_code
                        from telegram_outbox
                        where id = :id
                        """)
                .param("id", outboxId)
                .query()
                .singleRow();
    }

    private int hardLimitPiiCount() {
        return jdbc.sql("""
                        select count(*)
                        from leads
                        where created_at <= :hardCutoff
                          and (
                              name is not null
                              or phone is not null
                              or comment is not null
                              or payload_fingerprint is not null
                          )
                        """)
                .param(
                        "hardCutoff",
                        NOW.minus(Duration.ofDays(30))
                                .atOffset(ZoneOffset.UTC))
                .query(Integer.class)
                .single();
    }

    private boolean leadExists(long leadId) {
        return jdbc.sql("select exists(select 1 from leads where id = :id)")
                .param("id", leadId)
                .query(Boolean.class)
                .single();
    }

    private boolean outboxExists(long outboxId) {
        return jdbc.sql(
                        "select exists(select 1 from telegram_outbox where id = :id)")
                .param("id", outboxId)
                .query(Boolean.class)
                .single();
    }

    private record Seed(long leadId, long outboxId, UUID leaseToken) {}
}
