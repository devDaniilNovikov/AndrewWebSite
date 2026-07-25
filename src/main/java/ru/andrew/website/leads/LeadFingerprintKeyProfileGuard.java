package ru.andrew.website.leads;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

public final class LeadFingerprintKeyProfileGuard implements EnvironmentPostProcessor, Ordered {
    static final String TEST_KEY_MESSAGE =
            "The test fingerprint key is not allowed outside the test profile";

    private static final int ORDER = ConfigDataEnvironmentPostProcessor.ORDER + 2;

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        String fingerprintKey = Binder.get(environment)
                .bind("app.leads.fingerprint-key", String.class)
                .orElse(null);
        LeadProperties.validateFingerprintKey(fingerprintKey);
        if (LeadProperties.TEST_KEY.equals(fingerprintKey) && !environment.matchesProfiles("test")) {
            throw new IllegalStateException(TEST_KEY_MESSAGE);
        }
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
