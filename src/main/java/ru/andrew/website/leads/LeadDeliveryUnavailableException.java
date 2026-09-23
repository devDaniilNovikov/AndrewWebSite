package ru.andrew.website.leads;

public final class LeadDeliveryUnavailableException extends RuntimeException {
    public LeadDeliveryUnavailableException() {
        super("Lead delivery unavailable");
    }
}
