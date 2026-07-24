package ru.andrew.website.testing;

public final class TestAutoConfigurationExclusions {
    // Test properties replace application.yml's exclusion value, so retain the existing
    // UserDetails exclusion while adding only the database auto-configurations.
    public static final String NO_DATABASE =
            "spring.autoconfigure.exclude="
                    + "org.springframework.boot.security.autoconfigure."
                    + "UserDetailsServiceAutoConfiguration,"
                    + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                    + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration";

    private TestAutoConfigurationExclusions() {
    }
}
