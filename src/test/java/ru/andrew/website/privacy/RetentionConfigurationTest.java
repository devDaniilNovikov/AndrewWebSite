package ru.andrew.website.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.andrew.website.testing.TestAutoConfigurationExclusions.NO_DATABASE;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.config.TaskManagementConfigUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.andrew.website.leads.LeadAcceptanceTransaction;

@SpringBootTest(properties = NO_DATABASE)
@ActiveProfiles("test")
class RetentionConfigurationTest {
    @MockitoBean
    LeadAcceptanceTransaction transaction;

    @Autowired
    ApplicationContext context;

    @Autowired
    RetentionHeartbeat heartbeat;

    @Test
    void testProfileCreatesHeartbeatWithoutEnablingScheduling() {
        assertThat(heartbeat).isNotNull();
        assertThat(context.containsBean(
                        TaskManagementConfigUtils
                                .SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME))
                .isFalse();
    }

    @Test
    void databasePresenceActivatesRetentionScheduling() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JdbcClient> jdbc = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RetentionService> service = mock(ObjectProvider.class);
        when(jdbc.getIfAvailable()).thenReturn(mock(JdbcClient.class));

        new RetentionConfiguration.SchedulingEnabled(jdbc, service);

        verify(service).getObject();
    }

    @Test
    void missingDatabaseLeavesRetentionSchedulingDormant() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JdbcClient> jdbc = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RetentionService> service = mock(ObjectProvider.class);

        new RetentionConfiguration.SchedulingEnabled(jdbc, service);

        verify(service, never()).getObject();
    }

    @Test
    void lazyFactoriesCreateTheCompleteRetentionGraph() {
        var configuration = new RetentionConfiguration();
        var jdbc = mock(JdbcClient.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-01-30T00:00:00Z"), ZoneOffset.UTC);
        var properties = new RetentionProperties(
                Duration.ofDays(29),
                Duration.ofDays(30),
                Period.ofMonths(12),
                Duration.ofHours(1),
                100);
        var repository = configuration.jdbcRetentionRepository(jdbc, clock);
        var createdHeartbeat = configuration.retentionHeartbeat(clock);
        var metrics = configuration.retentionMetrics(
                new SimpleMeterRegistry(), createdHeartbeat, clock);

        assertThat(repository).isInstanceOf(JdbcRetentionRepository.class);
        assertThat(configuration.retentionService(
                        repository,
                        properties,
                        createdHeartbeat,
                        metrics,
                        clock))
                .isInstanceOf(RetentionService.class);
    }
}
