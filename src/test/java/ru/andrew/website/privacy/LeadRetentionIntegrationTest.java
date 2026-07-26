package ru.andrew.website.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import ru.andrew.website.testing.MutableClock;
import ru.andrew.website.testing.PostgresTestConfiguration;

@Tag("database")
@SpringBootTest
@ActiveProfiles("test")
@Import({PostgresTestConfiguration.class, RetentionTestConfiguration.class})
class LeadRetentionIntegrationTest {
    private static final Instant NOW =
            Instant.parse("2026-01-30T00:00:00Z");

    @Autowired
    RetentionService retention;

    @Autowired
    RetentionHeartbeat heartbeat;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    MutableClock clock;

    @BeforeEach
    void clean() {
        dropFailureTrigger();
        jdbc.sql("delete from telegram_outbox").update();
        jdbc.sql("delete from leads").update();
        clock.setInstant(NOW);
    }

    @AfterEach
    void removeFailureTrigger() {
        dropFailureTrigger();
    }

    @Test
    void concurrentFullPassesRemainIdempotentAndClearAllExpiredPii()
            throws Exception {
        for (int index = 0; index < 12; index++) {
            insertPendingLead(
                    NOW.minus(Duration.ofDays(29))
                            .minusSeconds(index));
        }
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> {
                start.await();
                retention.runOnce();
                return true;
            });
            var second = executor.submit(() -> {
                start.await();
                retention.runOnce();
                return true;
            });
            start.countDown();

            assertThat(List.of(first.get(), second.get()))
                    .containsExactly(true, true);
        }

        assertThat(piiBearingLeadCount()).isZero();
        assertThat(jdbc.sql("""
                                select count(*)
                                from telegram_outbox
                                where state <> 'blocked'
                                   or last_error_code <> 'privacy_expired'
                                """)
                        .query(Integer.class)
                        .single())
                .isZero();
    }

    @Test
    void failedAnonymizationRollsBackOutboxAndDoesNotAdvanceHeartbeat() {
        Seed seed = insertProcessingLead(
                NOW.minus(Duration.ofDays(29)));
        var heartbeatBefore = heartbeat.lastSuccess();
        installFailureTrigger();

        assertThatThrownBy(retention::runOnce)
                .isInstanceOf(DataAccessException.class);

        assertThat(heartbeat.lastSuccess()).isEqualTo(heartbeatBefore);
        assertThat(jdbc.sql("""
                                select count(*)
                                from leads
                                where id = :id
                                  and name is not null
                                  and phone is not null
                                  and payload_fingerprint is not null
                                  and anonymized_at is null
                                """)
                        .param("id", seed.leadId())
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
        assertThat(jdbc.sql("""
                                select count(*)
                                from telegram_outbox
                                where id = :id
                                  and state = 'processing'
                                  and lease_token = :leaseToken
                                  and lease_until is not null
                                """)
                        .param("id", seed.outboxId())
                        .param("leaseToken", seed.leaseToken())
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
    }

    private void insertPendingLead(Instant createdAt) {
        long leadId = insertLead(createdAt);
        jdbc.sql("""
                        insert into telegram_outbox(
                            lead_id, state, next_attempt_at,
                            created_at, updated_at
                        )
                        values (
                            :leadId, 'pending', :createdAt,
                            :createdAt, :createdAt
                        )
                        """)
                .param("leadId", leadId)
                .param("createdAt", createdAt.atOffset(ZoneOffset.UTC))
                .update();
    }

    private Seed insertProcessingLead(Instant createdAt) {
        long leadId = insertLead(createdAt);
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

    private long insertLead(Instant createdAt) {
        return jdbc.sql("""
                        insert into leads(
                            request_id, payload_fingerprint, name, phone,
                            comment, source_path, intent, consented_at,
                            created_at
                        )
                        values (
                            :requestId, decode(repeat('00', 32), 'hex'),
                            'Fictional Concurrent User', '70000000000',
                            'fictional-concurrency-comment',
                            '/fictional-concurrency/', 'repair',
                            :createdAt, :createdAt
                        )
                        returning id
                        """)
                .param("requestId", UUID.randomUUID())
                .param("createdAt", createdAt.atOffset(ZoneOffset.UTC))
                .query(Long.class)
                .single();
    }

    private int piiBearingLeadCount() {
        return jdbc.sql("""
                        select count(*)
                        from leads
                        where name is not null
                           or phone is not null
                           or comment is not null
                           or payload_fingerprint is not null
                        """)
                .query(Integer.class)
                .single();
    }

    private void installFailureTrigger() {
        jdbc.sql("""
                        create function test_fail_anonymization()
                        returns trigger language plpgsql as $$
                        begin
                            if new.anonymized_at is not null then
                                raise exception 'test-only anonymization failure';
                            end if;
                            return new;
                        end
                        $$
                        """)
                .update();
        jdbc.sql("""
                        create trigger test_fail_anonymization
                        before update on leads
                        for each row execute function test_fail_anonymization()
                        """)
                .update();
    }

    private void dropFailureTrigger() {
        jdbc.sql(
                        "drop trigger if exists test_fail_anonymization on leads")
                .update();
        jdbc.sql(
                        "drop function if exists test_fail_anonymization()")
                .update();
    }

    private record Seed(long leadId, long outboxId, UUID leaseToken) {}
}
