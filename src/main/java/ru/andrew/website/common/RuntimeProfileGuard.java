package ru.andrew.website.common;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

public final class RuntimeProfileGuard implements EnvironmentPostProcessor, Ordered {
    public static final String MESSAGE =
            "Exactly one active profile is required: test, local, or prod";

    private static final Set<String> ALLOWED = Set.of("test", "local", "prod");
    private static final int ORDER = ConfigDataEnvironmentPostProcessor.ORDER + 1;

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        Set<String> active = Arrays.stream(environment.getActiveProfiles())
                .collect(Collectors.toUnmodifiableSet());
        if (active.contains("prod")) {
            ProductionStartupFailureReporter
                    .prepareEarlyFailure(application);
        }
        if (active.size() != 1 || !ALLOWED.containsAll(active)) {
            throw new ApplicationContextException(MESSAGE);
        }
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
