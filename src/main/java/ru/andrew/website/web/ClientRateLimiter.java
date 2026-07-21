package ru.andrew.website.web;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ClientRateLimiter {
    private static final int DEFAULT_MAX_CLIENTS = 10_000;
    private static final Duration DEFAULT_IDLE_TTL = Duration.ofHours(1);
    private static final int DEFAULT_CLIENT_CAPACITY = 5;
    private static final Duration DEFAULT_CLIENT_REFILL = Duration.ofMinutes(1);

    private final Clock clock;
    private final SlidingWindowRateLimiter global;
    private final int maxClients;
    private final long idleTtlMillis;
    private final int clientCapacity;
    private final Duration clientRefill;
    private final Map<String, ClientBucket> clients =
            new LinkedHashMap<>(128, 0.75f, true);
    private long lastObservedMillis;

    ClientRateLimiter(Clock clock, SlidingWindowRateLimiter global) {
        this(
                clock,
                global,
                DEFAULT_MAX_CLIENTS,
                DEFAULT_IDLE_TTL,
                DEFAULT_CLIENT_CAPACITY,
                DEFAULT_CLIENT_REFILL);
    }

    ClientRateLimiter(
            Clock clock,
            SlidingWindowRateLimiter global,
            int maxClients,
            Duration idleTtl,
            int clientCapacity,
            Duration clientRefill) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.global = Objects.requireNonNull(global, "global");
        Objects.requireNonNull(idleTtl, "idleTtl");
        this.clientRefill = Objects.requireNonNull(clientRefill, "clientRefill");
        if (maxClients < 1
                || clientCapacity < 1
                || idleTtl.isZero()
                || idleTtl.isNegative()
                || idleTtl.toMillis() < 1
                || clientRefill.isZero()
                || clientRefill.isNegative()
                || clientRefill.toMillis() < 1) {
            throw new IllegalArgumentException("rate-limit bounds must be positive");
        }
        this.maxClients = maxClients;
        this.idleTtlMillis = idleTtl.toMillis();
        this.clientCapacity = clientCapacity;
        this.lastObservedMillis = clock.millis();
    }

    public static ClientRateLimiter defaults(Clock clock) {
        return new ClientRateLimiter(
                clock,
                new SlidingWindowRateLimiter(60, Duration.ofMinutes(1), clock));
    }

    public synchronized RateDecision tryAcquire(String connectionAddress) {
        Objects.requireNonNull(connectionAddress, "connectionAddress");
        long now = monotonicNow();
        evictIdleClients(now);

        ClientBucket client = clients.get(connectionAddress);
        if (client == null) {
            client = new ClientBucket(
                    new TokenBucket(clientCapacity, clientRefill, clock, now), now);
        } else {
            client = new ClientBucket(client.bucket(), now);
        }
        clients.put(connectionAddress, client);
        enforceHardCap();

        if (!client.bucket().tryAcquire()) {
            return RateDecision.rejected(client.bucket().retryAfter());
        }
        if (!global.tryAcquire()) {
            return RateDecision.rejected(global.retryAfter());
        }
        return RateDecision.allowedDecision();
    }

    private long monotonicNow() {
        lastObservedMillis = Math.max(lastObservedMillis, clock.millis());
        return lastObservedMillis;
    }

    private void evictIdleClients(long now) {
        var iterator = clients.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ClientBucket> oldest = iterator.next();
            if (now - oldest.getValue().lastSeenMillis() < idleTtlMillis) {
                return;
            }
            iterator.remove();
        }
    }

    private void enforceHardCap() {
        while (clients.size() > maxClients) {
            clients.remove(clients.keySet().iterator().next());
        }
    }

    private record ClientBucket(TokenBucket bucket, long lastSeenMillis) {}
}
