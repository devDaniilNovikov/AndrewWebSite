package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.andrew.website.testing.PostgresTestConfiguration;

@Tag("database")
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class JdbcOutboxRepositoryIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-01-30T00:00:00Z");
    private static final Instant PRIVACY_CUTOFF = NOW.minus(Duration.ofDays(29));
    private static final Duration LEASE = Duration.ofMinutes(2);

    @Autowired
    OutboxRepository outbox;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void clean() {
        jdbc.sql("""
                        drop trigger if exists test_skip_processing_claim
                        on telegram_outbox
                        """)
                .update();
        jdbc.sql("drop function if exists test_skip_processing_claim()")
                .update();
        jdbc.sql("delete from telegram_outbox").update();
        jdbc.sql("delete from leads").update();
    }

    @Test
    void claimsInDueOrderWithinRequestedLimitAndUsesDifferentLeases() {
        long later = seedDueLead(1, NOW.minusSeconds(30), NOW.minus(Duration.ofDays(1)));
        long firstAtSameTime =
                seedDueLead(2, NOW.minusSeconds(60), NOW.minus(Duration.ofDays(1)));
        long secondAtSameTime =
                seedDueLead(3, NOW.minusSeconds(60), NOW.minus(Duration.ofDays(1)));
        seedDueLead(4, NOW.plusSeconds(1), NOW.minus(Duration.ofDays(1)));

        List<ClaimedDelivery> claims =
                outbox.recoverExpiredAndClaimDue(NOW, PRIVACY_CUTOFF, 2, LEASE);

        assertThat(claims)
                .extracting(ClaimedDelivery::outboxId)
                .containsExactly(firstAtSameTime, secondAtSameTime);
        assertThat(claims).extracting(ClaimedDelivery::leaseToken)
                .doesNotHaveDuplicates();
        assertThat(claims).allSatisfy(claim -> {
            assertThat(claim.attemptCount()).isEqualTo(1);
            assertThat(claim.leaseUntil()).isEqualTo(NOW.plus(LEASE));
        });
        assertThat(state(later)).isEqualTo(OutboxState.pending);
    }

    @Test
    void twoConcurrentPostgresWorkersClaimDisjointIds() throws Exception {
        for (int index = 0; index < 20; index++) {
            seedDueLead(index + 10, NOW.minusSeconds(60), NOW.minus(Duration.ofDays(1)));
        }
        var barrier = new CyclicBarrier(2);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> claimAfter(barrier));
            var second = executor.submit(() -> claimAfter(barrier));
            List<ClaimedDelivery> left = first.get();
            List<ClaimedDelivery> right = second.get();

            assertThat(left).hasSize(10);
            assertThat(right).hasSize(10);
            var ids = new HashSet<Long>();
            left.forEach(claim -> assertThat(ids.add(claim.outboxId())).isTrue());
            right.forEach(claim -> assertThat(ids.add(claim.outboxId())).isTrue());
            assertThat(ids).hasSize(20);
        }
    }

    @Test
    void rejectsRequestedClaimLimitAboveHardMaximum() {
        long outboxId =
                seedDueLead(35, NOW.minusSeconds(60), NOW.minus(Duration.ofDays(1)));

        assertThatThrownBy(() -> outbox.recoverExpiredAndClaimDue(
                        NOW, PRIVACY_CUTOFF, 11, LEASE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("claim limit must be between 1 and 10");
        assertThat(state(outboxId)).isEqualTo(OutboxState.pending);
    }

    @Test
    void expiredLeaseIsRecoveredAndStaleTokenCannotComplete() {
        long outboxId =
                seedDueLead(40, NOW.minusSeconds(60), NOW.minus(Duration.ofDays(1)));
        ClaimedDelivery first =
                outbox.recoverExpiredAndClaimDue(NOW, PRIVACY_CUTOFF, 10, LEASE).getFirst();

        List<ClaimedDelivery> recovered = outbox.recoverExpiredAndClaimDue(
                NOW.plus(Duration.ofMinutes(3)),
                PRIVACY_CUTOFF,
                10,
                LEASE);

        assertThat(recovered).singleElement().satisfies(claim -> {
            assertThat(claim.outboxId()).isEqualTo(outboxId);
            assertThat(claim.leaseToken()).isNotEqualTo(first.leaseToken());
            assertThat(claim.attemptCount()).isEqualTo(2);
        });
        assertThat(outbox.markDelivered(
                        outboxId, first.leaseToken(), NOW.plusSeconds(181)))
                .isFalse();
        assertThat(outbox.markRetry(
                        outboxId,
                        first.leaseToken(),
                        "network",
                        NOW.plusSeconds(211),
                        NOW.plusSeconds(181)))
                .isFalse();
        assertThat(outbox.markBlocked(
                        outboxId,
                        first.leaseToken(),
                        "telegram_permanent_403",
                        NOW.plusSeconds(181)))
                .isFalse();
        assertThat(errorCode(outboxId)).isEqualTo("lease_expired");
    }

    @Test
    void expiredLeaseCannotReloadOrCompleteBeforeRecovery() {
        ClaimedDelivery delivered = claimed(41);
        ClaimedDelivery retry = claimed(42);
        ClaimedDelivery blocked = claimed(43);
        Instant expiredAt = NOW.plus(LEASE);

        assertThat(outbox.markDelivered(
                        delivered.outboxId(),
                        delivered.leaseToken(),
                        expiredAt))
                .isFalse();
        assertThat(outbox.markRetry(
                        retry.outboxId(),
                        retry.leaseToken(),
                        "network",
                        expiredAt.plusSeconds(30),
                        expiredAt))
                .isFalse();
        assertThat(outbox.markBlocked(
                        blocked.outboxId(),
                        blocked.leaseToken(),
                        "telegram_permanent_403",
                        expiredAt))
                .isFalse();
    }

    @Test
    void recoveryIsBoundedAndReportsLeaseExpiredTransitions() {
        Instant firstPoll = NOW.minus(Duration.ofMinutes(3));
        Instant dueAt = firstPoll.minusSeconds(1);
        for (int index = 0; index < 12; index++) {
            seedDueLead(
                    44 + index,
                    dueAt,
                    NOW.minus(Duration.ofDays(1)));
        }
        outbox.recoverExpiredAndClaimDue(
                firstPoll, PRIVACY_CUTOFF, 10, LEASE);
        outbox.recoverExpiredAndClaimDue(
                firstPoll, PRIVACY_CUTOFF, 10, LEASE);

        ClaimBatch batch = outbox.recoverExpiredAndClaimDueWithStats(
                NOW, PRIVACY_CUTOFF, 10, LEASE);

        assertThat(batch.recoveredLeaseCount()).isEqualTo(10);
        assertThat(batch.deliveries()).hasSize(10);
        assertThat(jdbc.sql("""
                                select count(*)
                                from telegram_outbox
                                where last_error_code = 'lease_expired'
                                """)
                        .query(Long.class)
                        .single())
                .isEqualTo(10L);
    }

    @Test
    void claimAndReloadUseStrictPrivacyCutoffAndCurrentLease() {
        long boundary =
                seedDueLead(50, NOW.minusSeconds(60), PRIVACY_CUTOFF);
        long eligible = seedDueLead(
                51, NOW.minusSeconds(60), PRIVACY_CUTOFF.plusSeconds(1));

        List<ClaimedDelivery> claims =
                outbox.recoverExpiredAndClaimDue(NOW, PRIVACY_CUTOFF, 10, LEASE);

        assertThat(claims).extracting(ClaimedDelivery::outboxId)
                .containsExactly(eligible);
        ClaimedDelivery claim = claims.getFirst();
        assertThat(outbox.reloadDeliverable(
                        eligible, claim.leaseToken(), PRIVACY_CUTOFF))
                .contains(claim.message());
        assertThat(outbox.reloadDeliverable(
                        eligible, UUID.randomUUID(), PRIVACY_CUTOFF))
                .isEmpty();
        assertThat(outbox.reloadDeliverable(
                        eligible,
                        claim.leaseToken(),
                        PRIVACY_CUTOFF.plusSeconds(2)))
                .isEmpty();
        jdbc.sql("""
                        update leads
                        set payload_fingerprint = null,
                            name = null,
                            phone = null,
                            comment = null,
                            anonymized_at = :now
                        where id = :leadId
                        """)
                .param("now", NOW.atOffset(ZoneOffset.UTC))
                .param("leadId", claim.leadId())
                .update();
        assertThat(outbox.reloadDeliverable(
                        eligible, claim.leaseToken(), PRIVACY_CUTOFF))
                .isEmpty();
        assertThat(state(boundary)).isEqualTo(OutboxState.pending);
    }

    @Test
    void privacyInvalidationIsTerminalOnlyForCurrentOwnership() {
        long outboxId = seedDueLead(
                52,
                NOW.minusSeconds(60),
                PRIVACY_CUTOFF.plusSeconds(1));
        ClaimedDelivery claim = outbox.recoverExpiredAndClaimDue(
                        NOW, PRIVACY_CUTOFF, 10, LEASE)
                .getFirst();
        Instant advancedNow = NOW.plusSeconds(2);
        Instant advancedCutoff = PRIVACY_CUTOFF.plusSeconds(2);

        assertThat(outbox.reloadDeliverable(
                        outboxId, claim.leaseToken(), advancedCutoff))
                .isEmpty();
        assertThat(outbox.resolvePrivacyInvalidation(
                        outboxId,
                        UUID.randomUUID(),
                        advancedCutoff,
                        advancedNow))
                .isFalse();
        assertThat(outbox.resolvePrivacyInvalidation(
                        outboxId,
                        claim.leaseToken(),
                        advancedCutoff,
                        advancedNow))
                .isTrue();
        assertCompleted(outboxId, OutboxState.blocked, "privacy_expired");
        assertThat(outbox.resolvePrivacyInvalidation(
                        outboxId,
                        claim.leaseToken(),
                        advancedCutoff,
                        advancedNow))
                .isTrue();
        jdbc.sql("""
                        delete from telegram_outbox
                        where id = :id
                        """)
                .param("id", outboxId)
                .update();
        assertThat(outbox.resolvePrivacyInvalidation(
                        outboxId,
                        claim.leaseToken(),
                        advancedCutoff,
                        advancedNow))
                .isFalse();
    }

    @Test
    void completionTransitionsCompareAndSetAndClearLease() {
        long deliveredId = claimed(60).outboxId();
        ClaimedDelivery delivered = currentClaim(deliveredId);
        assertThat(outbox.markDelivered(
                        deliveredId, delivered.leaseToken(), NOW.plusSeconds(1)))
                .isTrue();
        assertCompleted(deliveredId, OutboxState.delivered, null);

        ClaimedDelivery retry = claimed(61);
        assertThat(outbox.markRetry(
                        retry.outboxId(),
                        retry.leaseToken(),
                        "telegram_5xx",
                        NOW.plusSeconds(31),
                        NOW.plusSeconds(1)))
                .isTrue();
        assertCompleted(retry.outboxId(), OutboxState.retry, "telegram_5xx");

        ClaimedDelivery blocked = claimed(62);
        assertThat(outbox.markBlocked(
                        blocked.outboxId(),
                        blocked.leaseToken(),
                        "telegram_permanent_403",
                        NOW.plusSeconds(1)))
                .isTrue();
        assertCompleted(
                blocked.outboxId(),
                OutboxState.blocked,
                "telegram_permanent_403");
    }

    @Test
    void countByStateReadsEveryFixedQueueState() {
        ClaimedDelivery claim = claimed(70);

        assertThat(outbox.countByState(OutboxState.pending)).isZero();
        assertThat(outbox.countByState(OutboxState.processing)).isOne();
        assertThat(outbox.countByState(OutboxState.retry)).isZero();
        assertThat(outbox.countByState(OutboxState.blocked)).isZero();
        assertThat(outbox.countByState(OutboxState.delivered)).isZero();

        assertThat(outbox.markDelivered(
                        claim.outboxId(), claim.leaseToken(), NOW.plusSeconds(1)))
                .isTrue();
        assertThat(outbox.countByState(OutboxState.processing)).isZero();
        assertThat(outbox.countByState(OutboxState.delivered)).isOne();
    }

    @Test
    void saturatedAttemptCounterRemainsNonNegativeAndCapped() {
        long outboxId =
                seedDueLead(71, NOW.minusSeconds(1), NOW.minus(Duration.ofDays(1)));
        jdbc.sql("""
                        update telegram_outbox
                        set attempt_count = :maximum
                        where id = :id
                        """)
                .param("maximum", Integer.MAX_VALUE)
                .param("id", outboxId)
                .update();

        ClaimedDelivery claim = outbox.recoverExpiredAndClaimDue(
                        NOW, PRIVACY_CUTOFF, 1, LEASE)
                .getFirst();

        assertThat(claim.attemptCount()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void failsClosedWhenTheLockedClaimUpdateIsSkipped() {
        long outboxId =
                seedDueLead(72, NOW.minusSeconds(1), NOW.minus(Duration.ofDays(1)));
        jdbc.sql("""
                        create function test_skip_processing_claim()
                        returns trigger
                        language plpgsql
                        as $$
                        begin
                            if new.state = 'processing' then
                                return null;
                            end if;
                            return new;
                        end
                        $$
                        """)
                .update();
        jdbc.sql("""
                        create trigger test_skip_processing_claim
                        before update on telegram_outbox
                        for each row
                        execute function test_skip_processing_claim()
                        """)
                .update();

        assertThatThrownBy(() -> outbox.recoverExpiredAndClaimDue(
                        NOW, PRIVACY_CUTOFF, 1, LEASE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Locked outbox claim was not persisted");
        assertThat(state(outboxId)).isEqualTo(OutboxState.pending);
    }

    private List<ClaimedDelivery> claimAfter(CyclicBarrier barrier) throws Exception {
        barrier.await();
        List<ClaimedDelivery> claims =
                outbox.recoverExpiredAndClaimDue(NOW, PRIVACY_CUTOFF, 10, LEASE);
        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                .isFalse();
        return claims;
    }

    private ClaimedDelivery claimed(int index) {
        seedDueLead(index, NOW.minusSeconds(1), NOW.minus(Duration.ofDays(1)));
        return outbox.recoverExpiredAndClaimDue(
                        NOW, PRIVACY_CUTOFF, 1, LEASE)
                .getFirst();
    }

    private ClaimedDelivery currentClaim(long outboxId) {
        UUID leaseToken = jdbc.sql("""
                        select lease_token
                        from telegram_outbox
                        where id = :id
                        """)
                .param("id", outboxId)
                .query(UUID.class)
                .single();
        return new ClaimedDelivery(
                outboxId, 1L, leaseToken, 1, NOW.plus(LEASE), message(1L, 1));
    }

    private long seedDueLead(int index, Instant nextAttemptAt, Instant createdAt) {
        long leadId = jdbc.sql("""
                        insert into leads(
                            request_id, payload_fingerprint, name, phone,
                            source_path, intent, consented_at, created_at
                        )
                        values (
                            :requestId, decode(repeat('00', 32), 'hex'),
                            'Fictional User', '70000000000', '/fictional/',
                            'repair', :createdAt, :createdAt
                        )
                        returning id
                        """)
                .param("requestId", new UUID(0L, index + 1L))
                .param("createdAt", createdAt.atOffset(ZoneOffset.UTC))
                .query(Long.class)
                .single();
        return jdbc.sql("""
                        insert into telegram_outbox(
                            lead_id, state, next_attempt_at, created_at, updated_at
                        )
                        values (:leadId, 'pending', :nextAttemptAt, :createdAt, :createdAt)
                        returning id
                        """)
                .param("leadId", leadId)
                .param("nextAttemptAt", nextAttemptAt.atOffset(ZoneOffset.UTC))
                .param("createdAt", createdAt.atOffset(ZoneOffset.UTC))
                .query(Long.class)
                .single();
    }

    private OutboxState state(long outboxId) {
        String value = jdbc.sql("select state from telegram_outbox where id = :id")
                .param("id", outboxId)
                .query(String.class)
                .single();
        return OutboxState.valueOf(value);
    }

    private String errorCode(long outboxId) {
        return jdbc.sql("select last_error_code from telegram_outbox where id = :id")
                .param("id", outboxId)
                .query(String.class)
                .single();
    }

    private void assertCompleted(
            long outboxId, OutboxState expectedState, String expectedCode) {
        PersistedState persisted = jdbc.sql("""
                        select state, lease_token, lease_until, last_error_code
                        from telegram_outbox
                        where id = :id
                        """)
                .param("id", outboxId)
                .query((result, rowNumber) -> new PersistedState(
                        OutboxState.valueOf(result.getString("state")),
                        result.getObject("lease_token"),
                        result.getObject("lease_until"),
                        result.getString("last_error_code")))
                .single();
        assertThat(persisted.state()).isEqualTo(expectedState);
        assertThat(persisted.leaseToken()).isNull();
        assertThat(persisted.leaseUntil()).isNull();
        assertThat(persisted.lastErrorCode()).isEqualTo(expectedCode);
    }

    private static TelegramLeadMessage message(long leadId, int index) {
        return new TelegramLeadMessage(
                leadId,
                new UUID(0L, index + 1L),
                "Fictional User",
                "70000000000",
                null,
                "/fictional/",
                "repair",
                NOW.minus(Duration.ofDays(1)));
    }

    private record PersistedState(
            OutboxState state,
            Object leaseToken,
            Object leaseUntil,
            String lastErrorCode) {
    }
}
