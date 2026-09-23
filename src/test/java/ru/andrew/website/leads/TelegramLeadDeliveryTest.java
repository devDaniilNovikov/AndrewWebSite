package ru.andrew.website.leads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.andrew.website.telegram.TelegramDeliveryResult;
import ru.andrew.website.telegram.TelegramGateway;
import ru.andrew.website.telegram.TelegramLeadMessage;
import ru.andrew.website.testing.MutableClock;

class TelegramLeadDeliveryTest {
    private static final Instant CONSENTED_AT = Instant.parse("2026-09-23T10:15:30Z");

    private final RecordingGateway gateway = new RecordingGateway();
    private final LeadIdempotencyRegistry registry = new LeadIdempotencyRegistry(
            new MutableClock(CONSENTED_AT, ZoneOffset.UTC));
    private final TelegramLeadDelivery delivery = new TelegramLeadDelivery(registry, gateway);

    @Test
    void deliveredLeadIsCreatedAndRemembered() {
        NormalizedLead lead = lead(null);
        gateway.results.add(new TelegramDeliveryResult.Delivered());

        assertThat(delivery.accept(lead, fingerprint(1))).isEqualTo(AcceptanceOutcome.CREATED);

        assertThat(gateway.sent).singleElement().satisfies(message -> {
            assertThat(message.requestId()).isEqualTo(lead.requestId());
            assertThat(message.name()).isEqualTo("Иван");
            assertThat(message.phone()).isEqualTo("79991234567");
            assertThat(message.comment()).isNull();
            assertThat(message.sourcePath()).isEqualTo("/service/");
            assertThat(message.intent()).isEqualTo("maintenance");
            assertThat(message.createdAt()).isEqualTo(CONSENTED_AT);
        });
        assertThat(registry.find(lead.requestId())).isPresent();
    }

    @Test
    void sameFingerprintAfterDeliveryIsADuplicateWithoutSending() {
        NormalizedLead lead = lead("перезвоните");
        registry.remember(lead.requestId(), fingerprint(1));

        assertThat(delivery.accept(lead, fingerprint(1))).isEqualTo(AcceptanceOutcome.DUPLICATE);
        assertThat(gateway.sent).isEmpty();
    }

    @Test
    void differentFingerprintAfterDeliveryIsAConflictWithoutSending() {
        NormalizedLead lead = lead(null);
        registry.remember(lead.requestId(), fingerprint(1));

        assertThatThrownBy(() -> delivery.accept(lead, fingerprint(2)))
                .isInstanceOf(IdempotencyConflictException.class);
        assertThat(gateway.sent).isEmpty();
    }

    @Test
    void retryableFailureIsUnavailableAndNotRemembered() {
        NormalizedLead lead = lead(null);
        gateway.results.add(new TelegramDeliveryResult.Retryable("network", null));

        assertThatThrownBy(() -> delivery.accept(lead, fingerprint(1)))
                .isInstanceOf(LeadDeliveryUnavailableException.class)
                .hasMessage("Lead delivery unavailable");
        assertThat(registry.find(lead.requestId())).isEmpty();
    }

    @Test
    void permanentFailureIsUnavailableAndNotRemembered() {
        NormalizedLead lead = lead(null);
        gateway.results.add(new TelegramDeliveryResult.PermanentFailure("telegram_permanent_400"));

        assertThatThrownBy(() -> delivery.accept(lead, fingerprint(1)))
                .isInstanceOf(LeadDeliveryUnavailableException.class);
        assertThat(registry.find(lead.requestId())).isEmpty();
    }

    private static NormalizedLead lead(String comment) {
        return new NormalizedLead(
                UUID.randomUUID(),
                "Иван",
                "79991234567",
                comment,
                "/service/",
                LeadIntent.maintenance,
                CONSENTED_AT);
    }

    private static LeadFingerprint fingerprint(int seed) {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) seed);
        return new LeadFingerprint(bytes);
    }

    private static final class RecordingGateway implements TelegramGateway {
        private final List<TelegramDeliveryResult> results = new ArrayList<>();
        private final List<TelegramLeadMessage> sent = new ArrayList<>();

        @Override
        public TelegramDeliveryResult send(TelegramLeadMessage message) {
            sent.add(message);
            return results.removeFirst();
        }
    }
}
