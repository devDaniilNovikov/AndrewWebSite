package ru.andrew.website.leads;

import java.time.Instant;
import java.util.UUID;

public record NormalizedLead(
        UUID requestId,
        String name,
        String phoneDigits,
        String comment,
        String sourcePath,
        LeadIntent intent,
        Instant consentedAt) {
}
