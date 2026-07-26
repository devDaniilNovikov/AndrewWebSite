package ru.andrew.website.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RetentionServiceTest {
    @Test
    void drainsEveryBatchBeforeAdvancingHeartbeat() {
        Instant now = Instant.parse("2026-01-30T00:00:00Z");
        Instant expireCutoff = now.minus(Duration.ofDays(29));
        Instant deleteCutoff = Instant.parse("2025-01-30T00:00:00Z");
        RetentionRepository repository = mock(RetentionRepository.class);
        var fixture = fixture(repository, now, 2);
        when(repository.expireBatch(expireCutoff, 2))
                .thenReturn(
                        new RetentionBatchResult(2, 2),
                        new RetentionBatchResult(1, 1));
        when(repository.deleteBatch(deleteCutoff, 2))
                .thenReturn(2, 0);

        fixture.service().runOnce();

        verify(repository, times(2)).expireBatch(expireCutoff, 2);
        verify(repository, times(2)).deleteBatch(deleteCutoff, 2);
        assertThat(fixture.heartbeat().lastSuccess()).contains(now);
        assertThat(fixture.registry()
                        .get("andrew.privacy.anonymized")
                        .counter()
                        .count())
                .isEqualTo(3.0);
        assertThat(fixture.registry()
                        .get("andrew.privacy.deleted")
                        .counter()
                        .count())
                .isEqualTo(2.0);
    }

    @ParameterizedTest
    @MethodSource("calendarCutoffs")
    void computesDeletionCutoffInUtcCalendarMonths(
            String nowValue, String expectedCutoffValue) {
        Instant now = Instant.parse(nowValue);
        Instant expectedCutoff = Instant.parse(expectedCutoffValue);
        RetentionRepository repository = mock(RetentionRepository.class);
        var fixture = fixture(repository, now, 10);
        when(repository.expireBatch(now.minus(Duration.ofDays(29)), 10))
                .thenReturn(new RetentionBatchResult(0, 0));
        when(repository.deleteBatch(expectedCutoff, 10)).thenReturn(0);

        fixture.service().runOnce();

        verify(repository).deleteBatch(expectedCutoff, 10);
    }

    static Stream<Arguments> calendarCutoffs() {
        return Stream.of(
                Arguments.of(
                        "2028-02-29T00:00:00Z",
                        "2027-02-28T00:00:00Z"),
                Arguments.of(
                        "2026-03-31T12:30:00Z",
                        "2025-03-31T12:30:00Z"));
    }

    @Test
    void repositoryFailureDoesNotAdvanceHeartbeat() {
        Instant now = Instant.parse("2026-01-30T00:00:00Z");
        RetentionRepository repository = mock(RetentionRepository.class);
        var fixture = fixture(repository, now, 10);
        when(repository.expireBatch(now.minus(Duration.ofDays(29)), 10))
                .thenThrow(new IllegalStateException("test-only failure"));

        assertThatThrownBy(fixture.service()::runOnce)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("test-only failure");

        assertThat(fixture.heartbeat().lastSuccess()).isEmpty();
        assertThat(fixture.registry()
                        .get("andrew.privacy.anonymized")
                        .counter()
                        .count())
                .isZero();
        assertThat(fixture.registry()
                        .get("andrew.privacy.deleted")
                        .counter()
                        .count())
                .isZero();
    }

    private static Fixture fixture(
            RetentionRepository repository, Instant now, int batchSize) {
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        var heartbeat = new RetentionHeartbeat(clock);
        var registry = new SimpleMeterRegistry();
        var metrics = new RetentionMetrics(registry, heartbeat, clock);
        var properties = new RetentionProperties(
                Duration.ofDays(29),
                Duration.ofDays(30),
                Period.ofMonths(12),
                Duration.ofHours(1),
                batchSize);
        var service = new RetentionService(
                repository,
                properties,
                heartbeat,
                metrics,
                clock);
        return new Fixture(service, heartbeat, registry);
    }

    private record Fixture(
            RetentionService service,
            RetentionHeartbeat heartbeat,
            SimpleMeterRegistry registry) {}
}
