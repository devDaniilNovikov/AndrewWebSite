package ru.andrew.website.privacy;

import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import ru.andrew.website.testing.MutableClock;

@TestConfiguration(proxyBeanMethods = false)
public class RetentionTestConfiguration {
    @Bean
    @Primary
    MutableClock retentionClock() {
        return new MutableClock(
                Instant.parse("2026-01-30T00:00:00Z"),
                ZoneOffset.UTC);
    }
}
