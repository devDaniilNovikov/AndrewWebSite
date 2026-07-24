package ru.andrew.website.leads;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

class JdbcLeadAcceptanceTransactionTest {
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void missingRowAfterConflictFailsUnavailableInsteadOfReturningRetained() {
        JdbcClient jdbc = mock(JdbcClient.class);
        JdbcClient.StatementSpec insert = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<Long> insertedIds =
                mock(JdbcClient.MappedQuerySpec.class);
        JdbcClient.StatementSpec select = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec existingRows = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbc.sql(argThat(sql -> sql != null && sql.contains("insert into leads"))))
                .thenReturn(insert);
        when(insert.param(anyString(), nullable(Object.class))).thenReturn(insert);
        when(insert.query(Long.class)).thenReturn(insertedIds);
        when(insertedIds.optional()).thenReturn(Optional.empty());
        when(jdbc.sql(argThat(
                        sql -> sql != null
                                && sql.contains("select payload_fingerprint")
                                && sql.contains("for update"))))
                .thenReturn(select);
        when(select.param(anyString(), any())).thenReturn(select);
        when(select.query(any(RowMapper.class))).thenReturn(existingRows);
        when(existingRows.optional()).thenReturn(Optional.empty());

        var transaction = new JdbcLeadAcceptanceTransaction(jdbc);

        assertThatThrownBy(() -> transaction.accept(lead(), new LeadFingerprint(new byte[32])))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    private static NormalizedLead lead() {
        return new NormalizedLead(
                UUID.fromString("18181818-1818-4818-8818-181818181818"),
                "Иван",
                "79991234567",
                null,
                "/service/",
                LeadIntent.repair,
                Instant.parse("2026-07-24T12:00:00Z"));
    }
}
