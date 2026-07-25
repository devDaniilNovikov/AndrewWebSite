package ru.andrew.website.telegram;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TelegramLeadMessage(
        long leadId,
        UUID requestId,
        String name,
        String phone,
        String comment,
        String sourcePath,
        String intent,
        Instant createdAt) {

    public TelegramLeadMessage {
        if (leadId <= 0) {
            throw new IllegalArgumentException("leadId must be positive");
        }
        Objects.requireNonNull(requestId, "requestId must not be null");
        requireText(name, "name");
        requireText(phone, "phone");
        requireText(sourcePath, "sourcePath");
        requireText(intent, "intent");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    @Override
    public String toString() {
        return "TelegramLeadMessage[leadId=" + leadId + ", content=<redacted>]";
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
