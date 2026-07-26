package ru.andrew.website.leads;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public final class LeadMetrics {
    private static final String ACCEPTED_METER =
            "andrew.leads.accepted";
    private static final String REJECTED_METER =
            "andrew.leads.rejected";

    private final MeterRegistry registry;

    public LeadMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void accepted(AcceptanceOutcome outcome) {
        Counter.builder(ACCEPTED_METER)
                .tag("outcome", acceptedValue(outcome))
                .register(registry)
                .increment();
    }

    public void rejected(LeadRejectionReason reason) {
        Counter.builder(REJECTED_METER)
                .tag("reason", reason.metricValue())
                .register(registry)
                .increment();
    }

    private static String acceptedValue(AcceptanceOutcome outcome) {
        return switch (outcome) {
            case CREATED -> "created";
            case DUPLICATE -> "duplicate";
            case RETAINED -> "retained";
            case HONEYPOT -> "honeypot";
        };
    }
}
