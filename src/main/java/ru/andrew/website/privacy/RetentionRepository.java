package ru.andrew.website.privacy;

import java.time.Instant;

public interface RetentionRepository {
    RetentionBatchResult expireBatch(
            Instant cutoffInclusive, int limit);

    int deleteBatch(Instant anonymizedCutoffInclusive, int limit);

    boolean isComplete(
            Instant expireCutoffInclusive,
            Instant deleteCutoffInclusive);
}
