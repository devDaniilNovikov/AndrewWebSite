package ru.andrew.website.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import ru.andrew.website.AndrewWebsiteApplication;

class RuntimeProfileGuardTest {
    private static final String MESSAGE =
            "Exactly one active profile is required: test, local, or prod";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AndrewWebsiteApplication.class)
            .withPropertyValues("spring.main.web-application-type=none");

    @Test
    void missingActiveProfileFailsStartup() {
        runner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(MESSAGE);
        });
    }

    @Test
    void multipleActiveProfilesFailStartup() {
        runner.withPropertyValues("spring.profiles.active=local,test").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(MESSAGE);
        });
    }

    @Test
    void unknownActiveProfileFailsStartup() {
        runner.withPropertyValues("spring.profiles.active=staging").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(MESSAGE);
        });
    }
}
