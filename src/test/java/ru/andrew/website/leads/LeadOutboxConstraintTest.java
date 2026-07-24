package ru.andrew.website.leads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import ru.andrew.website.testing.PostgresTestConfiguration;

@Tag("database")
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class LeadOutboxConstraintTest {
    @Autowired
    JdbcClient jdbc;

    @Test
    void enforcesLeadUniquenessAndValueConstraints() {
        UUID requestId = UUID.randomUUID();
        insertActiveLead(requestId, 32, "70000000000", "repair");

        assertThatThrownBy(() -> insertActiveLead(requestId, 32, "70000000000", "repair"))
                .isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() ->
                        insertActiveLead(UUID.randomUUID(), 32, "70000000000", "unknown"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() ->
                        insertActiveLead(UUID.randomUUID(), 31, "70000000000", "maintenance"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() ->
                        insertActiveLead(UUID.randomUUID(), 33, "70000000000", "maintenance"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() ->
                        insertActiveLead(UUID.randomUUID(), 32, "700000", "repair"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() ->
                        insertActiveLead(UUID.randomUUID(), 32, "7000000000000000", "repair"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enforcesLeadPrivacyShapesAndAcceptsAnonymization() {
        assertThatThrownBy(() -> jdbc.sql("""
                                insert into leads(
                                    request_id,
                                    payload_fingerprint,
                                    name,
                                    phone,
                                    source_path,
                                    intent,
                                    consented_at,
                                    created_at
                                )
                                values (
                                    :requestId,
                                    decode(repeat('00', 32), 'hex'),
                                    null,
                                    '70000000000',
                                    '/test/',
                                    'repair',
                                    now(),
                                    now()
                                )
                                """)
                        .param("requestId", UUID.randomUUID())
                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbc.sql("""
                                insert into leads(
                                    request_id,
                                    payload_fingerprint,
                                    name,
                                    phone,
                                    comment,
                                    source_path,
                                    intent,
                                    consented_at,
                                    created_at,
                                    anonymized_at
                                )
                                values (
                                    :requestId,
                                    null,
                                    null,
                                    null,
                                    'retained comment',
                                    '/test/',
                                    'maintenance',
                                    now(),
                                    now(),
                                    now()
                                )
                                """)
                        .param("requestId", UUID.randomUUID())
                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);

        long anonymizedLeadId = jdbc.sql("""
                        insert into leads(
                            request_id,
                            payload_fingerprint,
                            name,
                            phone,
                            comment,
                            source_path,
                            intent,
                            consented_at,
                            created_at,
                            anonymized_at
                        )
                        values (
                            :requestId,
                            null,
                            null,
                            null,
                            null,
                            '/test/',
                            'maintenance',
                            now(),
                            now(),
                            now()
                        )
                        returning id
                        """)
                .param("requestId", UUID.randomUUID())
                .query(Long.class)
                .single();

        assertThat(anonymizedLeadId).isPositive();
    }

    @Test
    void enforcesOutboxUniquenessForeignKeyStateAndLeaseShapes() {
        long leadId = insertActiveLead(UUID.randomUUID(), 32, "70000000000", "repair");
        insertPendingOutbox(leadId);

        assertThatThrownBy(() -> insertPendingOutbox(leadId))
                .isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> insertPendingOutbox(Long.MAX_VALUE))
                .isInstanceOf(DataIntegrityViolationException.class);

        long invalidStateLead = insertActiveLead(
                UUID.randomUUID(), 32, "70000000000", "repair");
        assertThatThrownBy(() -> jdbc.sql("""
                                insert into telegram_outbox(
                                    lead_id,
                                    state,
                                    next_attempt_at,
                                    created_at,
                                    updated_at
                                )
                                values (:leadId, 'unknown', now(), now(), now())
                                """)
                        .param("leadId", invalidStateLead)
                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);

        long negativeAttemptLead = insertActiveLead(
                UUID.randomUUID(), 32, "70000000000", "repair");
        assertThatThrownBy(() -> jdbc.sql("""
                                insert into telegram_outbox(
                                    lead_id,
                                    state,
                                    attempt_count,
                                    next_attempt_at,
                                    created_at,
                                    updated_at
                                )
                                values (:leadId, 'pending', -1, now(), now(), now())
                                """)
                        .param("leadId", negativeAttemptLead)
                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);

        long missingLeaseLead = insertActiveLead(
                UUID.randomUUID(), 32, "70000000000", "repair");
        assertThatThrownBy(() -> jdbc.sql("""
                                insert into telegram_outbox(
                                    lead_id,
                                    state,
                                    next_attempt_at,
                                    created_at,
                                    updated_at
                                )
                                values (:leadId, 'processing', now(), now(), now())
                                """)
                        .param("leadId", missingLeaseLead)
                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);

        long pendingLeaseLead = insertActiveLead(
                UUID.randomUUID(), 32, "70000000000", "repair");
        assertThatThrownBy(() -> jdbc.sql("""
                                insert into telegram_outbox(
                                    lead_id,
                                    state,
                                    next_attempt_at,
                                    lease_token,
                                    lease_until,
                                    created_at,
                                    updated_at
                                )
                                values (
                                    :leadId,
                                    'pending',
                                    now(),
                                    '22222222-2222-4222-8222-222222222222',
                                    now(),
                                    now(),
                                    now()
                                )
                                """)
                        .param("leadId", pendingLeaseLead)
                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);

        long missingDeliveryLead = insertActiveLead(
                UUID.randomUUID(), 32, "70000000000", "repair");
        assertThatThrownBy(() -> jdbc.sql("""
                                insert into telegram_outbox(
                                    lead_id,
                                    state,
                                    next_attempt_at,
                                    created_at,
                                    updated_at
                                )
                                values (:leadId, 'delivered', now(), now(), now())
                                """)
                        .param("leadId", missingDeliveryLead)
                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void acceptsValidOutboxShapesAndCascadesLeadDeletion() {
        long processingLead = insertActiveLead(
                UUID.randomUUID(), 32, "70000000000", "repair");
        jdbc.sql("""
                        insert into telegram_outbox(
                            lead_id,
                            state,
                            next_attempt_at,
                            lease_token,
                            lease_until,
                            created_at,
                            updated_at
                        )
                        values (
                            :leadId,
                            'processing',
                            now(),
                            '33333333-3333-4333-8333-333333333333',
                            now(),
                            now(),
                            now()
                        )
                        """)
                .param("leadId", processingLead)
                .update();

        long deliveredLead = insertActiveLead(
                UUID.randomUUID(), 32, "70000000000", "maintenance");
        jdbc.sql("""
                        insert into telegram_outbox(
                            lead_id,
                            state,
                            next_attempt_at,
                            created_at,
                            updated_at,
                            delivered_at
                        )
                        values (:leadId, 'delivered', now(), now(), now(), now())
                        """)
                .param("leadId", deliveredLead)
                .update();

        jdbc.sql("delete from leads where id = :leadId")
                .param("leadId", processingLead)
                .update();

        long remaining = jdbc.sql("""
                        select count(*)
                        from telegram_outbox
                        where lead_id = :leadId
                        """)
                .param("leadId", processingLead)
                .query(Long.class)
                .single();
        assertThat(remaining).isZero();
    }

    private long insertActiveLead(
            UUID requestId, int fingerprintBytes, String phone, String intent) {
        return jdbc.sql("""
                        insert into leads(
                            request_id,
                            payload_fingerprint,
                            name,
                            phone,
                            source_path,
                            intent,
                            consented_at,
                            created_at
                        )
                        values (
                            :requestId,
                            decode(repeat('00', :fingerprintBytes), 'hex'),
                            'Test Lead',
                            :phone,
                            '/test/',
                            :intent,
                            now(),
                            now()
                        )
                        returning id
                        """)
                .param("requestId", requestId)
                .param("fingerprintBytes", fingerprintBytes)
                .param("phone", phone)
                .param("intent", intent)
                .query(Long.class)
                .single();
    }

    private void insertPendingOutbox(long leadId) {
        jdbc.sql("""
                        insert into telegram_outbox(
                            lead_id,
                            state,
                            next_attempt_at,
                            created_at,
                            updated_at
                        )
                        values (:leadId, 'pending', now(), now(), now())
                        """)
                .param("leadId", leadId)
                .update();
    }
}
