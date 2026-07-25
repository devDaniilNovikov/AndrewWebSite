package ru.andrew.website.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static ru.andrew.website.testing.TestAutoConfigurationExclusions.NO_DATABASE;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationListener;
import org.springframework.mock.env.MockEnvironment;
import ru.andrew.website.AndrewWebsiteApplication;

class ProductionHttpInvariantGuardTest {
    private final ProductionHttpInvariantGuard guard =
            new ProductionHttpInvariantGuard();

    @Test
    void acceptsCanonicalProductionHttpConfiguration() {
        assertThatCode(() -> validate(safeProductionEnvironment(), servletApplication()))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsEquivalentNonPublicManagementConfiguration() {
        MockEnvironment environment = safeProductionEnvironment()
                .withProperty("server.port", "8080")
                .withProperty("server.address", "0.0.0.0")
                .withProperty("management.server.port", "8080")
                .withProperty("management.server.base-path", "/manage")
                .withProperty("management.endpoints.web.exposure.exclude", "env")
                .withProperty("management.endpoints.web.path-mapping.env", "environment")
                .withProperty("management.endpoints.web.path-mapping.health", "health")
                .withProperty("management.endpoints.access.default", "NONE");

        assertThatCode(() -> validate(environment, servletApplication()))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsRelaxedHealthIdAndBlankHealthMappingAsEquivalent() {
        MockEnvironment environment = safeProductionEnvironment()
                .withProperty("management.endpoints.web.exposure.include", "he-alth")
                .withProperty("management.endpoints.web.path-mapping.he-alth", "");

        assertThatCode(() -> validate(environment, servletApplication()))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidExcludedEndpointIdWithStableFailure() {
        MockEnvironment environment = safeProductionEnvironment()
                .withProperty(
                        "management.endpoints.web.exposure.exclude",
                        "not@an@endpoint");

        assertProductionFailure(catchThrowable(() ->
                validate(environment, servletApplication())));
    }

    @Test
    void rejectsInvalidPathMappingEndpointIdWithStableFailure() {
        MockEnvironment environment = safeProductionEnvironment()
                .withProperty(
                        "management.endpoints.web.path-mapping.1invalid",
                        "healthz");

        assertProductionFailure(catchThrowable(() ->
                validate(environment, servletApplication())));
    }

    @Test
    void rejectsProgrammaticNonServletApplication() {
        assertProductionFailure(catchThrowable(() -> validate(
                safeProductionEnvironment(), application(WebApplicationType.NONE))));
    }

    @Test
    void deprecatedHealthDisableCannotOverridePinnedAccess() {
        MockEnvironment environment = safeProductionEnvironment()
                .withProperty("management.endpoint.health.enabled", "false");

        assertProductionFailure(catchThrowable(() ->
                validate(environment, servletApplication())));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsafeProductionProperties")
    void rejectsUnsafeProductionHttpConfiguration(
            String description, String property, String value) {
        MockEnvironment environment =
                safeProductionEnvironment().withProperty(property, value);

        assertProductionFailure(catchThrowable(() ->
                validate(environment, servletApplication())));
    }

    @Test
    void ignoresHttpOverridesOutsideProduction() {
        MockEnvironment environment = safeProductionEnvironment();
        environment.setActiveProfiles("test");
        environment
                .withProperty("spring.main.web-application-type", "none")
                .withProperty("server.port", "-1")
                .withProperty("server.forward-headers-strategy", "framework")
                .withProperty("server.tomcat.remoteip.remote-ip-header", "X-Forwarded-For")
                .withProperty("app.web.rate-limit.enabled", "false")
                .withProperty("management.endpoints.web.base-path", "/manage")
                .withProperty("management.endpoints.web.exposure.include", "*")
                .withProperty("spring.web.error.include-stacktrace", "always");

        assertThatCode(() -> validate(environment, application(WebApplicationType.NONE)))
                .doesNotThrowAnyException();
    }

    @Test
    void runsAfterProfileAndFingerprintGuards() {
        assertThat(guard.getOrder())
                .isEqualTo(ConfigDataEnvironmentPostProcessor.ORDER + 3);
    }

    @Test
    void springFactoriesValidationRunsBeforeApplicationContextInitialization() {
        AtomicBoolean contextInitialized = new AtomicBoolean();
        SpringApplication application =
                new SpringApplication(AndrewWebsiteApplication.class);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);
        application.setRegisterShutdownHook(false);
        application.setDefaultProperties(startupProperties());
        application.addListeners(
                (ApplicationListener<ApplicationContextInitializedEvent>)
                        event -> contextInitialized.set(true));

        assertProductionFailure(catchThrowable(() -> application.run()));
        assertThat(contextInitialized).isFalse();
    }

    private void validate(
            MockEnvironment environment, SpringApplication application) {
        guard.postProcessEnvironment(environment, application);
    }

    private static MockEnvironment safeProductionEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment
                .withProperty("server.forward-headers-strategy", "none")
                .withProperty("management.endpoints.web.base-path", "/actuator")
                .withProperty("management.endpoints.web.exposure.include", "health")
                .withProperty("management.endpoint.health.access", "READ_ONLY")
                .withProperty("management.endpoint.health.show-details", "never")
                .withProperty("management.endpoint.health.probes.enabled", "true")
                .withProperty(
                        "management.endpoint.health.group.liveness.include",
                        "livenessState")
                .withProperty(
                        "management.endpoint.health.group.readiness.include",
                        "readinessState")
                .withProperty("spring.web.error.path", "/error")
                .withProperty("spring.web.error.include-exception", "false")
                .withProperty("spring.web.error.include-message", "never")
                .withProperty("spring.web.error.include-binding-errors", "never")
                .withProperty("spring.web.error.include-stacktrace", "never")
                .withProperty("spring.web.error.include-path", "never");
    }

    private static SpringApplication servletApplication() {
        return application(WebApplicationType.SERVLET);
    }

    private static SpringApplication application(WebApplicationType type) {
        SpringApplication application = new SpringApplication();
        application.setWebApplicationType(type);
        return application;
    }

    private static Map<String, Object> startupProperties() {
        String[] noDatabase = NO_DATABASE.split("=", 2);
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.profiles.active", "prod");
        properties.put(
                "LEAD_FINGERPRINT_HMAC_KEY",
                "production-http-invariant-key-material-0001");
        properties.put("spring.main.lazy-initialization", "true");
        properties.put("server.port", "-1");
        properties.put(noDatabase[0], noDatabase[1]);
        return Map.copyOf(properties);
    }

    private static Stream<Arguments> unsafeProductionProperties() {
        return Stream.of(
                property(
                        "non-web application",
                        "spring.main.web-application-type",
                        "none"),
                property("disabled HTTP listener", "server.port", "-1"),
                property("ephemeral HTTP listener", "server.port", "0"),
                property("unexpected HTTP listener", "server.port", "8081"),
                property("loopback-only HTTP listener", "server.address", "127.0.0.1"),
                property("IPv6-only HTTP listener", "server.address", "::"),
                property(
                        "native forwarded-header handling",
                        "server.forward-headers-strategy",
                        "native"),
                property(
                        "framework forwarded-header handling",
                        "server.forward-headers-strategy",
                        "framework"),
                property(
                        "Tomcat forwarded protocol header",
                        "server.tomcat.remoteip.protocol-header",
                        "X-Forwarded-Proto"),
                property(
                        "Tomcat forwarded client address header",
                        "server.tomcat.remoteip.remote-ip-header",
                        "X-Forwarded-For"),
                property(
                        "disabled rate limiter",
                        "app.web.rate-limit.enabled",
                        "false"),
                property(
                        "production local CORS origins",
                        "app.web.local-cors-origins[0]",
                        "http://localhost:3000"),
                property(
                        "relocated actuator base path",
                        "management.endpoints.web.base-path",
                        "/manage"),
                property(
                        "wildcard actuator exposure",
                        "management.endpoints.web.exposure.include",
                        "*"),
                property(
                        "additional actuator exposure",
                        "management.endpoints.web.exposure.include",
                        "health,env"),
                property(
                        "excluded health endpoint",
                        "management.endpoints.web.exposure.exclude",
                        "health"),
                property(
                        "relaxed excluded health endpoint",
                        "management.endpoints.web.exposure.exclude",
                        "he-alth"),
                property(
                        "wildcard actuator exclusion",
                        "management.endpoints.web.exposure.exclude",
                        "*"),
                property(
                        "remapped health endpoint",
                        "management.endpoints.web.path-mapping.health",
                        "healthz"),
                property(
                        "relaxed remapped health endpoint",
                        "management.endpoints.web.path-mapping.he-alth",
                        "healthz"),
                property(
                        "actuator CORS origin",
                        "management.endpoints.web.cors.allowed-origins[0]",
                        "https://attacker.example"),
                property(
                        "actuator CORS origin pattern",
                        "management.endpoints.web.cors.allowed-origin-patterns[0]",
                        "*"),
                property(
                        "separate management port",
                        "management.server.port",
                        "0"),
                property(
                        "application context path",
                        "server.servlet.context-path",
                        "/site"),
                property(
                        "dispatcher servlet path",
                        "spring.mvc.servlet.path",
                        "/web"),
                property(
                        "public exception type",
                        "spring.web.error.include-exception",
                        "true"),
                property(
                        "public error message",
                        "spring.web.error.include-message",
                        "always"),
                property(
                        "public binding errors",
                        "spring.web.error.include-binding-errors",
                        "on-param"),
                property(
                        "public stack trace",
                        "spring.web.error.include-stacktrace",
                        "always"),
                property(
                        "public request path",
                        "spring.web.error.include-path",
                        "always"),
                property(
                        "relocated error endpoint",
                        "spring.web.error.path",
                        "/failure"),
                property(
                        "public health details",
                        "management.endpoint.health.show-details",
                        "always"),
                property(
                        "public health components",
                        "management.endpoint.health.show-components",
                        "always"),
                property(
                        "liveness health details",
                        "management.endpoint.health.group.liveness.show-details",
                        "always"),
                property(
                        "readiness health components",
                        "management.endpoint.health.group.readiness.show-components",
                        "always"),
                property(
                        "disabled health access",
                        "management.endpoint.health.access",
                        "NONE"),
                property(
                        "overbroad health access",
                        "management.endpoint.health.access",
                        "UNRESTRICTED"),
                property(
                        "globally capped health access",
                        "management.endpoints.access.max-permitted",
                        "NONE"),
                property(
                        "expanded liveness membership",
                        "management.endpoint.health.group.liveness.include",
                        "*"),
                property(
                        "excluded readiness contributor",
                        "management.endpoint.health.group.readiness.exclude",
                        "readinessState"),
                property(
                        "global health status order",
                        "management.endpoint.health.status.order",
                        "up,down"),
                property(
                        "global health HTTP mapping",
                        "management.endpoint.health.status.http-mapping.down",
                        "200"),
                property(
                        "cached health response",
                        "management.endpoint.health.cache.time-to-live",
                        "1m"),
                property(
                        "liveness health status order",
                        "management.endpoint.health.group.liveness.status.order",
                        "up,down"),
                property(
                        "readiness health HTTP mapping",
                        "management.endpoint.health.group.readiness.status."
                                + "http-mapping.out-of-service",
                        "200"),
                property(
                        "additional health group path",
                        "management.endpoint.health.group.custom.additional-path",
                        "server:/healthz"),
                property(
                        "additional probe paths",
                        "management.endpoint.health.probes.add-additional-paths",
                        "true"),
                property(
                        "disabled health probes",
                        "management.endpoint.health.probes.enabled",
                        "false"));
    }

    private static Arguments property(
            String description, String property, String value) {
        return Arguments.of(description, property, value);
    }

    private static void assertProductionFailure(Throwable startupFailure) {
        Throwable root = startupFailure;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        assertThat(root)
                .isInstanceOf(ApplicationContextException.class)
                .hasMessage(ProductionHttpInvariantGuard.MESSAGE);
    }
}
