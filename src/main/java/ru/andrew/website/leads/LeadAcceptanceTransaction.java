package ru.andrew.website.leads;

public interface LeadAcceptanceTransaction {
    AcceptanceOutcome accept(NormalizedLead lead, LeadFingerprint fingerprint);
}
