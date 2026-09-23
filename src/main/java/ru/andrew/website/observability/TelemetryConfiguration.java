package ru.andrew.website.observability;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class TelemetryConfiguration {
    private static final String APPLICATION = "andrew-website";
    private static final Map<String, Map<String, Set<String>>> CONTRACTS =
            Map.of(
                    "andrew.leads.accepted",
                    Map.of("outcome", Set.of(
                            "created", "duplicate", "honeypot")),
                    "andrew.leads.rejected",
                    Map.of("reason", Set.of(
                            "validation", "conflict", "payload",
                            "media_type", "rate_limit", "unavailable")),
                    "andrew.telegram.client",
                    Map.of(
                            "method", Set.of("POST"),
                            "uri", Set.of("/bot{token}/sendMessage"),
                            "outcome", Set.of(
                                    "delivered", "retryable",
                                    "permanent_failure")));

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    MeterRegistryCustomizer<MeterRegistry> boundedTelemetry(
            Environment environment) {
        String profile = environment.getActiveProfiles()[0];
        return registry -> registry.config()
                .commonTags(
                        "application", APPLICATION,
                        "profile", profile)
                .meterFilter(new CanonicalTelemetryFilter(profile));
    }

    private record CanonicalTelemetryFilter(String profile)
            implements MeterFilter {
        @Override
        public Meter.Id map(Meter.Id id) {
            if ("andrew.telegram.client".equals(id.getName())
                    && "none".equals(id.getTag("error"))) {
                return id.replaceTags(id.getTags().stream()
                        .filter(tag -> !"error".equals(tag.getKey()))
                        .map(tag -> Tag.of(
                                tag.getKey(), tag.getValue()))
                        .toList());
            }
            return id;
        }

        @Override
        public MeterFilterReply accept(Meter.Id id) {
            Map<String, Set<String>> business =
                    CONTRACTS.get(id.getName());
            if (business == null
                    || id.getTags().size() != business.size() + 2
                    || !APPLICATION.equals(id.getTag("application"))
                    || !profile.equals(id.getTag("profile"))) {
                return MeterFilterReply.DENY;
            }
            boolean validBusinessTags = business.entrySet().stream()
                    .allMatch(entry -> entry.getValue().contains(
                            id.getTag(entry.getKey())));
            return validBusinessTags
                    ? MeterFilterReply.ACCEPT
                    : MeterFilterReply.DENY;
        }
    }
}
