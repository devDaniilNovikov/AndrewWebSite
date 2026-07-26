package ru.andrew.website.observability;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.registry.otlp.OtlpConfig;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsProperties;
import org.springframework.boot.task.SimpleAsyncTaskSchedulerCustomizer;
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.util.ErrorHandler;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OtlpMetricsProperties.class)
public class TelemetryConfiguration {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(TelemetryConfiguration.class);
    private static final String APPLICATION = "andrew-website";
    private static final Map<String, Map<String, Set<String>>> CONTRACTS =
            Map.of(
                    "andrew.leads.accepted",
                    Map.of("outcome", Set.of(
                            "created", "duplicate", "retained", "honeypot")),
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
                                    "permanent_failure")),
                    "andrew.telegram.delivery",
                    Map.of(
                            "outcome", Set.of(
                                    "delivered", "retry", "blocked"),
                            "reason", Set.of(
                                    "success", "network", "telegram_429",
                                    "telegram_4xx", "telegram_5xx",
                                    "telegram_unexpected", "lease_expired",
                                    "privacy_expired")),
                    "andrew.telegram.queue.depth",
                    Map.of("state", Set.of(
                            "pending", "processing", "retry",
                            "blocked", "delivered")),
                    "andrew.telegram.worker.last_success.age",
                    Map.of(),
                    "andrew.privacy.anonymized",
                    Map.of(),
                    "andrew.privacy.deleted",
                    Map.of(),
                    "andrew.privacy.last_success.age",
                    Map.of());

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

    @Bean
    OtlpConfig boundedOtlpConfig(
            OtlpMetricsProperties properties) {
        return new BoundedOtlpConfig(
                properties.isEnabled(),
                Objects.requireNonNullElse(
                        properties.getUrl(),
                        OtlpConfig.DEFAULT.url()),
                properties.getStep(),
                Map.copyOf(Objects.requireNonNullElse(
                        properties.getHeaders(), Map.of())));
    }

    @Bean("scheduledTaskErrorHandler")
    ErrorHandler scheduledTaskErrorHandler() {
        return failure -> LOGGER.error("Scheduled task failed");
    }

    @Bean
    ThreadPoolTaskSchedulerCustomizer safeThreadPoolSchedulerErrors(
            ErrorHandler scheduledTaskErrorHandler) {
        return scheduler ->
                scheduler.setErrorHandler(scheduledTaskErrorHandler);
    }

    @Bean
    SimpleAsyncTaskSchedulerCustomizer safeSimpleSchedulerErrors(
            ErrorHandler scheduledTaskErrorHandler) {
        return scheduler ->
                scheduler.setErrorHandler(scheduledTaskErrorHandler);
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

    private record BoundedOtlpConfig(
            boolean enabled,
            String url,
            Duration step,
            Map<String, String> headers)
            implements OtlpConfig {
        private BoundedOtlpConfig {
            Objects.requireNonNull(url, "url");
            Objects.requireNonNull(step, "step");
            headers = Map.copyOf(headers);
        }

        @Override
        public String get(String key) {
            return null;
        }

        @Override
        public Map<String, String> resourceAttributes() {
            return Map.of("service.name", APPLICATION);
        }

        @Override
        public boolean publishMaxGaugeForHistograms() {
            return false;
        }
    }
}
