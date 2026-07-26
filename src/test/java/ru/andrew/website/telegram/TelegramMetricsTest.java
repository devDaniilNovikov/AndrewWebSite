package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TelegramMetricsTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:30Z");

    @Test
    void deliveryTagsUseOnlyFixedOutcomesAndReasons() {
        Fixture fixture = new Fixture();
        Set<String> reasons = Set.of(
                "success",
                "network",
                "telegram_429",
                "telegram_4xx",
                "telegram_5xx",
                "telegram_unexpected",
                "lease_expired",
                "privacy_expired");

        fixture.metrics.delivery("delivered", "success");
        reasons.stream()
                .filter(reason -> !"success".equals(reason))
                .forEach(reason -> fixture.metrics.delivery(
                        "privacy_expired".equals(reason) ? "blocked" : "retry",
                        reason));

        assertThat(fixture.registry.find("andrew.telegram.delivery").meters())
                .hasSize(8)
                .allSatisfy(meter -> assertThat(tagKeys(meter))
                        .containsExactlyInAnyOrder("outcome", "reason"));
    }

    @Test
    void unsupportedOrSensitiveTagsFailBeforeMeterCreation() {
        Fixture fixture = new Fixture();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> fixture.metrics.delivery(
                        "retry", "fictional-request-id"))
                .withMessage("Unsupported Telegram metric tag");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> fixture.metrics.delivery(
                        "fictional-outcome", "network"))
                .withMessage("Unsupported Telegram metric tag");

        assertThat(fixture.registry.find("andrew.telegram.delivery").meters())
                .isEmpty();
    }

    @Test
    void queueDepthPublishesExactlyFiveStateTagsAndWorkerAgeHasNoTags() {
        Fixture fixture = new Fixture();
        Arrays.stream(OutboxState.values())
                .forEach(state -> when(fixture.outbox.countByState(state))
                        .thenReturn((long) state.ordinal()));

        Set<String> states = fixture.registry
                .find("andrew.telegram.queue.depth")
                .gauges()
                .stream()
                .map(gauge -> gauge.getId().getTag("state"))
                .collect(Collectors.toSet());

        assertThat(states).containsExactlyInAnyOrder(
                "pending", "processing", "retry", "blocked", "delivered");
        assertThat(fixture.registry.find("andrew.telegram.queue.depth").gauges())
                .hasSize(5)
                .allSatisfy(gauge -> {
                    OutboxState state =
                            OutboxState.valueOf(gauge.getId().getTag("state"));
                    assertThat(gauge.value()).isEqualTo(state.ordinal());
                    assertThat(tagKeys(gauge)).containsExactly("state");
                });
        assertThat(fixture.registry
                        .find("andrew.telegram.worker.last_success.age")
                        .gauge()
                        .getId()
                        .getTags())
                .isEmpty();
        assertThat(fixture.registry
                        .find("andrew.telegram.worker.last_success.age")
                        .gauge()
                        .value())
                .isEqualTo(30.0);
    }

    @Test
    void workerAgeUsesLastSuccessAfterHeartbeatAdvances() {
        Fixture fixture = new Fixture();
        fixture.heartbeat.success(NOW.minusSeconds(5));

        assertThat(fixture.registry
                        .find("andrew.telegram.worker.last_success.age")
                        .gauge()
                        .value())
                .isEqualTo(5.0);
    }

    private static Set<String> tagKeys(Meter meter) {
        return meter.getId().getTags().stream()
                .map(tag -> tag.getKey())
                .collect(Collectors.toSet());
    }

    private static final class Fixture {
        private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
        private final OutboxRepository outbox =
                Mockito.mock(OutboxRepository.class);
        private final WorkerHeartbeat heartbeat = new WorkerHeartbeat(
                Clock.fixed(NOW.minusSeconds(30), ZoneOffset.UTC));
        private final TelegramMetrics metrics = new TelegramMetrics(
                registry,
                outbox,
                heartbeat,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
