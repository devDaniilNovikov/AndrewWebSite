package ru.andrew.website.telegram;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micrometer.observation.ObservationRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.OptionalLong;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public final class TelegramRestClientGateway implements TelegramGateway {
    private static final Duration MAX_RETRY_AFTER = Duration.ofHours(6);
    private static final String EXPIRED_DEADLINE =
            "Telegram delivery deadline expired before HTTP call";

    private final RestClient restClient;
    private final TelegramClientProperties properties;
    private final TelegramMessageFormatter formatter;
    private final TelegramRetryAfterParser retryAfterParser;
    private final TelegramClientTelemetry telemetry;
    private final Clock clock;

    public TelegramRestClientGateway(
            RestClient.Builder restClientBuilder,
            TelegramClientProperties properties,
            TelegramMessageFormatter formatter,
            TelegramRetryAfterParser retryAfterParser,
            TelegramClientTelemetry telemetry,
            Clock clock) {
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl().toString())
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
        this.properties = properties;
        this.formatter = formatter;
        this.retryAfterParser = retryAfterParser;
        this.telemetry = telemetry;
        this.clock = clock;
    }

    @Override
    public TelegramDeliveryResult send(
            TelegramLeadMessage message, Instant latestStart) {
        String text = formatter.format(message);
        return telemetry.observe(() -> send(text, latestStart));
    }

    private TelegramDeliveryResult send(
            String text, Instant latestStart) {
        if (!clock.instant().isBefore(latestStart)) {
            throw new IllegalStateException(EXPIRED_DEADLINE);
        }
        try {
            return restClient
                    .post()
                    .uri("/bot{token}/sendMessage", properties.botToken())
                    .body(new TelegramSendMessageRequest(
                            properties.chatId(),
                            text))
                    .exchange((request, response) ->
                            classify(response.getStatusCode(), response.getBody()));
        } catch (RestClientException ignored) {
            return new TelegramDeliveryResult.Retryable("network", null);
        }
    }

    private TelegramDeliveryResult classify(
            HttpStatusCode status,
            java.io.InputStream body) {
        if (status.is2xxSuccessful()) {
            return new TelegramDeliveryResult.Delivered();
        }
        if (status.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return new TelegramDeliveryResult.Retryable(
                    "telegram_429", retryAfter(body));
        }
        if (status.is4xxClientError()) {
            return new TelegramDeliveryResult.PermanentFailure(
                    "telegram_permanent_" + status.value());
        }
        if (status.is5xxServerError()) {
            return new TelegramDeliveryResult.Retryable("telegram_5xx", null);
        }
        return new TelegramDeliveryResult.Retryable(
                "telegram_unexpected", null);
    }

    private Duration retryAfter(java.io.InputStream body) {
        OptionalLong parsed = retryAfterParser.parseSeconds(body);
        if (parsed.isEmpty()) {
            return null;
        }
        long boundedSeconds =
                Math.min(parsed.getAsLong(), MAX_RETRY_AFTER.toSeconds());
        return Duration.ofSeconds(boundedSeconds);
    }

    private record TelegramSendMessageRequest(
            @JsonProperty("chat_id") String chatId,
            String text) {
        @Override
        public String toString() {
            return "TelegramSendMessageRequest[content=<redacted>]";
        }
    }
}
