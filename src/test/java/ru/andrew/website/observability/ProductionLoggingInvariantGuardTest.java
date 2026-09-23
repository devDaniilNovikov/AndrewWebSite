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

class ProductionLoggingInvariantGuardTest {
    private final ProductionLoggingInvariantGuard guard =
            new ProductionLoggingInvariantGuard();

    @Test
    void acceptsTheCanonicalProductionLoggingConfiguration() {
        assertThatCode(() -> validate(safeEnvironment()))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsAdditionalLoggersThatStaySilent() {
        assertThatCode(() -> validate(safeEnvironment().withProperty(
                        "logging.level.com.example.fictional", "OFF")))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"local", "test"})
    void ignoresLoggingOverridesOutsideProduction(String profile) {
        MockEnvironment environment = safeEnvironment();
        environment.setActiveProfiles(profile);
        environment
                .withProperty("logging.level.root", "DEBUG")
                .withProperty("logging.structured.format.console", "logstash")
                .withProperty("debug", "true");

        assertThatCode(() -> validate(environment))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "logging.level.root",
            "logging.level.org.springframework.web",
            "logging.level.org.springframework.web.servlet",
            "logging.level.org.apache.catalina",
            "logging.level.sql",
            "logging.level.ru.andrew.website.common"
                    + ".ProductionStartupFailureReporter",
            "logging.level.ru.andrew.website.leads"
                    + ".TelegramLeadDelivery"
    })
    void rejectsEveryNonCanonicalProductionLogLevel(String property) {
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(property, "TRACE"))));
    }

    @Test
    void rejectsCaseVariantLoggerCollision() {
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(
                        "logging.level.RU.andrew.website.leads"
                                + ".TelegramLeadDelivery",
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
                                "ru.andrew.website.leads")
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
    void unbindableLoggingSettingFailsClosed() {
        assertFailure(catchThrowable(() -> validate(
                safeEnvironment().withProperty(
                        "spring.main.log-startup-info",
                        "fictional-not-a-boolean"))));
    }

    @Test
    void acceptsOnlyTheBuildDerivedApplicationVersion() {
        assertThat(ProductionLoggingInvariantGuard
                        .isSafeApplicationVersion(
                                null, null))
                .isTrue();
        assertThat(ProductionLoggingInvariantGuard
                        .isSafeApplicationVersion(
                                "1.2.3", "1.2.3"))
                .isTrue();
        assertThat(ProductionLoggingInvariantGuard
                        .isSafeApplicationVersion(
                                "1.2.3", ""))
                .isFalse();
        assertThat(ProductionLoggingInvariantGuard
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

    // Default properties rank below application-prod.yml, so the violation is one that
    // no bundled profile configures.
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
                "debug", "true",
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
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment
                .withProperty(
                        "logging.level.org.springframework.web",
                        "OFF")
                .withProperty(
                        "logging.level.org.apache.catalina",
                        "OFF")
                .withProperty(
                        "logging.level.root",
                        "OFF")
                .withProperty(
                        "logging.level.ru.andrew.website.leads"
                                + ".TelegramLeadDelivery",
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

    private static void assertFailure(Throwable failure) {
        Throwable root = failure;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        assertThat(root)
                .isInstanceOf(ApplicationContextException.class)
                .hasMessage(ProductionLoggingInvariantGuard.MESSAGE)
                .hasNoCause();
        assertThat(failure.toString()).doesNotContain(
                "fictional-private-authorization",
                "fictional-private-value");
    }
}
