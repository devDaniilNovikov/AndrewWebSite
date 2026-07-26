package ru.andrew.website.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class ProductionStartupFailureReporterTest {
    @Test
    void productionDetectionCoversEveryCanonicalPropertySource() {
        assertThat(ProductionStartupFailureReporter
                        .productionRequested(
                                "local, prod", null, null))
                .isTrue();
        assertThat(ProductionStartupFailureReporter
                        .productionRequested(
                                "", "prod", null))
                .isTrue();
        assertThat(ProductionStartupFailureReporter
                        .productionRequested(
                                "", "", "prod"))
                .isTrue();
        assertThat(ProductionStartupFailureReporter
                        .productionRequested(
                                "test", "local", null))
                .isFalse();
    }

    @Test
    void earlyProductionFailureIsOneGenericEcsEvent(
            CapturedOutput output) {
        SpringApplication application =
                new SpringApplication();
        ProductionStartupFailureReporter reporter =
                new ProductionStartupFailureReporter();
        application.addListeners(reporter);
        LoggingSystem logging = LoggingSystem.get(
                application.getClassLoader());
        LogLevel previousRoot = logging.getLoggerConfiguration(
                        LoggingSystem.ROOT_LOGGER_NAME)
                .getEffectiveLevel();
        Thread thread = Thread.currentThread();
        Thread.UncaughtExceptionHandler previousHandler =
                thread.getUncaughtExceptionHandler();

        try {
            reporter.prepareForArguments(
                    application,
                    new String[] {
                            "--spring.profiles.active=local,prod"
                    });
            reporter.report();

            Thread.UncaughtExceptionHandler safeHandler =
                    thread.getUncaughtExceptionHandler();
            assertThat(safeHandler).isNotSameAs(previousHandler);
            safeHandler.uncaughtException(
                    thread,
                    new IllegalStateException(
                            "fictional-private-detail"));
            assertThat(output.getAll())
                    .contains(
                            "\"message\":\"Application startup failed\"",
                            "\"ecs\":{\"version\":\"8.11\"}")
                    .doesNotContain(
                            "fictional-private-detail");
        } finally {
            thread.setUncaughtExceptionHandler(previousHandler);
            logging.setLogLevel(
                    LoggingSystem.ROOT_LOGGER_NAME,
                    previousRoot);
        }
    }

    @Test
    void repeatedProfileArgumentsFailClosed(
            CapturedOutput output) {
        SpringApplication application =
                new SpringApplication();
        ProductionStartupFailureReporter reporter =
                new ProductionStartupFailureReporter();
        application.addListeners(reporter);
        LoggingSystem logging = LoggingSystem.get(
                application.getClassLoader());
        LogLevel previousRoot = logging.getLoggerConfiguration(
                        LoggingSystem.ROOT_LOGGER_NAME)
                .getEffectiveLevel();
        Thread thread = Thread.currentThread();
        Thread.UncaughtExceptionHandler previousHandler =
                thread.getUncaughtExceptionHandler();

        try {
            reporter.prepareForArguments(
                    application,
                    new String[] {
                            "--spring.profiles.active=test",
                            "--spring.profiles.active=prod"
                    });
            reporter.report();

            assertThat(output.getAll())
                    .contains(
                            "\"message\":\"Application startup failed\"");
        } finally {
            thread.setUncaughtExceptionHandler(previousHandler);
            logging.setLogLevel(
                    LoggingSystem.ROOT_LOGGER_NAME,
                    previousRoot);
        }
    }

    @Test
    void missingReporterAndNonProductionArgumentsAreNoOps() {
        SpringApplication withoutReporter =
                new SpringApplication();
        ProductionStartupFailureReporter.prepareEarlyFailure(
                withoutReporter);
        ProductionStartupFailureReporter reporter =
                new ProductionStartupFailureReporter();
        reporter.prepareForArguments(
                withoutReporter,
                new String[] {"--spring.profiles.active=test"});
        Thread.UncaughtExceptionHandler previous =
                Thread.currentThread().getUncaughtExceptionHandler();

        reporter.report();

        assertThat(Thread.currentThread()
                        .getUncaughtExceptionHandler())
                .isSameAs(previous);
    }
}
