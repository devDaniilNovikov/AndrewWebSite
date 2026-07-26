package ru.andrew.website.privacy;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@Import(RetentionConfiguration.SchedulingEnabled.class)
public class RetentionConfiguration {
    @Bean
    @Lazy
    JdbcRetentionRepository jdbcRetentionRepository(
            JdbcClient jdbc, Clock clock) {
        return new JdbcRetentionRepository(jdbc, clock);
    }

    @Bean
    RetentionHeartbeat retentionHeartbeat(Clock clock) {
        return new RetentionHeartbeat(clock);
    }

    @Bean
    @Lazy
    RetentionMetrics retentionMetrics(
            MeterRegistry registry,
            RetentionHeartbeat heartbeat,
            Clock clock) {
        return new RetentionMetrics(registry, heartbeat, clock);
    }

    @Bean
    @Lazy
    RetentionService retentionService(
            RetentionRepository repository,
            RetentionProperties properties,
            RetentionHeartbeat heartbeat,
            RetentionMetrics metrics,
            Clock clock) {
        return new RetentionService(
                repository,
                properties,
                heartbeat,
                metrics,
                clock);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    @Profile("!test")
    static class SchedulingEnabled {
        SchedulingEnabled(
                ObjectProvider<JdbcClient> jdbc,
                ObjectProvider<RetentionService> service) {
            if (jdbc.getIfAvailable() != null) {
                service.getObject();
            }
        }
    }
}
