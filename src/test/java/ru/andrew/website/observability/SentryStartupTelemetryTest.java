package ru.andrew.website.observability;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.sentry.IScopes;
import io.sentry.logger.ILoggerApi;
import io.sentry.metrics.IMetricsApi;
import io.sentry.metrics.SentryMetricsParameters;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;

class SentryStartupTelemetryTest {
    @Test
    void emitsExactlyOneSafeLogAndCounterEvenIfReadyEventRepeats() {
        IScopes scopes = mock(IScopes.class);
        ILoggerApi logger = mock(ILoggerApi.class);
        IMetricsApi metrics = mock(IMetricsApi.class);
        when(scopes.logger()).thenReturn(logger);
        when(scopes.metrics()).thenReturn(metrics);
        SentryStartupTelemetry telemetry = new SentryStartupTelemetry(scopes);
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);

        telemetry.onApplicationEvent(event);
        telemetry.onApplicationEvent(event);

        verify(logger, times(1)).info(SentryPrivacyConfiguration.STARTUP_LOG);
        verify(metrics, times(1)).count(
                eq(SentryPrivacyConfiguration.STARTUP_METRIC),
                eq(1D),
                isNull(),
                any(SentryMetricsParameters.class));
    }
}
