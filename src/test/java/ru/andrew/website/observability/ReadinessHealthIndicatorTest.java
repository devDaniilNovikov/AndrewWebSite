package ru.andrew.website.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.jdbc.core.simple.JdbcClient;
import ru.andrew.website.telegram.WorkerHeartbeat;
import ru.andrew.website.testing.MutableClock;

class ReadinessHealthIndicatorTest {
    private static final Instant NOW =
            Instant.parse("2026-07-26T00:00:00Z");

    @Test
    void databaseIsDownWithoutJdbcClientAndExposesNoDetails() {
        ObjectProvider<JdbcClient> provider = provider();

        Health health =
                new DatabaseReadinessHealthIndicator(provider).health();

        assertMinimal(health, Status.DOWN);
    }

    @Test
    void databaseIsUpOnlyForTheExactBoundedValidationResult() {
        JdbcClient jdbc = mock(JdbcClient.class, RETURNS_DEEP_STUBS);
        when(jdbc.sql("select 1").query(Integer.class).single())
                .thenReturn(1);
        clearInvocations(jdbc);

        Health health = new DatabaseReadinessHealthIndicator(provider(jdbc))
                .health();

        assertMinimal(health, Status.UP);
        verify(jdbc).sql("select 1");
    }

    @Test
    void databaseIsDownForAnUnexpectedValidationResult() {
        JdbcClient jdbc = mock(JdbcClient.class, RETURNS_DEEP_STUBS);
        when(jdbc.sql("select 1").query(Integer.class).single())
                .thenReturn(0);

        Health health = new DatabaseReadinessHealthIndicator(provider(jdbc))
                .health();

        assertMinimal(health, Status.DOWN);
    }

    @Test
    void databaseIsDownForANullValidationResult() {
        JdbcClient jdbc = mock(JdbcClient.class, RETURNS_DEEP_STUBS);
        when(jdbc.sql("select 1").query(Integer.class).single())
                .thenReturn(null);

        Health health = new DatabaseReadinessHealthIndicator(provider(jdbc))
                .health();

        assertMinimal(health, Status.DOWN);
    }

    @Test
    void databaseIsDownWithoutLeakingQueryExceptionDetails() {
        JdbcClient jdbc = mock(JdbcClient.class);
        when(jdbc.sql("select 1"))
                .thenThrow(new IllegalStateException("database-secret-detail"));

        Health health = new DatabaseReadinessHealthIndicator(provider(jdbc))
                .health();

        assertMinimal(health, Status.DOWN);
        assertThat(health.toString()).doesNotContain("database-secret-detail");
    }

    @Test
    void databaseIsDownWhenTheOptionalDependencyLookupFails() {
        ObjectProvider<JdbcClient> provider = provider();
        when(provider.getIfAvailable())
                .thenThrow(new IllegalStateException("container-detail"));

        Health health =
                new DatabaseReadinessHealthIndicator(provider).health();

        assertMinimal(health, Status.DOWN);
        assertThat(health.toString()).doesNotContain("container-detail");
    }

    @Test
    void workerStartupGraceIncludesTheExactBoundary() {
        MutableClock clock = clock();
        WorkerHeartbeat heartbeat = new WorkerHeartbeat(clock);
        WorkerReadinessHealthIndicator indicator =
                new WorkerReadinessHealthIndicator(heartbeat, clock);

        clock.advance(Duration.ofSeconds(45));

        assertMinimal(indicator.health(), Status.UP);
    }

    @Test
    void workerStartupGraceExpiresImmediatelyAfterTheBoundary() {
        MutableClock clock = clock();
        WorkerHeartbeat heartbeat = new WorkerHeartbeat(clock);
        WorkerReadinessHealthIndicator indicator =
                new WorkerReadinessHealthIndicator(heartbeat, clock);

        clock.advance(Duration.ofSeconds(45).plusNanos(1));

        assertMinimal(indicator.health(), Status.DOWN);
    }

    @Test
    void successfulWorkerPollIsFreshThroughTheExactBoundary() {
        MutableClock clock = clock();
        WorkerHeartbeat heartbeat = new WorkerHeartbeat(clock);
        heartbeat.success(NOW);
        WorkerReadinessHealthIndicator indicator =
                new WorkerReadinessHealthIndicator(heartbeat, clock);

        clock.advance(Duration.ofSeconds(45));

        assertMinimal(indicator.health(), Status.UP);
    }

    @Test
    void successfulWorkerPollBecomesStaleImmediatelyAfterTheBoundary() {
        MutableClock clock = clock();
        WorkerHeartbeat heartbeat = new WorkerHeartbeat(clock);
        heartbeat.success(NOW);
        WorkerReadinessHealthIndicator indicator =
                new WorkerReadinessHealthIndicator(heartbeat, clock);

        clock.advance(Duration.ofSeconds(45).plusNanos(1));

        assertMinimal(indicator.health(), Status.DOWN);
    }

    @Test
    void futureHeartbeatDuringClockRollbackRemainsFresh() {
        MutableClock clock = clock();
        WorkerHeartbeat heartbeat = new WorkerHeartbeat(clock);
        heartbeat.success(NOW);
        WorkerReadinessHealthIndicator indicator =
                new WorkerReadinessHealthIndicator(heartbeat, clock);

        clock.setInstant(NOW.minus(Duration.ofDays(1)));

        assertMinimal(indicator.health(), Status.UP);
    }

    @Test
    void workerClockFailureProducesDetailFreeDownHealth() {
        MutableClock startupClock = clock();
        WorkerHeartbeat heartbeat = new WorkerHeartbeat(startupClock);
        java.time.Clock failingClock = mock(java.time.Clock.class);
        when(failingClock.instant())
                .thenThrow(new IllegalStateException("clock-detail"));

        Health health = new WorkerReadinessHealthIndicator(
                heartbeat, failingClock).health();

        assertMinimal(health, Status.DOWN);
        assertThat(health.toString()).doesNotContain("clock-detail");
    }

    private static MutableClock clock() {
        return new MutableClock(NOW, ZoneOffset.UTC);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JdbcClient> provider(JdbcClient... jdbc) {
        ObjectProvider<JdbcClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable())
                .thenReturn(jdbc.length == 0 ? null : jdbc[0]);
        return provider;
    }

    private static void assertMinimal(Health health, Status expected) {
        assertThat(health.getStatus()).isEqualTo(expected);
        assertThat(health.getDetails()).isEmpty();
    }
}
