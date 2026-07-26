package ru.andrew.website.leads;

public enum LeadRejectionReason {
    VALIDATION("validation"),
    CONFLICT("conflict"),
    PAYLOAD("payload"),
    MEDIA_TYPE("media_type"),
    RATE_LIMIT("rate_limit"),
    UNAVAILABLE("unavailable");

    private final String metricValue;

    LeadRejectionReason(String metricValue) {
        this.metricValue = metricValue;
    }

    public String metricValue() {
        return metricValue;
    }
}
