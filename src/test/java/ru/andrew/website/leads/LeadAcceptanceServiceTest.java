package ru.andrew.website.leads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.validation.Validator;
import java.time.Clock;
import org.junit.jupiter.api.Test;

class LeadAcceptanceServiceTest {
    @Test
    void honeypotClassificationPrecedesValidationNormalizationFingerprintAndTransaction() {
        Validator validator = mock(Validator.class);
        LeadNormalizer normalizer = mock(LeadNormalizer.class);
        LeadFingerprintService fingerprints = mock(LeadFingerprintService.class);
        LeadAcceptanceTransaction transaction = mock(LeadAcceptanceTransaction.class);
        Clock clock = mock(Clock.class);
        var service = new LeadAcceptanceService(
                validator, normalizer, fingerprints, transaction, clock);
        var honeypot =
                new LeadRequest(null, null, null, null, null, null, null, "filled-by-bot");

        assertThat(service.accept(honeypot)).isEqualTo(AcceptanceOutcome.HONEYPOT);
        verifyNoInteractions(validator, normalizer, fingerprints, transaction, clock);
    }
}
