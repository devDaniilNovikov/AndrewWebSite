package ru.andrew.website.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;

class SentryInvariantGuardTest {
    private final SentryInvariantGuard guard = new SentryInvariantGuard();

    @Test
    void acceptsTheCanonicalProductionConfiguration() {
        assertThatCode(() -> validate(safeProductionEnvironment()))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"local", "test"})
    void acceptsDisabledDestinationFreeConfigurationOutsideProduction(
            String profile) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        environment
                .withProperty("sentry.enabled", "false")
                .withProperty("sentry.logs.enabled", "false")
                .withProperty("sentry.metrics.enabled", "false")
                .withProperty("sentry.logging.enabled", "false");

        assertThatCode(() -> validate(environment))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "/1",
            "http://public-key@o1.ingest.sentry.io/1",
            "https:///1",
            "https://public-key:@o1.ingest.sentry.io/1",
            "https://o1.ingest.sentry.io/1",
            "https://public-key@localhost/1",
            "https://public-key@127.0.0.1/1",
            "https://publickey@o1.ingest.sentry.io:443/1",
            "https://publickey@o1.ingest.sentry.io/project",
            "https://publickey@o1.ingest.sentry.io/1?private=value",
            "https://publickey@o1.ingest.sentry.io/1#secret",
            "not a URI"
    })
    void rejectsMissingOrUnsafeProductionDsn(String dsn) {
        assertGenericFailure(catchThrowable(() -> validate(
                safeProductionEnvironment().withProperty("sentry.dsn", dsn))));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "sentry.enabled=false",
            "sentry.environment=staging",
            "sentry.send-default-pii=true",
            "sentry.max-request-body-size=always",
            "sentry.server-name=attacker-controlled",
            "sentry.release=attacker-controlled",
            "sentry.debug=true",
            "sentry.enable-external-configuration=true",
            "sentry.enable-spotlight=true",
            "sentry.enable-pretty-serialization-output=true",
            "sentry.use-git-commit-id-as-release=true",
            "sentry.send-modules=true",
            "sentry.sample-rate=0.5",
            "sentry.traces-sample-rate=1.0",
            "sentry.profile-session-sample-rate=0.5",
            "sentry.profile-lifecycle=MANUAL",
            "sentry.logs.enabled=false",
            "sentry.metrics.enabled=false",
            "sentry.logging.enabled=true",
            "sentry.max-breadcrumbs=1",
            "sentry.strict-trace-continuation=false"
    })
    void rejectsEveryProductionPrivacyOrSecurityOverride(String override) {
        int separator = override.indexOf('=');
        MockEnvironment environment = safeProductionEnvironment().withProperty(
                override.substring(0, separator),
                override.substring(separator + 1));

        assertGenericFailure(catchThrowable(() -> validate(environment)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "sentry.attach-stacktrace",
            "sentry.attach-threads",
            "sentry.print-uncaught-stack-trace",
            "sentry.send-client-reports",
            "sentry.enable-auto-session-tracking",
            "sentry.capture-open-telemetry-events",
            "sentry.enable-database-transaction-tracing",
            "sentry.enable-cache-tracing",
            "sentry.enable-queue-tracing",
            "sentry.trace-options-requests",
            "sentry.propagate-traceparent",
            "sentry.enable-app-start-profiling",
            "sentry.start-profiler-on-app-start",
            "sentry.enable-legacy-profiling",
            "sentry.max-feature-flags"
    })
    void rejectsMissingExplicitProductionPrivacyBoundary(String property) {
        MockEnvironment environment = safeProductionEnvironment();
        MapPropertySource properties = (MapPropertySource) environment
                .getPropertySources().get("mockProperties");
        properties.getSource().remove(property);

        assertGenericFailure(catchThrowable(() -> validate(environment)));
    }

    @Test
    void rejectsReleaseEvenWhenItComesFromTheBinderOnlySource() {
        MockEnvironment environment = safeProductionEnvironment();
        environment.getPropertySources().addFirst(
                new PropertySource<Map<String, Object>>(
                        "binderOnly",
                        Map.of("sentry.release", "fictional-release")) {
                    @Override
                    public Object getProperty(String name) {
                        return getSource().get(name);
                    }
                });

        assertGenericFailure(catchThrowable(() -> validate(environment)));
    }

    @Test
    void rejectsReleaseEvenWhenItComesFromAnAllowedVersionedSource() {
        MockEnvironment environment = safeProductionEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "Config resource 'class path resource [application-prod.yml]'",
                Map.of("sentry.release", "fictional-release")));

        assertGenericFailure(catchThrowable(() -> validate(environment)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "sentry.attach-threads",
            "sentry.cache-dir-path",
            "sentry.dist",
            "sentry.print-uncaught-stack-trace",
            "sentry.profiles-sample-rate",
            "sentry.profiling-traces-dir-path",
            "sentry.proxy.host",
            "sentry.trace-propagation-targets"
    })
    void rejectsEveryUnversionedSentryProperty(String property) {
        MockEnvironment environment = safeProductionEnvironment()
                .withProperty(property, "fictional-private-override");

        assertGenericFailure(catchThrowable(() -> validate(environment)));
    }

    @Test
    void rejectsEveryRuntimeSentryEnvironmentVariableExceptTheDsn() {
        MockEnvironment rejected = safeProductionEnvironment();
        rejected.getPropertySources().addFirst(new MapPropertySource(
                "runtimeEnvironment",
                Map.of("SENTRY_ATTACH_THREADS", "true")));
        assertGenericFailure(catchThrowable(() -> validate(rejected)));

        MockEnvironment accepted = safeProductionEnvironment();
        accepted.getPropertySources().addFirst(new MapPropertySource(
                "systemEnvironment",
                Map.of(
                        "SENTRY_DSN",
                        "https://publickey@o1.ingest.sentry.io/1")));
        assertThatCode(() -> validate(accepted)).doesNotThrowAnyException();

        MockEnvironment commandLine = safeProductionEnvironment();
        commandLine.getPropertySources().addFirst(new MapPropertySource(
                "commandLineArgs",
                Map.of("sentry.attach-threads", "false")));
        assertGenericFailure(catchThrowable(() -> validate(commandLine)));
    }

    @Test
    void acceptsDsnFromMockPropertiesForIsolatedInvariantTests() {
        MockEnvironment environment = safeProductionEnvironment();
        MapPropertySource properties = (MapPropertySource) environment
                .getPropertySources().get("mockProperties");
        properties.getSource().put(
                "SENTRY_DSN",
                "https://publickey@o1.ingest.sentry.io/1");

        assertThatCode(() -> validate(environment)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "commandLineArgs",
            "systemProperties",
            "spring.application.json",
            "Config resource 'file [/tmp/application.yml]'"
    })
    void rejectsDsnFromEveryNonSecretStoreSource(String sourceName) {
        MockEnvironment environment = safeProductionEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                sourceName,
                Map.of(
                        "sentry.dsn",
                        "https://publickey@o1.ingest.sentry.io/1")));

        assertGenericFailure(catchThrowable(() -> validate(environment)));
    }

    @Test
    void rejectsRawDsnFromCommandLineButAcceptsPackagedConfiguration() {
        MockEnvironment commandLine = safeProductionEnvironment();
        commandLine.getPropertySources().addFirst(new MapPropertySource(
                "commandLineArgs",
                Map.of(
                        "SENTRY_DSN",
                        "https://publickey@o1.ingest.sentry.io/1")));
        assertGenericFailure(catchThrowable(() -> validate(commandLine)));

        MockEnvironment packaged = safeProductionEnvironment();
        packaged.getPropertySources().addFirst(new MapPropertySource(
                "Config resource 'class path resource [application-prod.yml]'",
                Map.of("sentry.attach-threads", "false")));
        assertThatCode(() -> validate(packaged)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"local", "test"})
    void rejectsAnyDsnOrEnabledSurfaceOutsideProduction(String profile) {
        MockEnvironment withDsn = new MockEnvironment();
        withDsn.setActiveProfiles(profile);
        withDsn
                .withProperty("sentry.enabled", "false")
                .withProperty("sentry.dsn", "https://public-key@o1.ingest.sentry.io/1")
                .withProperty("sentry.logs.enabled", "false")
                .withProperty("sentry.metrics.enabled", "false")
                .withProperty("sentry.logging.enabled", "false");
        assertGenericFailure(catchThrowable(() -> validate(withDsn)));

        MockEnvironment enabled = new MockEnvironment();
        enabled.setActiveProfiles(profile);
        enabled
                .withProperty("sentry.enabled", "true")
                .withProperty("sentry.logs.enabled", "false")
                .withProperty("sentry.metrics.enabled", "false")
                .withProperty("sentry.logging.enabled", "false");
        assertGenericFailure(catchThrowable(() -> validate(enabled)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "sentry.logs.enabled=true",
            "sentry.metrics.enabled=true",
            "sentry.logging.enabled=true"
    })
    void rejectsEverySignalSurfaceOutsideProduction(String override) {
        int separator = override.indexOf('=');
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        environment
                .withProperty("sentry.enabled", "false")
                .withProperty("sentry.logs.enabled", "false")
                .withProperty("sentry.metrics.enabled", "false")
                .withProperty("sentry.logging.enabled", "false")
                .withProperty(
                        override.substring(0, separator),
                        override.substring(separator + 1));

        assertGenericFailure(catchThrowable(() -> validate(environment)));
    }

    @Test
    void runsAfterExistingProductionGuards() {
        assertThat(guard.getOrder())
                .isEqualTo(ConfigDataEnvironmentPostProcessor.ORDER + 5);
    }

    private void validate(MockEnvironment environment) {
        guard.postProcessEnvironment(environment, new SpringApplication());
    }

    private static MockEnvironment safeProductionEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment
                .withProperty("sentry.enabled", "true")
                .withProperty(
                        "sentry.dsn",
                        "https://publickey@o1.ingest.sentry.io/1")
                .withProperty("sentry.environment", "prod")
                .withProperty("sentry.send-default-pii", "false")
                .withProperty("sentry.max-request-body-size", "none")
                .withProperty("sentry.server-name", "andrew-website")
                .withProperty("sentry.debug", "false")
                .withProperty("sentry.enable-external-configuration", "false")
                .withProperty("sentry.enable-spotlight", "false")
                .withProperty(
                        "sentry.enable-pretty-serialization-output",
                        "false")
                .withProperty("sentry.use-git-commit-id-as-release", "false")
                .withProperty("sentry.send-modules", "false")
                .withProperty("sentry.attach-stacktrace", "false")
                .withProperty("sentry.attach-threads", "false")
                .withProperty("sentry.print-uncaught-stack-trace", "false")
                .withProperty("sentry.send-client-reports", "false")
                .withProperty("sentry.enable-auto-session-tracking", "false")
                .withProperty("sentry.capture-open-telemetry-events", "false")
                .withProperty(
                        "sentry.enable-database-transaction-tracing",
                        "false")
                .withProperty("sentry.enable-cache-tracing", "false")
                .withProperty("sentry.enable-queue-tracing", "false")
                .withProperty("sentry.trace-options-requests", "false")
                .withProperty("sentry.propagate-traceparent", "false")
                .withProperty("sentry.enable-app-start-profiling", "false")
                .withProperty("sentry.start-profiler-on-app-start", "false")
                .withProperty("sentry.enable-legacy-profiling", "false")
                .withProperty("sentry.max-feature-flags", "0")
                .withProperty("sentry.sample-rate", "1.0")
                .withProperty("sentry.traces-sample-rate", "0.10")
                .withProperty("sentry.profile-session-sample-rate", "1.0")
                .withProperty("sentry.profile-lifecycle", "TRACE")
                .withProperty("sentry.logs.enabled", "true")
                .withProperty("sentry.metrics.enabled", "true")
                .withProperty("sentry.logging.enabled", "false")
                .withProperty("sentry.max-breadcrumbs", "0")
                .withProperty("sentry.strict-trace-continuation", "true");
    }

    private static void assertGenericFailure(Throwable failure) {
        Throwable root = failure;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        assertThat(root)
                .isInstanceOf(ApplicationContextException.class)
                .hasMessage(SentryInvariantGuard.MESSAGE);
        assertThat(root.getMessage())
                .doesNotContain("public-key", "secret", "attacker-controlled");
    }
}
