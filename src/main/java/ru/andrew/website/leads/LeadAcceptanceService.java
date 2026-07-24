package ru.andrew.website.leads;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class LeadAcceptanceService {
    private final Validator validator;
    private final LeadNormalizer normalizer;
    private final LeadFingerprintService fingerprints;
    private final LeadAcceptanceTransaction transaction;
    private final Clock clock;

    public LeadAcceptanceService(
            Validator validator,
            LeadNormalizer normalizer,
            LeadFingerprintService fingerprints,
            LeadAcceptanceTransaction transaction,
            Clock clock) {
        this.validator = validator;
        this.normalizer = normalizer;
        this.fingerprints = fingerprints;
        this.transaction = transaction;
        this.clock = clock;
    }

    public AcceptanceOutcome accept(LeadRequest request) {
        if (request == null) {
            throw new InvalidLeadRequestException();
        }
        if (request.website() != null && !request.website().isEmpty()) {
            return AcceptanceOutcome.HONEYPOT;
        }
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        Instant now = clock.instant();
        NormalizedLead lead = normalizer.normalize(request, now);
        LeadFingerprint fingerprint = fingerprints.fingerprint(lead);
        return transaction.accept(lead, fingerprint);
    }
}
