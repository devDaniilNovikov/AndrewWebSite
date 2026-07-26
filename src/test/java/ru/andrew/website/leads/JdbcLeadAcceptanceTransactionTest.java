package ru.andrew.website.leads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

class JdbcLeadAcceptanceTransactionTest {
    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");

    @Test
    void createdLeadEnqueuesPendingTelegramDelivery() {
        JdbcFixture fixture = new JdbcFixture();
        fixture.insertedIds(Optional.of(42L));

        AcceptanceOutcome outcome =
                fixture.transaction.accept(lead(), new LeadFingerprint(new byte[32]));

        assertThat(outcome).isEqualTo(AcceptanceOutcome.CREATED);
        verify(fixture.outbox).param("leadId", 42L);
        verify(fixture.outbox).param("now", NOW.atOffset(ZoneOffset.UTC));
        verify(fixture.outbox).update();
        verify(fixture.jdbc, never())
                .sql(argThat(sql -> sql != null && sql.contains("select payload_fingerprint")));
    }

    @Test
    void retainedRequestIdWithoutFingerprintReturnsSafeAcceptance() throws Exception {
        JdbcFixture fixture = new JdbcFixture();
        fixture.insertedIds(Optional.empty());
        fixture.existingFingerprint(null);

        AcceptanceOutcome outcome =
                fixture.transaction.accept(lead(), new LeadFingerprint(new byte[32]));

        assertThat(outcome).isEqualTo(AcceptanceOutcome.RETAINED);
    }

    @Test
    void matchingFingerprintReturnsDuplicateAndDefensivelyCopiesDatabaseBytes()
            throws Exception {
        JdbcFixture fixture = new JdbcFixture();
        fixture.insertedIds(Optional.empty());
        byte[] databaseFingerprint = fingerprintWithFirstByte(7);
        byte[] expectedFingerprint = databaseFingerprint.clone();
        fixture.existingFingerprint(databaseFingerprint);
        LeadFingerprint candidate = mock(LeadFingerprint.class);
        when(candidate.bytes()).thenAnswer(invocation -> expectedFingerprint.clone());
        List<byte[]> retainedArguments = new ArrayList<>();
        List<byte[]> retainedSnapshots = new ArrayList<>();
        when(candidate.matches(any(byte[].class))).thenAnswer(invocation -> {
            byte[] retained = invocation.getArgument(0);
            retainedArguments.add(retained);
            retainedSnapshots.add(retained.clone());
            retained[0] = 99;
            return true;
        });

        assertThat(fixture.transaction.accept(lead(), candidate))
                .isEqualTo(AcceptanceOutcome.DUPLICATE);
        assertThat(fixture.transaction.accept(lead(), candidate))
                .isEqualTo(AcceptanceOutcome.DUPLICATE);

        assertThat(databaseFingerprint[0]).isNotEqualTo(expectedFingerprint[0]);
        assertThat(retainedSnapshots)
                .allSatisfy(snapshot -> assertThat(snapshot).containsExactly(expectedFingerprint));
        assertThat(retainedArguments.get(0)).isNotSameAs(retainedArguments.get(1));
    }

    @Test
    void differentFingerprintRaisesIdempotencyConflict() throws Exception {
        JdbcFixture fixture = new JdbcFixture();
        fixture.insertedIds(Optional.empty());
        fixture.existingFingerprint(fingerprintWithFirstByte(1));

        assertThatThrownBy(() -> fixture.transaction.accept(
                        lead(), new LeadFingerprint(fingerprintWithFirstByte(2))))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void missingRowAfterConflictFailsUnavailableInsteadOfReturningRetained() {
        JdbcFixture fixture = new JdbcFixture();
        fixture.insertedIds(Optional.empty());
        fixture.missingExistingLead();

        assertThatThrownBy(() ->
                        fixture.transaction.accept(lead(), new LeadFingerprint(new byte[32])))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    private static byte[] fingerprintWithFirstByte(int firstByte) {
        byte[] fingerprint = new byte[32];
        fingerprint[0] = (byte) firstByte;
        return fingerprint;
    }

    private static NormalizedLead lead() {
        return new NormalizedLead(
                UUID.fromString("18181818-1818-4818-8818-181818181818"),
                "Иван",
                "79991234567",
                null,
                "/service/",
                LeadIntent.repair,
                NOW);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class JdbcFixture {
        private final JdbcClient jdbc = mock(JdbcClient.class);
        private final JdbcClient.StatementSpec insert = mock(JdbcClient.StatementSpec.class);
        private final JdbcClient.MappedQuerySpec<Long> insertedIds =
                mock(JdbcClient.MappedQuerySpec.class);
        private final JdbcClient.StatementSpec select = mock(JdbcClient.StatementSpec.class);
        private final JdbcClient.MappedQuerySpec<?> existingRows =
                mock(JdbcClient.MappedQuerySpec.class);
        private final JdbcClient.StatementSpec outbox = mock(JdbcClient.StatementSpec.class);
        private final JdbcLeadAcceptanceTransaction transaction =
                new JdbcLeadAcceptanceTransaction(jdbc);

        private JdbcFixture() {
            when(jdbc.sql(argThat(sql -> sql != null && sql.contains("insert into leads"))))
                    .thenReturn(insert);
            when(insert.param(anyString(), nullable(Object.class))).thenReturn(insert);
            when(insert.query(Long.class)).thenReturn(insertedIds);

            when(jdbc.sql(argThat(
                            sql -> sql != null
                                    && sql.contains("select payload_fingerprint")
                                    && sql.contains("for update"))))
                    .thenReturn(select);
            when(select.param(anyString(), any())).thenReturn(select);

            when(jdbc.sql(argThat(
                            sql -> sql != null && sql.contains("insert into telegram_outbox"))))
                    .thenReturn(outbox);
            when(outbox.param(anyString(), any())).thenReturn(outbox);
        }

        private void insertedIds(Optional<Long> ids) {
            when(insertedIds.optional()).thenReturn(ids);
        }

        private void existingFingerprint(byte[] databaseFingerprint) throws Exception {
            AtomicReference<Object> mappedExisting = new AtomicReference<>();
            when(select.query(any(RowMapper.class))).thenAnswer(invocation -> {
                if (mappedExisting.get() == null) {
                    RowMapper<?> mapper = invocation.getArgument(0);
                    ResultSet result = mock(ResultSet.class);
                    when(result.getBytes("payload_fingerprint"))
                            .thenReturn(databaseFingerprint);
                    Object existing = mapper.mapRow(result, 0);
                    mappedExisting.set(existing);
                    if (databaseFingerprint != null) {
                        databaseFingerprint[0] ^= 0x7f;
                    }
                    when(((JdbcClient.MappedQuerySpec) existingRows).optional())
                            .thenReturn(Optional.of(existing));
                }
                return existingRows;
            });
        }

        private void missingExistingLead() {
            when(select.query(any(RowMapper.class)))
                    .thenReturn((JdbcClient.MappedQuerySpec) existingRows);
            when(((JdbcClient.MappedQuerySpec) existingRows).optional())
                    .thenReturn(Optional.empty());
        }
    }
}
