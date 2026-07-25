package ru.andrew.website.leads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.mock.env.MockEnvironment;
import ru.andrew.website.AndrewWebsiteApplication;

class LeadPropertiesTest {
    @Test
    void requiresEnoughUtf8KeyMaterialWithoutExposingIt() {
        assertThatThrownBy(() -> new LeadProperties("x".repeat(31)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UTF-8 bytes")
                .hasMessageNotContaining("x".repeat(31));

        LeadProperties properties = new LeadProperties("секрет".repeat(8));

        assertThat(properties.toString()).doesNotContain(properties.fingerprintKey());
    }

    @Test
    void earlyGuardRejectsMissingBlankAndShortKeys() {
        LeadFingerprintKeyProfileGuard guard = new LeadFingerprintKeyProfileGuard();

        for (String value : new String[] {null, " ", "x".repeat(31)}) {
            MockEnvironment environment = environment("prod", value);

            assertThatThrownBy(() -> guard.postProcessEnvironment(
                            environment, new SpringApplication()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("app.leads.fingerprint-key must contain at least 32 UTF-8 bytes");
        }
    }

    @Test
    void canonicalTestKeyIsRejectedOutsideTheTestProfile() {
        LeadFingerprintKeyProfileGuard guard = new LeadFingerprintKeyProfileGuard();

        assertThatCode(() -> guard.postProcessEnvironment(
                        environment("test", LeadProperties.TEST_KEY),
                        new SpringApplication()))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.postProcessEnvironment(
                        environment("prod", LeadProperties.TEST_KEY),
                        new SpringApplication()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(LeadFingerprintKeyProfileGuard.TEST_KEY_MESSAGE)
                .hasMessageNotContaining(LeadProperties.TEST_KEY);
    }

    @Test
    void keyGuardRunsAfterProfileValidationAndBeforeHttpValidation() {
        assertThat(new LeadFingerprintKeyProfileGuard().getOrder())
                .isEqualTo(ConfigDataEnvironmentPostProcessor.ORDER + 2);
    }

    @Test
    void springFactoriesKeyValidationRunsBeforeApplicationContextInitialization() {
        AtomicBoolean contextInitialized = new AtomicBoolean();
        SpringApplication application =
                new SpringApplication(AndrewWebsiteApplication.class);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);
        application.setRegisterShutdownHook(false);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setDefaultProperties(Map.of(
                "spring.profiles.active", "prod",
                "LEAD_FINGERPRINT_HMAC_KEY", LeadProperties.TEST_KEY,
                "spring.main.lazy-initialization", "true"));
        application.addListeners(
                (ApplicationListener<ApplicationContextInitializedEvent>)
                        event -> contextInitialized.set(true));

        Throwable failure = catchThrowable(() -> application.run());

        assertThat(failure)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(LeadFingerprintKeyProfileGuard.TEST_KEY_MESSAGE);
        assertThat(failure.toString()).doesNotContain(LeadProperties.TEST_KEY);
        assertThat(contextInitialized).isFalse();
    }

    private static MockEnvironment environment(String profile, String key) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        if (key != null) {
            environment.withProperty("app.leads.fingerprint-key", key);
        }
        return environment;
    }
}
