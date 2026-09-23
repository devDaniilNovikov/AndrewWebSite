package ru.andrew.website.leads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.validation.Validator;
import java.time.Clock;
import org.junit.jupiter.api.Test;

class LeadAcceptanceServiceTest {
    @Test
    void nullRequestIsRejectedBeforeAnyCollaboratorIsUsed() {
        Validator validator = mock(Validator.class);
        LeadNormalizer normalizer = mock(LeadNormalizer.class);
        LeadFingerprintService fingerprints = mock(LeadFingerprintService.class);
        LeadDelivery delivery = mock(LeadDelivery.class);
        Clock clock = mock(Clock.class);
        var service = new LeadAcceptanceService(
                validator, normalizer, fingerprints, delivery, clock);

        assertThatThrownBy(() -> service.accept(null))
                .isInstanceOf(InvalidLeadRequestException.class);
        verifyNoInteractions(validator, normalizer, fingerprints, delivery, clock);
    }

    @Test
    void honeypotClassificationPrecedesValidationNormalizationFingerprintAndDelivery() {
        Validator validator = mock(Validator.class);
        LeadNormalizer normalizer = mock(LeadNormalizer.class);
        LeadFingerprintService fingerprints = mock(LeadFingerprintService.class);
        LeadDelivery delivery = mock(LeadDelivery.class);
        Clock clock = mock(Clock.class);
        var service = new LeadAcceptanceService(
                validator, normalizer, fingerprints, delivery, clock);
        var honeypot =
                new LeadRequest(null, null, null, null, null, null, null, "filled-by-bot");

        assertThat(service.accept(honeypot)).isEqualTo(AcceptanceOutcome.HONEYPOT);
        verifyNoInteractions(validator, normalizer, fingerprints, delivery, clock);
    }
}
