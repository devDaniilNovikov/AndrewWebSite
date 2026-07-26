package ru.andrew.website.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsProperties;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ErrorHandler;

class TelemetryConfigurationTest {
    private final TelemetryConfiguration configuration =
            new TelemetryConfiguration();
    private final ErrorHandler errorHandler =
            configuration.scheduledTaskErrorHandler();

    @Test
    void installsTheSafeHandlerOnBothSchedulerImplementations() {
        ThreadPoolTaskScheduler threadPool =
                mock(ThreadPoolTaskScheduler.class);
        SimpleAsyncTaskScheduler simple =
                mock(SimpleAsyncTaskScheduler.class);

        configuration.safeThreadPoolSchedulerErrors(errorHandler)
                .customize(threadPool);
        configuration.safeSimpleSchedulerErrors(errorHandler)
                .customize(simple);

        verify(threadPool).setErrorHandler(errorHandler);
        verify(simple).setErrorHandler(errorHandler);
    }

    @Test
    void otlpConfigKeepsOnlyCanonicalTransportAndResourceData() {
        OtlpMetricsProperties properties =
                new OtlpMetricsProperties();
        properties.setEnabled(true);
        properties.setUrl(
                "https://collector.invalid/v1/metrics");
        properties.setHeaders(Map.of(
                "Authorization", "Bearer fictional"));
        properties.setStep(Duration.ofSeconds(30));

        var config = configuration.boundedOtlpConfig(properties);

        assertThat(config.enabled()).isTrue();
        assertThat(config.url())
                .isEqualTo(
                        "https://collector.invalid/v1/metrics");
        assertThat(config.headers()).containsExactly(
                Map.entry(
                        "Authorization", "Bearer fictional"));
        assertThat(config.step())
                .isEqualTo(Duration.ofSeconds(30));
        assertThat(config.resourceAttributes())
                .containsExactly(
                        Map.entry(
                                "service.name", "andrew-website"));
        assertThat(config.publishMaxGaugeForHistograms())
                .isFalse();
    }
}
