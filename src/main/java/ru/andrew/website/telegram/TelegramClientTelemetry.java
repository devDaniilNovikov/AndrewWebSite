package ru.andrew.website.telegram;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
final class TelegramClientTelemetry {
    static final String OBSERVATION_NAME = "andrew.telegram.client";
    static final String SAFE_ROUTE = "/bot{token}/sendMessage";

    private final ObservationRegistry observationRegistry;

    TelegramClientTelemetry(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    TelegramDeliveryResult observe(
            Supplier<TelegramDeliveryResult> delivery) {
        Observation observation = Observation
                .createNotStarted(OBSERVATION_NAME, observationRegistry)
                .lowCardinalityKeyValue("method", "POST")
                .lowCardinalityKeyValue("uri", SAFE_ROUTE)
                .highCardinalityKeyValue("http.url", SAFE_ROUTE)
                .start();
        try (Observation.Scope ignored = observation.openScope()) {
            TelegramDeliveryResult result = delivery.get();
            observation.lowCardinalityKeyValue(
                    "outcome", outcome(result));
            return result;
        } finally {
            observation.stop();
        }
    }

    private String outcome(TelegramDeliveryResult result) {
        return switch (result) {
            case TelegramDeliveryResult.Delivered ignored -> "delivered";
            case TelegramDeliveryResult.Retryable ignored -> "retryable";
            case TelegramDeliveryResult.PermanentFailure ignored ->
                    "permanent_failure";
        };
    }
}
