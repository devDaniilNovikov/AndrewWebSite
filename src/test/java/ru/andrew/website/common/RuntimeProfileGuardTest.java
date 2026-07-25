package ru.andrew.website.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationListener;
import org.springframework.mock.env.MockEnvironment;
import ru.andrew.website.AndrewWebsiteApplication;

class RuntimeProfileGuardTest {
    private final RuntimeProfileGuard guard = new RuntimeProfileGuard();

    @ParameterizedTest
    @ValueSource(strings = {"test", "local", "prod"})
    void acceptsEachCanonicalProfile(String profile) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);

        guard.postProcessEnvironment(environment, new SpringApplication());
    }

    @Test
    void rejectsMissingActiveProfile() {
        assertProfileFailure(catchThrowable(() -> guard.postProcessEnvironment(
                new MockEnvironment(), new SpringApplication())));
    }

    @Test
    void rejectsMultipleActiveProfiles() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local", "test");

        assertProfileFailure(catchThrowable(() -> guard.postProcessEnvironment(
                environment, new SpringApplication())));
    }

    @Test
    void rejectsUnknownActiveProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("staging");

        assertProfileFailure(catchThrowable(() -> guard.postProcessEnvironment(
                environment, new SpringApplication())));
    }

    @Test
    void runsImmediatelyAfterConfigData() {
        assertThat(guard.getOrder())
                .isEqualTo(ConfigDataEnvironmentPostProcessor.ORDER + 1);
    }

    @Test
    void springFactoriesValidationRunsBeforeApplicationContextInitialization() {
        AtomicBoolean contextInitialized = new AtomicBoolean();
        SpringApplication application =
                new SpringApplication(AndrewWebsiteApplication.class);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);
        application.setRegisterShutdownHook(false);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setDefaultProperties(
                Map.of("spring.main.lazy-initialization", "true"));
        application.addListeners(
                (ApplicationListener<ApplicationContextInitializedEvent>)
                        event -> contextInitialized.set(true));

        assertProfileFailure(catchThrowable(() -> application.run()));
        assertThat(contextInitialized).isFalse();
    }

    private static void assertProfileFailure(Throwable startupFailure) {
        Throwable root = startupFailure;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        assertThat(root)
                .isInstanceOf(ApplicationContextException.class)
                .hasMessage(RuntimeProfileGuard.MESSAGE);
    }
}
