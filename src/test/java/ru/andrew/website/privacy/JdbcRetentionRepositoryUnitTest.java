package ru.andrew.website.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

class JdbcRetentionRepositoryUnitTest {
    private static final Instant NOW =
            Instant.parse("2026-01-30T00:00:00Z");
    private static final Instant EXPIRE_CUTOFF =
            NOW.minus(Duration.ofDays(29));
    private static final Instant DELETE_CUTOFF =
            Instant.parse("2025-01-30T00:00:00Z");

    @Test
    void expiresOneBoundedLockedBatchWithUtcParameters()
            throws Exception {
        JdbcClient jdbc = mock(JdbcClient.class);
        JdbcClient.StatementSpec statement = retentionResultSpec(2, 3);
        when(jdbc.sql(contains("update telegram_outbox o")))
                .thenReturn(statement);
        var repository = repository(jdbc);

        assertThat(repository.expireBatch(EXPIRE_CUTOFF, 7))
                .isEqualTo(new RetentionBatchResult(2, 3));
        verify(jdbc).sql(contains("for update skip locked"));
        verify(jdbc).sql(contains("o.state <> 'delivered'"));
        verify(statement).param(
                "cutoff", EXPIRE_CUTOFF.atOffset(ZoneOffset.UTC));
        verify(statement).param("limit", 7);
        verify(statement).param(
                "anonymizedAt", NOW.atOffset(ZoneOffset.UTC));
    }

    @Test
    void deletesOneBoundedLockedBatchWithUtcParameters() {
        JdbcClient jdbc = mock(JdbcClient.class);
        JdbcClient.StatementSpec statement =
                mock(JdbcClient.StatementSpec.class, RETURNS_SELF);
        @SuppressWarnings("unchecked")
        JdbcClient.MappedQuerySpec<Integer> query =
                mock(JdbcClient.MappedQuerySpec.class);
        when(statement.query(Integer.class)).thenReturn(query);
        when(query.single()).thenReturn(4);
        when(jdbc.sql(contains("delete from leads l")))
                .thenReturn(statement);
        var repository = repository(jdbc);

        assertThat(repository.deleteBatch(DELETE_CUTOFF, 5))
                .isEqualTo(4);
        verify(jdbc).sql(contains("for update skip locked"));
        verify(statement).param(
                "cutoff", DELETE_CUTOFF.atOffset(ZoneOffset.UTC));
        verify(statement).param("limit", 5);
    }

    @Test
    void provesCompletionAcrossBothIndexedCutoffs() {
        JdbcClient jdbc = mock(JdbcClient.class);
        JdbcClient.StatementSpec statement =
                mock(JdbcClient.StatementSpec.class, RETURNS_SELF);
        @SuppressWarnings("unchecked")
        JdbcClient.MappedQuerySpec<Boolean> query =
                mock(JdbcClient.MappedQuerySpec.class);
        when(statement.query(Boolean.class)).thenReturn(query);
        when(query.single()).thenReturn(true);
        when(jdbc.sql(contains("select not (")))
                .thenReturn(statement);
        var repository = repository(jdbc);

        assertThat(repository.isComplete(
                        EXPIRE_CUTOFF, DELETE_CUTOFF))
                .isTrue();
        verify(jdbc).sql(contains("created_at <= :expireCutoff"));
        verify(jdbc).sql(contains("anonymized_at <= :deleteCutoff"));
        verify(statement).param(
                "expireCutoff",
                EXPIRE_CUTOFF.atOffset(ZoneOffset.UTC));
        verify(statement).param(
                "deleteCutoff",
                DELETE_CUTOFF.atOffset(ZoneOffset.UTC));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static JdbcClient.StatementSpec retentionResultSpec(
            int anonymized, int blocked) throws Exception {
        JdbcClient.StatementSpec statement =
                mock(JdbcClient.StatementSpec.class, RETURNS_SELF);
        ResultSet result = mock(ResultSet.class);
        when(result.getInt("anonymized_count")).thenReturn(anonymized);
        when(result.getInt("blocked_count")).thenReturn(blocked);
        when(statement.query(any(RowMapper.class))).thenAnswer(invocation -> {
            RowMapper mapper = invocation.getArgument(0);
            Object mapped = mapper.mapRow(result, 0);
            JdbcClient.MappedQuerySpec query =
                    mock(JdbcClient.MappedQuerySpec.class);
            when(query.single()).thenReturn(mapped);
            return query;
        });
        return statement;
    }

    private static JdbcRetentionRepository repository(
            JdbcClient jdbc) {
        return new JdbcRetentionRepository(
                jdbc, Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
