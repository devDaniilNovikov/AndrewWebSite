package ru.andrew.website.leads;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class LeadMetricsTest {
    @Test
    void everyAcceptedOutcomeUsesItsCanonicalBoundedTag() {
        var registry = new SimpleMeterRegistry();
        var metrics = new LeadMetrics(registry);

        for (AcceptanceOutcome outcome : AcceptanceOutcome.values()) {
            metrics.accepted(outcome);
        }

        assertThat(registry.find("andrew.leads.accepted").meters())
                .hasSize(4)
                .allSatisfy(meter -> {
                    assertThat(meter.getId().getTags()).hasSize(1);
                    assertThat(meter.getId().getTag("outcome"))
                            .isIn("created", "duplicate", "retained", "honeypot");
                });
    }

    @Test
    void everyRejectedReasonUsesItsCanonicalBoundedTag() {
        var registry = new SimpleMeterRegistry();
        var metrics = new LeadMetrics(registry);

        for (LeadRejectionReason reason : LeadRejectionReason.values()) {
            metrics.rejected(reason);
        }

        assertThat(registry.find("andrew.leads.rejected").meters())
                .hasSize(6)
                .allSatisfy(meter -> {
                    assertThat(meter.getId().getTags()).hasSize(1);
                    assertThat(meter.getId().getTag("reason"))
                            .isIn(
                                    "validation",
                                    "conflict",
                                    "payload",
                                    "media_type",
                                    "rate_limit",
                                    "unavailable");
                });
    }
}
