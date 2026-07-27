package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.andrew.website.testing.TestAutoConfigurationExclusions.NO_DATABASE;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.client.autoconfigure.HttpClientsProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.config.TaskManagementConfigUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.andrew.website.leads.LeadAcceptanceTransaction;

@SpringBootTest(properties = NO_DATABASE)
@ActiveProfiles("test")
class TelegramSchedulingConfigurationTest {
    @MockitoBean
    LeadAcceptanceTransaction transaction;

    @Autowired
    ApplicationContext context;

    @Autowired
    RetryPolicy retryPolicy;

    @Test
    void testProfileCreatesWorkerPolicyWithoutEnablingScheduling() {
        assertThat(retryPolicy.delay(1, null))
                .isEqualTo(java.time.Duration.ofSeconds(30));
        assertThat(context.containsBean(
                        TaskManagementConfigUtils
                                .SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME))
                .isFalse();
    }

    @Test
    void databasePresenceActivatesTheWorkerForScheduling() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JdbcClient> jdbc = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<TelegramWorker> worker = mock(ObjectProvider.class);
        when(jdbc.getIfAvailable()).thenReturn(mock(JdbcClient.class));

        new TelegramSchedulingConfiguration.SchedulingEnabled(jdbc, worker);

        verify(worker).getObject();
    }

    @Test
    void lazyBeanFactoriesCreateTheCompleteWorkerGraph() {
        var configuration = new TelegramSchedulingConfiguration();
        var jdbc = mock(JdbcClient.class);
        var outbox = mock(OutboxRepository.class);
        var gateway = mock(TelegramGateway.class);
        var registry = new SimpleMeterRegistry();
        var clock = Clock.fixed(
                Instant.parse("2026-01-30T00:00:00Z"), ZoneOffset.UTC);
        var heartbeat = new WorkerHeartbeat(clock);
        var properties = new TelegramWorkerProperties(
                Duration.ofSeconds(15),
                10,
                Duration.ofMinutes(2),
                Duration.ofSeconds(30),
                Duration.ofHours(6));
        var httpClients = new HttpClientsProperties();
        httpClients.setConnectTimeout(Duration.ofSeconds(3));
        httpClients.setReadTimeout(Duration.ofSeconds(10));
        var deliveryWindow = configuration.telegramDeliveryWindow(
                properties, httpClients);
        var metrics = configuration.telegramMetrics(
                registry, outbox, heartbeat, clock);

        assertThat(configuration.jdbcOutboxRepository(jdbc))
                .isInstanceOf(JdbcOutboxRepository.class);
        assertThat(configuration.telegramWorker(
                        outbox,
                        gateway,
                        configuration.telegramRetryPolicy(properties),
                        heartbeat,
                        metrics,
                        properties,
                        deliveryWindow,
                        clock))
                .isInstanceOf(TelegramWorker.class);
    }
}
