package ru.andrew.website.leads;

public interface LeadDelivery {
    AcceptanceOutcome accept(NormalizedLead lead, LeadFingerprint fingerprint);
}
