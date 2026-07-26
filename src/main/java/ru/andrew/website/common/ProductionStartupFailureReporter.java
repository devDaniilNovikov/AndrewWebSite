package ru.andrew.website.common;

import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ApplicationListener;
import org.springframework.util.StringUtils;

public final class ProductionStartupFailureReporter implements
        ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            ProductionStartupFailureReporter.class);
    private static final String ACTIVE_PROFILES_ARGUMENT =
            "--spring.profiles.active=";
    private static final String ACTIVE_PROFILES_PROPERTY =
            "spring.profiles.active";
    private static final String ACTIVE_PROFILES_ENVIRONMENT =
            "SPRING_PROFILES_ACTIVE";
    private static final String APPLICATION = "andrew-website";
    private static final String MESSAGE =
            "Application startup failed";

    private boolean production;
    private boolean structuredLoggingReady;

    public void prepareForArguments(
            SpringApplication application,
            String[] args) {
        String argumentProfiles = Arrays.stream(args)
                .filter(argument -> argument.startsWith(
                        ACTIVE_PROFILES_ARGUMENT))
                .map(argument -> argument.substring(
                        ACTIVE_PROFILES_ARGUMENT.length()))
                .collect(Collectors.joining(","));
        if (productionRequested(
                argumentProfiles,
                System.getProperty(ACTIVE_PROFILES_PROPERTY),
                System.getenv(ACTIVE_PROFILES_ENVIRONMENT))) {
            prepareEarlyFailure(application);
        }
    }

    public static void prepareEarlyFailure(
            SpringApplication application) {
        var reporters = application.getListeners().stream()
                .filter(ProductionStartupFailureReporter.class::isInstance)
                .map(ProductionStartupFailureReporter.class::cast)
                .toList();
        if (reporters.isEmpty()) {
            return;
        }
        reporters.forEach(reporter ->
                reporter.production = true);
        LoggingSystem.get(application.getClassLoader())
                .setLogLevel(
                        LoggingSystem.ROOT_LOGGER_NAME,
                        LogLevel.OFF);
    }

    @Override
    public void onApplicationEvent(
            ApplicationEnvironmentPreparedEvent event) {
        production = event.getEnvironment()
                .matchesProfiles("prod");
        structuredLoggingReady = production;
    }

    public void report() {
        if (!production) {
            return;
        }
        Thread.currentThread().setUncaughtExceptionHandler(
                ProductionStartupFailureReporter
                        ::suppressFailureDetails);
        if (structuredLoggingReady) {
            LOGGER.error(MESSAGE);
        } else {
            System.err.println(earlyEcsEvent());
        }
    }

    private static boolean includesProduction(String profiles) {
        if (!StringUtils.hasText(profiles)) {
            return false;
        }
        return Arrays.stream(profiles.split(","))
                .map(String::trim)
                .anyMatch("prod"::equals);
    }

    static boolean productionRequested(
            String argumentProfiles,
            String systemProfiles,
            String environmentProfiles) {
        return includesProduction(argumentProfiles)
                || includesProduction(systemProfiles)
                || includesProduction(environmentProfiles);
    }

    private static String earlyEcsEvent() {
        return "{\"@timestamp\":\""
                + Instant.now()
                + "\",\"log\":{\"level\":\"ERROR\",\"logger\":\""
                + ProductionStartupFailureReporter.class.getName()
                + "\"},\"service\":{\"name\":\""
                + APPLICATION
                + "\"},\"message\":\""
                + MESSAGE
                + "\",\"ecs\":{\"version\":\"8.11\"}}";
    }

    private static void suppressFailureDetails(
            Thread thread,
            Throwable failure) {
    }
}
