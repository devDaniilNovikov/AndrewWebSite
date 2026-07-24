package ru.andrew.website.leads;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
final class LeadFingerprintKeyProfileGuard implements InitializingBean {
    private static final String TEST_KEY =
            "test-only-key-material-not-for-production-0001";

    private final LeadProperties properties;
    private final Environment environment;

    LeadFingerprintKeyProfileGuard(LeadProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        if (TEST_KEY.equals(properties.fingerprintKey())
                && !environment.matchesProfiles("test")) {
            throw new IllegalStateException(
                    "The test fingerprint key is not allowed outside the test profile");
        }
    }
}
