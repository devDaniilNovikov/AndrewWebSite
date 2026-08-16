package ru.andrew.website.observability;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.util.StringUtils;
import ru.andrew.website.common.ProductionStartupFailureReporter;

public final class SentryInvariantGuard
        implements EnvironmentPostProcessor, Ordered {
    public static final String MESSAGE =
            "Sentry configuration violates the telemetry boundary";

    private static final String PREFIX = "sentry.";
    private static final String APPLICATION = "andrew-website";
    private static final Set<String> VERSIONED_PROPERTIES = Set.of(
            "sentry.attach-stacktrace",
            "sentry.attach-threads",
            "sentry.capture-open-telemetry-events",
            "sentry.debug",
            "sentry.dsn",
            "sentry.enable-app-start-profiling",
            "sentry.enable-auto-session-tracking",
            "sentry.enable-cache-tracing",
            "sentry.enable-database-transaction-tracing",
            "sentry.enable-external-configuration",
            "sentry.enable-legacy-profiling",
            "sentry.enable-pretty-serialization-output",
            "sentry.enable-queue-tracing",
            "sentry.enable-spotlight",
            "sentry.enabled",
            "sentry.environment",
            "sentry.logging.enabled",
            "sentry.logs.enabled",
            "sentry.max-breadcrumbs",
            "sentry.max-feature-flags",
            "sentry.max-request-body-size",
            "sentry.metrics.enabled",
            "sentry.print-uncaught-stack-trace",
            "sentry.profile-lifecycle",
            "sentry.profile-session-sample-rate",
            "sentry.propagate-traceparent",
            "sentry.sample-rate",
            "sentry.send-client-reports",
            "sentry.send-default-pii",
            "sentry.send-modules",
            "sentry.server-name",
            "sentry.start-profiler-on-app-start",
            "sentry.strict-trace-continuation",
            "sentry.trace-options-requests",
            "sentry.trace-propagation-targets",
            "sentry.traces-sample-rate",
            "sentry.use-git-commit-id-as-release");
    private static final Pattern SENTRY_INGEST_HOST = Pattern.compile(
            "[a-z0-9-]+\\.ingest(?:\\.[a-z0-9-]+)?\\.sentry\\.io");
    private static final Pattern PUBLIC_KEY = Pattern.compile("[A-Za-z0-9]+");
    private static final Pattern PROJECT_PATH = Pattern.compile("/[0-9]+");
    private static final int ORDER =
            ConfigDataEnvironmentPostProcessor.ORDER + 5;

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application) {
        boolean production = environment.matchesProfiles("prod");
        if (production) {
            ProductionStartupFailureReporter.prepareEarlyFailure(application);
        }
        boolean valid;
        try {
            Binder binder = Binder.get(environment);
            valid = containsOnlyVersionedProperties(environment)
                    && (production
                    ? isSafeProductionConfiguration(binder)
                    : isDisabledOutsideProduction(binder));
        } catch (RuntimeException invalidConfiguration) {
            valid = false;
        }
        if (!valid) {
            throw new ApplicationContextException(MESSAGE);
        }
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    private static boolean isSafeProductionConfiguration(Binder binder) {
        return binder.bind(PREFIX + "enabled", Boolean.class).orElse(false)
                && isSafeDsn(binder.bind(PREFIX + "dsn", String.class).orElse(""))
                && "prod".equals(binder.bind(
                        PREFIX + "environment", String.class).orElse(""))
                && APPLICATION.equals(binder.bind(
                        PREFIX + "server-name", String.class).orElse(""))
                && !binder.bind(PREFIX + "release", String.class).isBound()
                && !binder.bind(
                        PREFIX + "send-default-pii", Boolean.class).orElse(true)
                && "none".equalsIgnoreCase(binder.bind(
                        PREFIX + "max-request-body-size", String.class).orElse(""))
                && binder.bind(
                        PREFIX + "max-breadcrumbs", Integer.class).orElse(-1) == 0
                && !binder.bind(PREFIX + "debug", Boolean.class).orElse(true)
                && !binder.bind(
                        PREFIX + "enable-external-configuration",
                        Boolean.class).orElse(true)
                && !binder.bind(
                        PREFIX + "enable-spotlight", Boolean.class).orElse(true)
                && !binder.bind(
                        PREFIX + "enable-pretty-serialization-output",
                        Boolean.class).orElse(true)
                && !binder.bind(
                        PREFIX + "use-git-commit-id-as-release",
                        Boolean.class).orElse(true)
                && !binder.bind(
                        PREFIX + "send-modules", Boolean.class).orElse(true)
                && !binder.bind(
                        PREFIX + "attach-stacktrace", Boolean.class).orElse(true)
                && !binder.bind(
                        PREFIX + "attach-threads", Boolean.class).orElse(true)
                && !binder.bind(
                        PREFIX + "print-uncaught-stack-trace",
                        Boolean.class).orElse(true)
                && !binder.bind(
                        PREFIX + "send-client-reports",
                        Boolean.class).orElse(true)
                && !binder.bind(
                        PREFIX + "enable-auto-session-tracking",
                        Boolean.class).orElse(true)
                && !binder.bind(
                        PREFIX + "capture-open-telemetry-events",
                        Boolean.class).orElse(true)
                && !binder.bind(
                        PREFIX + "enable-database-transaction-tracing",
                        Boolean.class).orElse(true)
                && !binder.bind(
                        PREFIX + "enable-cache-tracing",
                        Boolean.class).orElse(true)
                && !binder.bind(
                        PREFIX + "enable-queue-tracing",
                        Boolean.class).orElse(true)
                && !binder.bind(
                        PREFIX + "trace-options-requests",
                        Boolean.class).orElse(true)
                && !binder.bind(
                        PREFIX + "propagate-traceparent",
                        Boolean.class).orElse(true)
                && binder.bind(
                        PREFIX + "trace-propagation-targets",
                        org.springframework.boot.context.properties.bind.Bindable
                                .listOf(String.class))
                        .orElse(List.of())
                        .isEmpty()
                && !binder.bind(
                        PREFIX + "enable-app-start-profiling",
                        Boolean.class).orElse(true)
                && !binder.bind(
                        PREFIX + "start-profiler-on-app-start",
                        Boolean.class).orElse(true)
                && !binder.bind(
                        PREFIX + "enable-legacy-profiling",
                        Boolean.class).orElse(true)
                && binder.bind(
                        PREFIX + "max-feature-flags", Integer.class).orElse(-1)
                        == 0
                && hasExactRate(binder, "sample-rate", 1.0D)
                && hasExactRate(binder, "traces-sample-rate", 0.10D)
                && hasExactRate(binder, "profile-session-sample-rate", 1.0D)
                && "TRACE".equalsIgnoreCase(binder.bind(
                        PREFIX + "profile-lifecycle", String.class).orElse(""))
                && binder.bind(
                        PREFIX + "logs.enabled", Boolean.class).orElse(false)
                && binder.bind(
                        PREFIX + "metrics.enabled", Boolean.class).orElse(false)
                && !binder.bind(
                        PREFIX + "logging.enabled", Boolean.class).orElse(true)
                && binder.bind(
                        PREFIX + "strict-trace-continuation",
                        Boolean.class).orElse(false);
    }

    private static boolean isDisabledOutsideProduction(Binder binder) {
        return !binder.bind(PREFIX + "enabled", Boolean.class).orElse(true)
                && !StringUtils.hasText(binder.bind(
                        PREFIX + "dsn", String.class).orElse(""))
                && !binder.bind(
                        PREFIX + "logs.enabled", Boolean.class).orElse(true)
                && !binder.bind(
                        PREFIX + "metrics.enabled", Boolean.class).orElse(true)
                && !binder.bind(
                        PREFIX + "logging.enabled", Boolean.class).orElse(true);
    }

    private static boolean containsOnlyVersionedProperties(
            ConfigurableEnvironment environment) {
        for (var source : environment.getPropertySources()) {
            if ("configurationProperties".equals(source.getName())) {
                continue;
            }
            if (!(source instanceof EnumerablePropertySource<?> enumerable)) {
                continue;
            }
            for (String name : enumerable.getPropertyNames()) {
                if (isForbiddenPropertyName(source.getName(), name)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isForbiddenPropertyName(
            String sourceName, String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        if (upper.startsWith("SENTRY_")) {
            return !"SENTRY_DSN".equals(upper)
                    || !isSecretStoreDsnSource(sourceName);
        }
        String canonical = name.toLowerCase(Locale.ROOT);
        if (!canonical.startsWith(PREFIX)) {
            return false;
        }
        boolean versionedSource = isVersionedSource(sourceName);
        return !versionedSource
                || !VERSIONED_PROPERTIES.contains(canonical);
    }

    private static boolean isSecretStoreDsnSource(String sourceName) {
        return "systemEnvironment".equals(sourceName)
                || "Inlined Test Properties".equals(sourceName)
                || "mockProperties".equals(sourceName);
    }

    private static boolean isVersionedSource(String sourceName) {
        return "mockProperties".equals(sourceName)
                || sourceName.startsWith(
                        "Config resource 'class path resource [application");
    }

    private static boolean hasExactRate(
            Binder binder, String property, double expected) {
        return Double.compare(
                binder.bind(PREFIX + property, Double.class).orElse(-1D),
                expected) == 0;
    }

    private static boolean isSafeDsn(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        URI uri = URI.create(value);
        String host = uri.getHost();
        String userInfo = uri.getRawUserInfo();
        return uri.isAbsolute()
                && "https".equalsIgnoreCase(uri.getScheme())
                && host != null
                && SENTRY_INGEST_HOST.matcher(
                        host.toLowerCase(Locale.ROOT)).matches()
                && uri.getPort() == -1
                && userInfo != null
                && PUBLIC_KEY.matcher(userInfo).matches()
                && PROJECT_PATH.matcher(uri.getRawPath()).matches()
                && uri.getRawQuery() == null
                && uri.getRawFragment() == null;
    }
}
