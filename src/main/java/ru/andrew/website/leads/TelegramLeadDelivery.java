package ru.andrew.website.leads;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.andrew.website.telegram.TelegramDeliveryResult;
import ru.andrew.website.telegram.TelegramGateway;
import ru.andrew.website.telegram.TelegramLeadMessage;

/**
 * Delivers an accepted lead to the owner's Telegram chat before the visitor gets a
 * response. Concurrent submissions that reuse one request identifier are not serialized:
 * the form never sends them in parallel, and a sequential retry is answered from
 * {@link LeadIdempotencyRegistry} without a second message.
 */
@Component
public final class TelegramLeadDelivery implements LeadDelivery {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(TelegramLeadDelivery.class);

    private final LeadIdempotencyRegistry registry;
    private final TelegramGateway gateway;

    public TelegramLeadDelivery(
            LeadIdempotencyRegistry registry, TelegramGateway gateway) {
        this.registry = registry;
        this.gateway = gateway;
    }

    @Override
    public AcceptanceOutcome accept(NormalizedLead lead, LeadFingerprint fingerprint) {
        Optional<LeadFingerprint> delivered = registry.find(lead.requestId());
        if (delivered.isPresent()) {
            if (delivered.orElseThrow().matches(fingerprint.bytes())) {
                return AcceptanceOutcome.DUPLICATE;
            }
            throw new IdempotencyConflictException();
        }
        return switch (gateway.send(message(lead))) {
            case TelegramDeliveryResult.Delivered ignored -> {
                registry.remember(lead.requestId(), fingerprint);
                yield AcceptanceOutcome.CREATED;
            }
            case TelegramDeliveryResult.Retryable retryable ->
                    throw unavailable(retryable.code());
            case TelegramDeliveryResult.PermanentFailure failure ->
                    throw unavailable(failure.code());
        };
    }

    private static TelegramLeadMessage message(NormalizedLead lead) {
        return new TelegramLeadMessage(
                lead.requestId(),
                lead.name(),
                lead.phoneDigits(),
                lead.comment(),
                lead.sourcePath(),
                lead.intent().name(),
                lead.consentedAt());
    }

    private static LeadDeliveryUnavailableException unavailable(String code) {
        LOGGER.error("Telegram delivery failed: {}", code);
        return new LeadDeliveryUnavailableException();
    }
}
