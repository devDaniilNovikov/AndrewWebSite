package ru.andrew.website.leads;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Remembers which request identifiers were already delivered so a retried submission is
 * answered without a second Telegram message. Only the HMAC fingerprint is kept, never
 * the lead itself, and the memory is bounded by age and by entry count.
 */
@Component
public final class LeadIdempotencyRegistry {
    static final Duration RETENTION = Duration.ofHours(1);
    static final int CAPACITY = 10_000;

    private final Map<UUID, Entry> entries = new LinkedHashMap<>();
    private final Clock clock;

    public LeadIdempotencyRegistry(Clock clock) {
        this.clock = clock;
    }

    public synchronized Optional<LeadFingerprint> find(UUID requestId) {
        Entry entry = entries.get(requestId);
        if (entry == null || entry.isExpiredAt(clock.instant())) {
            return Optional.empty();
        }
        return Optional.of(entry.fingerprint());
    }

    public synchronized void remember(UUID requestId, LeadFingerprint fingerprint) {
        Instant now = clock.instant();
        entries.values().removeIf(entry -> entry.isExpiredAt(now));
        entries.remove(requestId);
        if (entries.size() >= CAPACITY) {
            Iterator<UUID> oldest = entries.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
        entries.put(requestId, new Entry(fingerprint, now.plus(RETENTION)));
    }

    synchronized int size() {
        return entries.size();
    }

    private record Entry(LeadFingerprint fingerprint, Instant expiresAt) {
        private boolean isExpiredAt(Instant now) {
            return !now.isBefore(expiresAt);
        }
    }
}
