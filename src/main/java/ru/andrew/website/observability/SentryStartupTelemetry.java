package ru.andrew.website.observability;

import io.sentry.IScopes;
import io.sentry.metrics.SentryMetricsParameters;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public final class SentryStartupTelemetry
        implements ApplicationListener<ApplicationReadyEvent> {
    private final IScopes scopes;
    private final AtomicBoolean emitted = new AtomicBoolean();

    public SentryStartupTelemetry(IScopes scopes) {
        this.scopes = scopes;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (emitted.compareAndSet(false, true)) {
            scopes.logger().info(SentryPrivacyConfiguration.STARTUP_LOG);
            scopes.metrics().count(
                    SentryPrivacyConfiguration.STARTUP_METRIC,
                    1D,
                    null,
                    new SentryMetricsParameters());
        }
    }
}
