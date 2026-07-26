package ru.andrew.website.telegram;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public final class WorkerHeartbeat {
    private final Instant startedAt;
    private final AtomicReference<Instant> lastSuccess =
            new AtomicReference<>();

    public WorkerHeartbeat(Clock clock) {
        this.startedAt = clock.instant();
    }

    public void success(Instant instant) {
        lastSuccess.set(instant);
    }

    public Optional<Instant> lastSuccess() {
        return Optional.ofNullable(lastSuccess.get());
    }

    public Instant startedAt() {
        return startedAt;
    }
}
