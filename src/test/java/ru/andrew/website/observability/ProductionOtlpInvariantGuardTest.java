package ru.andrew.website.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationListener;
import org.springframework.mock.env.MockEnvironment;
import ru.andrew.website.AndrewWebsiteApplication;

class ProductionOtlpInvariantGuardTest {
    private static final String PREFIX =
            "management.otlp.metrics.export.";

    private final ProductionOtlpInvariantGuard guard =
            new ProductionOtlpInvariantGuard();

    @Test
    void acceptsTheCanonicalProductionExporterConfiguration() {
        assertThatCode(() -> validate(safeEnvironment()))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"local", "test"})
    void acceptsOnlyDisabledExporterOutsideProduction(String profile) {
        MockEnvironment environment = safeEnvironment();
        environment.setActiveProfiles(profile);
        environment
                .withProperty(PREFIX + "enabled", "false")
                .withProperty(PREFIX + "url", "http://user:@invalid/?x#y")
                .withProperty(PREFIX + "headers.X-Private", "private-marker")
                .withProperty(PREFIX + "step", "1h");

        assertThatCode(() -> validate(environment))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"local", "test"})
    void rejectsEnabledExporterOutsideProduction(String profile) {
        MockEnvironment environment = safeEnvironment();
        environment.setActiveProfiles(profile);

        assertFailure(catchThrowable(() -> validate(environment)));
    }

    @Test
    void rejectsDisabledExporterWithOnlyAGenericFailure() {
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(
                        PREFIX + "enabled", "false"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "http://collector.invalid/v1/metrics",
            "/v1/metrics",
            "https:///v1/metrics",
            "https://user:@collector.invalid/v1/metrics",
            "https://collector.invalid/v1/metrics?tenant=private",
            "https://collector.invalid/v1/metrics#private",
            "not a URI"
    })
    void rejectsEveryUnsafeExporterUrl(String value) {
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(
                        PREFIX + "url", value))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "Bearer value\runsafe", "Bearer value\nunsafe"})
    void rejectsBlankOrMultilineAuthorization(String value) {
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(
                        PREFIX + "headers.Authorization", value))));
    }

    @Test
    void rejectsMissingWrongOrAdditionalHeaders() {
        MockEnvironment missing = baseEnvironment()
                .withProperty(PREFIX + "enabled", "true")
                .withProperty(
                        PREFIX + "url",
                        "https://collector.invalid/v1/metrics")
                .withProperty(PREFIX + "step", "30s");
        assertFailure(catchThrowable(() -> validate(missing)));

        assertFailure(catchThrowable(() -> validate(
                baseEnvironment()
                        .withProperty(PREFIX + "enabled", "true")
                        .withProperty(
                                PREFIX + "url",
                                "https://collector.invalid/v1/metrics")
                        .withProperty(
                                PREFIX + "headers.authorization",
                                "Bearer fictional")
                        .withProperty(PREFIX + "step", "30s"))));

        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(
                        PREFIX + "headers.X-Extra", "not-allowed"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1m", "29s", "not-a-duration"})
    void rejectsNonCanonicalExportInterval(String value) {
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(
                        PREFIX + "step", value))));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "management.opentelemetry.enabled",
            "management.opentelemetry.map-environment-variables"
    })
    void rejectsOtherOpenTelemetryExportSurfaces(String property) {
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(property, "true"))));
    }

    @Test
    void rejectsCustomOpenTelemetryResourceAttributes() {
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(
                        "management.opentelemetry"
                                + ".resource-attributes.private",
                        "not-allowed"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "logging.level.io.micrometer.core.instrument"
                    + ".push",
            "logging.level.io.micrometer.registry.otlp"
    })
    void rejectsUnsafeInternalExporterLogging(String property) {
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(property, "INFO"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "logging.level.io.micrometer.core.instrument"
                    + ".push.PushMeterRegistry",
            "logging.level.io.micrometer.registry.otlp"
                    + ".OtlpMeterRegistry",
            "logging.level.org.flywaydb.core.FlywayExecutor"
    })
    void rejectsMoreSpecificSensitiveLoggerOverrides(String property) {
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(property, "TRACE"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "logging.level.root",
            "logging.level.org.springframework.web",
            "logging.level.org.springframework.jdbc.core",
            "logging.level.com.zaxxer.hikari",
            "logging.level.org.postgresql",
            "logging.level.sql",
            "logging.level.ru.andrew.website.common"
                    + ".ProductionStartupFailureReporter",
            "logging.level.ru.andrew.website.observability"
                    + ".TelemetryConfiguration"
    })
    void rejectsEveryNonCanonicalProductionLogLevel(String property) {
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(property, "TRACE"))));
    }

    @Test
    void rejectsCaseVariantLoggerCollision() {
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(
                        "logging.level.RU.andrew.website.observability"
                                + ".TelemetryConfiguration",
                        "TRACE"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"debug", "trace"})
    void rejectsFrameworkDebugAndTraceModes(String property) {
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(property, "true"))));
    }

    @Test
    void rejectsCustomLoggingGroups() {
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment()
                        .withProperty(
                                "logging.group.secret",
                                "io.micrometer.registry.otlp"
                                        + ".OtlpMeterRegistry")
                        .withProperty(
                                "logging.level.secret", "OFF"))));
    }

    @Test
    void rejectsNonEcsOrExternalProductionLoggingConfiguration() {
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(
                        "logging.structured.format.console", "logstash"))));
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(
                        "logging.config", "classpath:unsafe.xml"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "logging.structured.json.customizer",
            "logging.structured.json.exclude",
            "logging.structured.json.include",
            "logging.structured.json.rename.message",
            "logging.structured.ecs.service.name",
            "logging.structured.format.file",
            "logging.file.name",
            "logging.file.path"
    })
    void rejectsAlternativeStructuredLoggingSurfaces(String property) {
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(
                        property, "fictional-private-value"))));
    }

    @Test
    void rejectsStructuredJsonMembersThatCouldInjectSecrets() {
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(
                        "logging.structured.json.add.authorization",
                        "Bearer fictional-private-authorization"))));
    }

    @Test
    void rejectsStructuredContextAndApplicationNameOverrides() {
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(
                        "logging.structured.json.context.include",
                        "true"))));
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(
                        "spring.application.name",
                        "fictional-private-service"))));
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(
                        "spring.application.version",
                        "fictional-private-version"))));
    }

    @Test
    void acceptsOnlyTheBuildDerivedApplicationVersion() {
        assertThat(ProductionOtlpInvariantGuard
                        .isSafeApplicationVersion(
                                null, null))
                .isTrue();
        assertThat(ProductionOtlpInvariantGuard
                        .isSafeApplicationVersion(
                                "1.2.3", "1.2.3"))
                .isTrue();
        assertThat(ProductionOtlpInvariantGuard
                        .isSafeApplicationVersion(
                                "1.2.3", ""))
                .isFalse();
        assertThat(ProductionOtlpInvariantGuard
                        .isSafeApplicationVersion(
                                "1.2.3", "9.9.9"))
                .isFalse();
    }

    @Test
    void rejectsBannerAndStartupIdentityLogging() {
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(
                        "spring.main.banner-mode", "console"))));
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(
                        "spring.main.log-startup-info", "true"))));
    }

    @Test
    void runsAfterTheExistingProductionBoundaryGuards() {
        assertThat(guard.getOrder())
                .isEqualTo(ConfigDataEnvironmentPostProcessor.ORDER + 4);
    }

    @Test
    void springFactoriesValidationRunsBeforeContextInitialization() {
        AtomicBoolean initialized = new AtomicBoolean();
        SpringApplication application =
                new SpringApplication(AndrewWebsiteApplication.class);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);
        application.setRegisterShutdownHook(false);
        application.setDefaultProperties(Map.of(
                "spring.profiles.active", "prod",
                "LEAD_FINGERPRINT_HMAC_KEY",
                "production-observability-key-material-0001",
                PREFIX + "enabled", "false",
                "spring.main.lazy-initialization", "true"));
        application.addListeners(
                (ApplicationListener<ApplicationContextInitializedEvent>)
                        event -> initialized.set(true));

        assertFailure(catchThrowable(application::run));
        assertThat(initialized).isFalse();
    }

    private void validate(MockEnvironment environment) {
        guard.postProcessEnvironment(
                environment, new SpringApplication());
    }

    private static MockEnvironment safeEnvironment() {
        return baseEnvironment()
                .withProperty(PREFIX + "enabled", "true")
                .withProperty(
                        PREFIX + "url",
                        "https://collector.invalid/v1/metrics")
                .withProperty(
                        PREFIX + "headers.Authorization",
                        "Bearer fictional-authorization")
                .withProperty(PREFIX + "step", "30s")
                .withProperty(
                        "management.opentelemetry.enabled", "false")
                .withProperty(
                        "management.opentelemetry"
                                + ".map-environment-variables",
                        "false")
                .withProperty(
                        "logging.level.io.micrometer.core.instrument"
                                + ".push",
                        "OFF")
                .withProperty(
                        "logging.level.io.micrometer.registry.otlp",
                        "OFF")
                .withProperty(
                        "logging.level.org.flywaydb",
                        "OFF")
                .withProperty(
                        "logging.level.org.springframework.web",
                        "OFF")
                .withProperty(
                        "logging.level.org.springframework.jdbc",
                        "OFF")
                .withProperty(
                        "logging.level.org.apache.catalina",
                        "OFF")
                .withProperty(
                        "logging.level.com.zaxxer.hikari",
                        "OFF")
                .withProperty(
                        "logging.level.org.postgresql",
                        "OFF")
                .withProperty(
                        "logging.level.root",
                        "OFF")
                .withProperty(
                        "logging.level.ru.andrew.website.observability"
                                + ".TelemetryConfiguration",
                        "ERROR")
                .withProperty(
                        "logging.level.ru.andrew.website.common"
                                + ".ProductionStartupFailureReporter",
                        "ERROR")
                .withProperty(
                        "logging.structured.json.context.include",
                        "false")
                .withProperty(
                        "logging.structured.format.console",
                        "ecs")
                .withProperty(
                        "spring.application.name",
                        "andrew-website")
                .withProperty(
                        "spring.main.banner-mode",
                        "off")
                .withProperty(
                        "spring.main.log-startup-info",
                        "false");
    }

    private static MockEnvironment baseEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment;
    }

    private static void assertFailure(Throwable failure) {
        Throwable root = failure;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        assertThat(root)
                .isInstanceOf(ApplicationContextException.class)
                .hasMessage(ProductionOtlpInvariantGuard.MESSAGE)
                .hasNoCause();
        assertThat(failure.toString()).doesNotContain(
                "collector.invalid",
                "fictional-authorization",
                "user:password",
                "tenant=private");
    }
}
