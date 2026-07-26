package ru.andrew.website.observability;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component("dbReadiness")
public final class DatabaseReadinessHealthIndicator
        implements HealthIndicator {
    private static final String VALIDATION_QUERY = "select 1";

    private final ObjectProvider<JdbcClient> jdbc;

    public DatabaseReadinessHealthIndicator(
            ObjectProvider<JdbcClient> jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Health health() {
        try {
            JdbcClient client = jdbc.getIfAvailable();
            if (client == null) {
                return Health.down().build();
            }
            Integer result = client.sql(VALIDATION_QUERY)
                    .query(Integer.class)
                    .single();
            return Integer.valueOf(1).equals(result)
                    ? Health.up().build()
                    : Health.down().build();
        } catch (RuntimeException unavailable) {
            return Health.down().build();
        }
    }
}
