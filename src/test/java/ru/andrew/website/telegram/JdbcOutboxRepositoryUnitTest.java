package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

class JdbcOutboxRepositoryUnitTest {
    private static final Instant NOW = Instant.parse("2026-01-30T00:00:00Z");
    private static final Instant CREATED_AT =
            Instant.parse("2026-01-29T12:00:00Z");
    private static final Instant PRIVACY_CUTOFF =
            NOW.minus(Duration.ofDays(29));
    private static final Duration LEASE = Duration.ofMinutes(2);
    private static final UUID REQUEST_ID =
            UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID LEASE_TOKEN =
            UUID.fromString("33333333-3333-4333-8333-333333333333");

    @Test
    void recoversAndClaimsMappedRowsWithIndependentLeases() throws Exception {
        JdbcClient jdbc = mock(JdbcClient.class);
        JdbcClient.StatementSpec recovery = updateSpec(1);
        JdbcClient.StatementSpec due = rowQuerySpec(
                dueRow(7L, 17L, 1),
                dueRow(8L, 18L, Integer.MAX_VALUE));
        JdbcClient.StatementSpec claim = updateSpec(1);
        when(jdbc.sql(contains("lease_expired"))).thenReturn(recovery);
        when(jdbc.sql(contains("o.id as outbox_id"))).thenReturn(due);
        when(jdbc.sql(contains("attempt_count = :attemptCount")))
                .thenReturn(claim);

        var repository = new JdbcOutboxRepository(jdbc);

        ClaimBatch batch = repository.recoverExpiredAndClaimDueWithStats(
                NOW, PRIVACY_CUTOFF, 2, LEASE);
        List<ClaimedDelivery> claimed = batch.deliveries();

        assertThat(batch.recoveredLeaseCount()).isOne();
        assertThat(claimed)
                .extracting(ClaimedDelivery::outboxId)
                .containsExactly(7L, 8L);
        assertThat(claimed)
                .extracting(ClaimedDelivery::attemptCount)
                .containsExactly(2, Integer.MAX_VALUE);
        assertThat(claimed)
                .extracting(ClaimedDelivery::leaseUntil)
                .containsOnly(NOW.plus(LEASE));
        assertThat(claimed)
                .extracting(ClaimedDelivery::leaseToken)
                .doesNotHaveDuplicates();
        assertThat(claimed.getFirst().message())
                .isEqualTo(new TelegramLeadMessage(
                        17L,
                        REQUEST_ID,
                        "Fictional Name",
                        "70000000000",
                        "Fictional comment",
                        "/fictional/",
                        "repair",
                        CREATED_AT));
        verify(recovery).update();
        verify(claim, times(2)).update();
    }

    @Test
    void failsClosedWhenLockedClaimCannotBePersisted() throws Exception {
        JdbcClient jdbc = mock(JdbcClient.class);
        JdbcClient.StatementSpec recovery = updateSpec(0);
        JdbcClient.StatementSpec due =
                rowQuerySpec(dueRow(7L, 17L, 0));
        JdbcClient.StatementSpec claim = updateSpec(0);
        when(jdbc.sql(contains("lease_expired"))).thenReturn(recovery);
        when(jdbc.sql(contains("o.id as outbox_id")))
                .thenReturn(due);
        when(jdbc.sql(contains("attempt_count = :attemptCount")))
                .thenReturn(claim);

        var repository = new JdbcOutboxRepository(jdbc);

        assertThatThrownBy(() -> repository.recoverExpiredAndClaimDue(
                        NOW, PRIVACY_CUTOFF, 1, LEASE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Locked outbox claim was not persisted");
    }

    @Test
    void reloadsAndMapsDeliverableContent() throws Exception {
        JdbcClient jdbc = mock(JdbcClient.class);
        JdbcClient.StatementSpec reload = rowQuerySpec(reloadRow(17L));
        when(jdbc.sql(contains("where o.id = :outboxId")))
                .thenReturn(reload);
        var repository = new JdbcOutboxRepository(jdbc);

        var message =
                repository.reloadDeliverable(7L, LEASE_TOKEN, PRIVACY_CUTOFF);

        assertThat(message).contains(new TelegramLeadMessage(
                17L,
                REQUEST_ID,
                "Fictional Name",
                "70000000000",
                "Fictional comment",
                "/fictional/",
                "repair",
                CREATED_AT));
    }

    @Test
    void stateTransitionsExposeCasResultAndCountQueueState() {
        JdbcClient jdbc = mock(JdbcClient.class);
        JdbcClient.StatementSpec delivered = updateSpec(1, 0);
        JdbcClient.StatementSpec retry = updateSpec(1, 0);
        JdbcClient.StatementSpec blocked = updateSpec(1, 0);
        JdbcClient.StatementSpec count = mock(
                JdbcClient.StatementSpec.class, RETURNS_SELF);
        @SuppressWarnings("unchecked")
        JdbcClient.MappedQuerySpec<Long> countQuery =
                mock(JdbcClient.MappedQuerySpec.class);
        when(count.query(Long.class)).thenReturn(countQuery);
        when(countQuery.single()).thenReturn(7L);
        when(jdbc.sql(contains("set state = 'delivered'")))
                .thenReturn(delivered);
        when(jdbc.sql(contains("set state = 'retry'")))
                .thenReturn(retry);
        when(jdbc.sql(contains("set state = 'blocked'")))
                .thenReturn(blocked);
        when(jdbc.sql(contains("select count(*)"))).thenReturn(count);
        var repository = new JdbcOutboxRepository(jdbc);

        assertThat(repository.markDelivered(7L, LEASE_TOKEN, NOW)).isTrue();
        assertThat(repository.markDelivered(7L, LEASE_TOKEN, NOW)).isFalse();
        assertThat(repository.markRetry(
                        7L,
                        LEASE_TOKEN,
                        "network",
                        NOW.plusSeconds(30),
                        NOW))
                .isTrue();
        assertThat(repository.markRetry(
                        7L,
                        LEASE_TOKEN,
                        "network",
                        NOW.plusSeconds(30),
                        NOW))
                .isFalse();
        assertThat(repository.markBlocked(
                        7L, LEASE_TOKEN, "telegram_permanent_400", NOW))
                .isTrue();
        assertThat(repository.markBlocked(
                        7L, LEASE_TOKEN, "telegram_permanent_400", NOW))
                .isFalse();
        assertThat(repository.countByState(OutboxState.retry)).isEqualTo(7L);
        verify(count).param("state", "retry");
    }

    @Test
    void approvedClaimMethodStillReturnsOnlyClaimedDeliveries()
            throws Exception {
        JdbcClient jdbc = mock(JdbcClient.class);
        JdbcClient.StatementSpec recovery = updateSpec(0);
        JdbcClient.StatementSpec due =
                rowQuerySpec(dueRow(7L, 17L, 0));
        JdbcClient.StatementSpec claim = updateSpec(1);
        when(jdbc.sql(contains("lease_expired"))).thenReturn(recovery);
        when(jdbc.sql(contains("o.id as outbox_id"))).thenReturn(due);
        when(jdbc.sql(contains("attempt_count = :attemptCount")))
                .thenReturn(claim);

        var repository = new JdbcOutboxRepository(jdbc);

        assertThat(repository.recoverExpiredAndClaimDue(
                        NOW, PRIVACY_CUTOFF, 1, LEASE))
                .singleElement()
                .extracting(ClaimedDelivery::outboxId)
                .isEqualTo(7L);
    }

    @Test
    void privacyResolutionTransitionsOwnedRowsOrConfirmsTerminalState() {
        JdbcClient jdbc = mock(JdbcClient.class);
        JdbcClient.StatementSpec privacyUpdate = updateSpec(1, 0, 0);
        JdbcClient.StatementSpec confirmation = scalarSpec(
                Boolean.TRUE, Boolean.FALSE);
        when(jdbc.sql(contains("last_error_code = 'privacy_expired'")))
                .thenReturn(privacyUpdate);
        when(jdbc.sql(contains("select exists")))
                .thenReturn(confirmation);
        var repository = new JdbcOutboxRepository(jdbc);

        assertThat(repository.resolvePrivacyInvalidation(
                        7L, LEASE_TOKEN, PRIVACY_CUTOFF, NOW))
                .isTrue();
        assertThat(repository.resolvePrivacyInvalidation(
                        7L, LEASE_TOKEN, PRIVACY_CUTOFF, NOW))
                .isTrue();
        assertThat(repository.resolvePrivacyInvalidation(
                        7L, LEASE_TOKEN, PRIVACY_CUTOFF, NOW))
                .isFalse();
    }

    private static JdbcClient.StatementSpec updateSpec(int first, int... rest) {
        JdbcClient.StatementSpec statement =
                mock(JdbcClient.StatementSpec.class, RETURNS_SELF);
        Integer[] remaining = java.util.Arrays.stream(rest)
                .boxed()
                .toArray(Integer[]::new);
        when(statement.update()).thenReturn(first, remaining);
        return statement;
    }

    @SafeVarargs
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> JdbcClient.StatementSpec scalarSpec(
            T first, T... rest) {
        JdbcClient.StatementSpec statement =
                mock(JdbcClient.StatementSpec.class, RETURNS_SELF);
        @SuppressWarnings("unchecked")
        JdbcClient.MappedQuerySpec<T> query =
                mock(JdbcClient.MappedQuerySpec.class);
        when(statement.query(any(Class.class))).thenReturn(query);
        when(query.single()).thenReturn(first, rest);
        return statement;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static JdbcClient.StatementSpec rowQuerySpec(ResultSet... rows)
            throws Exception {
        JdbcClient.StatementSpec statement =
                mock(JdbcClient.StatementSpec.class, RETURNS_SELF);
        when(statement.query(any(RowMapper.class))).thenAnswer(invocation -> {
            RowMapper mapper = invocation.getArgument(0);
            List<Object> mapped = new ArrayList<>();
            for (int index = 0; index < rows.length; index++) {
                mapped.add(mapper.mapRow(rows[index], index));
            }
            JdbcClient.MappedQuerySpec query =
                    mock(JdbcClient.MappedQuerySpec.class);
            when(query.list()).thenReturn(List.copyOf(mapped));
            when(query.optional())
                    .thenReturn(mapped.stream().findFirst());
            return query;
        });
        return statement;
    }

    private static ResultSet dueRow(
            long outboxId, long leadId, int attemptCount) throws Exception {
        ResultSet result = messageRow(leadId);
        when(result.getLong("outbox_id")).thenReturn(outboxId);
        when(result.getLong("lead_id")).thenReturn(leadId);
        when(result.getInt("attempt_count")).thenReturn(attemptCount);
        return result;
    }

    private static ResultSet reloadRow(long leadId) throws Exception {
        ResultSet result = messageRow(leadId);
        when(result.getLong("id")).thenReturn(leadId);
        return result;
    }

    private static ResultSet messageRow(long leadId) throws Exception {
        ResultSet result = mock(ResultSet.class);
        when(result.getLong("lead_id")).thenReturn(leadId);
        when(result.getObject("request_id", UUID.class))
                .thenReturn(REQUEST_ID);
        when(result.getString("name")).thenReturn("Fictional Name");
        when(result.getString("phone")).thenReturn("70000000000");
        when(result.getString("comment")).thenReturn("Fictional comment");
        when(result.getString("source_path")).thenReturn("/fictional/");
        when(result.getString("intent")).thenReturn("repair");
        when(result.getObject("created_at", OffsetDateTime.class))
                .thenReturn(CREATED_AT.atOffset(ZoneOffset.UTC));
        return result;
    }
}
