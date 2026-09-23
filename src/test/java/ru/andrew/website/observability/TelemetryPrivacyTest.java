package ru.andrew.website.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.andrew.website.leads.AcceptanceOutcome;
import ru.andrew.website.leads.LeadDelivery;
import ru.andrew.website.leads.LeadMetrics;
import ru.andrew.website.leads.LeadRejectionReason;

@SpringBootTest
@ActiveProfiles("test")
class TelemetryPrivacyTest {
    private static final String APPLICATION = "andrew-website";
    private static final String PROFILE = "test";
    private static final String CLIENT_ROUTE =
            "/bot{token}/sendMessage";
    private static final Map<String, Map<String, Set<String>>> CONTRACTS =
            Map.of(
                    "andrew.leads.accepted",
                    Map.of("outcome", Set.of(
                            "created", "duplicate", "honeypot")),
                    "andrew.leads.rejected",
                    Map.of("reason", Set.of(
                            "validation", "conflict", "payload",
                            "media_type", "rate_limit", "unavailable")),
                    "andrew.telegram.client",
                    Map.of(
                            "method", Set.of("POST"),
                            "uri", Set.of(CLIENT_ROUTE),
                            "outcome", Set.of(
                                    "delivered", "retryable",
                                    "permanent_failure")));

    @Autowired
    MeterRegistry registry;

    @Autowired
    ObservationRegistry observations;

    @MockitoBean
    LeadDelivery delivery;

    @BeforeEach
    void clearMeters() {
        registry.clear();
    }

    @Test
    void centralFilterAllowsOnlyCanonicalMetersAndExactTags() {
        var leadMetrics = new LeadMetrics(registry);
        for (AcceptanceOutcome outcome : AcceptanceOutcome.values()) {
            leadMetrics.accepted(outcome);
        }
        for (LeadRejectionReason reason : LeadRejectionReason.values()) {
            leadMetrics.rejected(reason);
        }

        Observation observation = Observation.createNotStarted(
                        "andrew.telegram.client", observations)
                .lowCardinalityKeyValue("method", "POST")
                .lowCardinalityKeyValue("uri", CLIENT_ROUTE)
                .highCardinalityKeyValue("http.url", CLIENT_ROUTE)
                .start();
        observation.lowCardinalityKeyValue("outcome", "delivered");
        observation.stop();

        assertThat(registry.getMeters().stream()
                        .map(meter -> meter.getId().getName())
                        .collect(java.util.stream.Collectors.toSet()))
                .containsExactlyInAnyOrderElementsOf(CONTRACTS.keySet());
        assertThat(registry.getMeters())
                .allSatisfy(TelemetryPrivacyTest::assertCanonicalMeter);
    }

    @Test
    void centralFilterRejectsUnknownNamesTagsAndValues() {
        Counter.builder("jvm.memory.used")
                .tag("area", "heap")
                .register(registry)
                .increment();
        Counter.builder("andrew.leads.accepted")
                .tag("outcome", "invented")
                .register(registry)
                .increment();
        Counter.builder("andrew.leads.rejected")
                .tag("reason", "validation")
                .tag("requestId", "11111111-1111-4111-8111-111111111111")
                .register(registry)
                .increment();
        Counter.builder("andrew.leads.rejected")
                .tag("reason", "validation")
                .tag("profile", "prod")
                .register(registry)
                .increment();
        Counter.builder("andrew.leads.accepted")
                .tag("outcome", "created")
                .tag("application", "attacker-controlled")
                .register(registry)
                .increment();
        Counter.builder("andrew.telegram.client")
                .tag("method", "POST")
                .tag("uri", CLIENT_ROUTE)
                .tag("outcome", "delivered")
                .tag("error", "SensitiveException")
                .register(registry)
                .increment();

        assertThat(registry.getMeters()).isEmpty();
    }

    private static void assertCanonicalMeter(Meter meter) {
        String name = meter.getId().getName();
        Map<String, Set<String>> business = CONTRACTS.get(name);
        assertThat(business).isNotNull();
        assertThat(meter.getId().getTag("application"))
                .isEqualTo(APPLICATION);
        assertThat(meter.getId().getTag("profile"))
                .isEqualTo(PROFILE);
        assertThat(meter.getId().getTags())
                .extracting(tag -> tag.getKey())
                .containsExactlyInAnyOrderElementsOf(
                        union(business.keySet(), Set.of(
                                "application", "profile")));
        for (var entry : business.entrySet()) {
            assertThat(meter.getId().getTag(entry.getKey()))
                    .isIn((Object[]) entry.getValue()
                            .toArray(String[]::new));
        }
        assertThat(meter.getId().getTag("http.url")).isNull();
    }

    private static Set<String> union(
            Set<String> first, Set<String> second) {
        var union = new java.util.HashSet<>(first);
        union.addAll(second);
        return Set.copyOf(union);
    }
}
