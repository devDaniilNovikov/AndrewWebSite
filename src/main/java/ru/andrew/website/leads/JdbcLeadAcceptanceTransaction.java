package ru.andrew.website.leads;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JdbcLeadAcceptanceTransaction implements LeadAcceptanceTransaction {
    private final JdbcClient jdbc;

    public JdbcLeadAcceptanceTransaction(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AcceptanceOutcome accept(NormalizedLead lead, LeadFingerprint fingerprint) {
        Instant now = lead.consentedAt();
        Optional<Long> inserted = insertLead(lead, fingerprint.bytes(), now);
        if (inserted.isPresent()) {
            insertOutbox(inserted.orElseThrow(), now);
            return AcceptanceOutcome.CREATED;
        }
        return resolveExisting(lead, fingerprint);
    }

    private Optional<Long> insertLead(
            NormalizedLead lead, byte[] fingerprint, Instant createdAt) {
        return jdbc.sql("""
                        insert into leads(
                            request_id,
                            payload_fingerprint,
                            name,
                            phone,
                            comment,
                            source_path,
                            intent,
                            consented_at,
                            created_at
                        )
                        values (
                            :requestId,
                            :fingerprint,
                            :name,
                            :phone,
                            :comment,
                            :sourcePath,
                            :intent,
                            :consentedAt,
                            :createdAt
                        )
                        on conflict (request_id) do nothing
                        returning id
                        """)
                .param("requestId", lead.requestId())
                .param("fingerprint", fingerprint)
                .param("name", lead.name())
                .param("phone", lead.phoneDigits())
                .param("comment", lead.comment())
                .param("sourcePath", lead.sourcePath())
                .param("intent", lead.intent().name())
                .param("consentedAt", asUtcTimestamp(lead.consentedAt()))
                .param("createdAt", asUtcTimestamp(createdAt))
                .query(Long.class)
                .optional();
    }

    private AcceptanceOutcome resolveExisting(
            NormalizedLead lead, LeadFingerprint candidate) {
        Optional<ExistingLead> existing = jdbc.sql("""
                        select payload_fingerprint
                        from leads
                        where request_id = :requestId
                        for update
                        """)
                .param("requestId", lead.requestId())
                .query((result, rowNumber) ->
                        new ExistingLead(result.getBytes("payload_fingerprint")))
                .optional();
        if (existing.isEmpty()) {
            throw new DataAccessResourceFailureException(
                    "Conflicting lead disappeared before idempotency resolution");
        }
        byte[] retained = existing.orElseThrow().fingerprint();
        if (retained == null) {
            return AcceptanceOutcome.RETAINED;
        }
        if (candidate.matches(retained)) {
            return AcceptanceOutcome.DUPLICATE;
        }
        throw new IdempotencyConflictException();
    }

    private void insertOutbox(long leadId, Instant now) {
        jdbc.sql("""
                        insert into telegram_outbox(
                            lead_id,
                            state,
                            next_attempt_at,
                            created_at,
                            updated_at
                        )
                        values (:leadId, 'pending', :now, :now, :now)
                        """)
                .param("leadId", leadId)
                .param("now", asUtcTimestamp(now))
                .update();
    }

    private OffsetDateTime asUtcTimestamp(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private record ExistingLead(byte[] fingerprint) {
        private ExistingLead {
            fingerprint = fingerprint == null ? null : fingerprint.clone();
        }

        @Override
        public byte[] fingerprint() {
            return fingerprint == null ? null : fingerprint.clone();
        }
    }
}
