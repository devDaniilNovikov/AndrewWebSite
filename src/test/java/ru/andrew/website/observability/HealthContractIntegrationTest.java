package ru.andrew.website.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.andrew.website.privacy.RetentionHeartbeat;
import ru.andrew.website.telegram.WorkerHeartbeat;
import ru.andrew.website.testing.MutableClock;
import ru.andrew.website.testing.PostgresTestConfiguration;

@Tag("database")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({
        PostgresTestConfiguration.class,
        HealthContractIntegrationTest.ClockConfiguration.class
})
class HealthContractIntegrationTest {
    private static final Instant NOW =
            Instant.parse("2026-07-26T00:00:00Z");

    @Autowired
    MockMvc mvc;

    @Autowired
    WorkerHeartbeat workerHeartbeat;

    @Autowired
    RetentionHeartbeat retentionHeartbeat;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    MutableClock clock;

    @BeforeEach
    void postgresFixtureIsAvailableAndMigrated() {
        clock.setInstant(NOW);
        assertThat(jdbc.sql("select 1").query(Integer.class).single())
                .isEqualTo(1);
    }

    @Test
    void staleWorkerMakesOnlyReadinessDown() throws Exception {
        workerHeartbeat.success(NOW.minusSeconds(46));

        mvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(content().string("{\"status\":\"UP\"}"));
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(content().string("{\"status\":\"DOWN\"}"));
    }

    @Test
    void migratedPostgresAndFreshWorkerMakeReadinessUpWithoutDetails()
            throws Exception {
        workerHeartbeat.success(NOW);

        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(content().string("{\"status\":\"UP\"}"));
    }

    @Test
    void retentionStalenessDoesNotAffectReadiness() throws Exception {
        workerHeartbeat.success(NOW);
        retentionHeartbeat.success(NOW.minus(Duration.ofHours(3)));

        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(content().string("{\"status\":\"UP\"}"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfiguration {
        @Bean
        @Primary
        MutableClock observabilityClock() {
            return new MutableClock(NOW, ZoneOffset.UTC);
        }
    }
}
