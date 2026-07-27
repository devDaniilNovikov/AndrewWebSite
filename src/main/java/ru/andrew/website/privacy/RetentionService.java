package ru.andrew.website.privacy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.springframework.scheduling.annotation.Scheduled;

public final class RetentionService {
    private final RetentionRepository repository;
    private final RetentionProperties properties;
    private final RetentionHeartbeat heartbeat;
    private final RetentionMetrics metrics;
    private final Clock clock;

    public RetentionService(
            RetentionRepository repository,
            RetentionProperties properties,
            RetentionHeartbeat heartbeat,
            RetentionMetrics metrics,
            Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.heartbeat = heartbeat;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString =
                    "${app.privacy.retention.poll-interval:1h}")
    public void runOnce() {
        Instant now = clock.instant();
        Instant expireCutoff =
                now.minus(properties.anonymizeAfter());
        RetentionBatchResult expired;
        do {
            expired = repository.expireBatch(
                    expireCutoff,
                    properties.batchSize());
            metrics.anonymized(expired.anonymized());
        } while (expired.anonymized() == properties.batchSize());

        Instant deleteCutoff = deletionCutoff(now);
        int deleted;
        do {
            deleted = repository.deleteBatch(
                    deleteCutoff,
                    properties.batchSize());
            metrics.deleted(deleted);
        } while (deleted == properties.batchSize());
        if (repository.isComplete(
                expireCutoff, deleteCutoff)) {
            heartbeat.success(now);
        }
    }

    private Instant deletionCutoff(Instant now) {
        return ZonedDateTime.ofInstant(now, ZoneOffset.UTC)
                .minus(properties.deleteAfter())
                .toInstant();
    }
}
