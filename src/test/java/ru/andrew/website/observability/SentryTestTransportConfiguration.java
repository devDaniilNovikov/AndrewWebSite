package ru.andrew.website.observability;

import io.sentry.Hint;
import io.sentry.ISerializer;
import io.sentry.ITransportFactory;
import io.sentry.RequestDetails;
import io.sentry.SentryEnvelope;
import io.sentry.SentryEvent;
import io.sentry.SentryLogEvent;
import io.sentry.SentryMetricsEvent;
import io.sentry.SentryOptions;
import io.sentry.SentryItemType;
import io.sentry.protocol.SentryTransaction;
import io.sentry.transport.ITransport;
import io.sentry.transport.RateLimiter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        name = "test.sentry.capture-transport",
        havingValue = "true")
public class SentryTestTransportConfiguration {
    @Bean
    SentryTestTransportFactory sentryTestTransportFactory() {
        return new SentryTestTransportFactory();
    }

    @Bean
    @Primary
    SentryOptions.TracesSamplerCallback sentryTestTracesSampler() {
        return samplingContext ->
                SentryPrivacyConfiguration.sampleCanonicalHttpRoute(
                        samplingContext, 1D);
    }

    static final class SentryTestTransportFactory
            implements ITransportFactory, ITransport {
        private final CopyOnWriteArrayList<SentryEnvelope> envelopes =
                new CopyOnWriteArrayList<>();
        private volatile ISerializer serializer;

        @Override
        public @NotNull ITransport create(
                @NotNull SentryOptions options,
                @NotNull RequestDetails requestDetails) {
            serializer = options.getSerializer();
            return this;
        }

        @Override
        public void send(
                @NotNull SentryEnvelope envelope,
                @NotNull Hint hint) {
            envelopes.add(envelope);
        }

        @Override
        public void flush(long timeoutMillis) {
        }

        @Override
        public RateLimiter getRateLimiter() {
            return null;
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public void close(boolean isRestarting) throws IOException {
        }

        List<SentryLogEvent> logs() {
            return envelopes.stream()
                    .flatMap(envelope -> envelopeItems(envelope).stream())
                    .map(item -> {
                        try {
                            return item.getLogs(serializer);
                        } catch (Exception invalidEnvelope) {
                            throw new AssertionError(invalidEnvelope);
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .flatMap(events -> events.getItems().stream())
                    .toList();
        }

        List<SentryEvent> events() {
            return envelopes.stream()
                    .flatMap(envelope -> envelopeItems(envelope).stream())
                    .map(item -> {
                        try {
                            return item.getEvent(serializer);
                        } catch (Exception invalidEnvelope) {
                            throw new AssertionError(invalidEnvelope);
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        String serializedPayload() {
            StringBuilder payload = new StringBuilder();
            envelopes.stream()
                    .flatMap(envelope -> envelopeItems(envelope).stream())
                    .forEach(item -> {
                        try {
                            payload.append(new String(
                                    item.getData(),
                                    java.nio.charset.StandardCharsets.ISO_8859_1));
                        } catch (Exception invalidEnvelope) {
                            throw new AssertionError(invalidEnvelope);
                        }
                    });
            return payload.toString();
        }

        List<SentryMetricsEvent> metrics() {
            return envelopes.stream()
                    .flatMap(envelope -> envelopeItems(envelope).stream())
                    .map(item -> {
                        try {
                            return item.getMetrics(serializer);
                        } catch (Exception invalidEnvelope) {
                            throw new AssertionError(invalidEnvelope);
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .flatMap(events -> events.getItems().stream())
                    .toList();
        }

        List<SentryTransaction> transactions() {
            return envelopes.stream()
                    .flatMap(envelope -> envelopeItems(envelope).stream())
                    .map(item -> {
                        try {
                            return item.getTransaction(serializer);
                        } catch (Exception invalidEnvelope) {
                            throw new AssertionError(invalidEnvelope);
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        int envelopeCount() {
            return envelopes.size();
        }

        List<SentryItemType> itemTypesSince(int envelopeIndex) {
            return envelopes.stream()
                    .skip(envelopeIndex)
                    .flatMap(envelope -> envelopeItems(envelope).stream())
                    .map(item -> item.getHeader().getType())
                    .toList();
        }

        private static List<io.sentry.SentryEnvelopeItem> envelopeItems(
                SentryEnvelope envelope) {
            List<io.sentry.SentryEnvelopeItem> items =
                    new java.util.ArrayList<>();
            envelope.getItems().forEach(items::add);
            return List.copyOf(items);
        }
    }
}
