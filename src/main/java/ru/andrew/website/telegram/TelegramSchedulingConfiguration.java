package ru.andrew.website.telegram;

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
@Import(TelegramSchedulingConfiguration.SchedulingEnabled.class)
public class TelegramSchedulingConfiguration {
    @Bean
    @Lazy
    JdbcOutboxRepository jdbcOutboxRepository(JdbcClient jdbc) {
        return new JdbcOutboxRepository(jdbc);
    }

    @Bean
    @Lazy
    TelegramMetrics telegramMetrics(
            MeterRegistry registry,
            OutboxRepository outbox,
            WorkerHeartbeat heartbeat,
            Clock clock) {
        return new TelegramMetrics(registry, outbox, heartbeat, clock);
    }

    @Bean
    @Lazy
    TelegramWorker telegramWorker(
            OutboxRepository outbox,
            TelegramGateway gateway,
            RetryPolicy retryPolicy,
            WorkerHeartbeat heartbeat,
            TelegramMetrics metrics,
            TelegramWorkerProperties properties,
            Clock clock) {
        return new TelegramWorker(
                outbox,
                gateway,
                retryPolicy,
                heartbeat,
                metrics,
                properties,
                clock);
    }

    @Bean
    RetryPolicy telegramRetryPolicy(TelegramWorkerProperties properties) {
        return new RetryPolicy(
                properties.retryInitial(),
                properties.retryMaximum());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    @Profile("!test")
    static class SchedulingEnabled {
        SchedulingEnabled(
                ObjectProvider<JdbcClient> jdbc,
                ObjectProvider<TelegramWorker> worker) {
            if (jdbc.getIfAvailable() != null) {
                worker.getObject();
            }
        }
    }
}
