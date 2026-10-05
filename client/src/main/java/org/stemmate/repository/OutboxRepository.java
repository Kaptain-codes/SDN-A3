package org.stemmate.repository;

import org.stemmate.model.SyncRecord;
import org.stemmate.model.SyncStatus;

import java.util.List;
import java.util.UUID;

/**
 * Local boundary for plans waiting to be synchronised.
 */
public interface OutboxRepository {
    void enqueue(SyncRecord record);

    SyncRecord findByPlanId(UUID planId);

    List<SyncRecord> findPending();

    void updateStatus(UUID planId, SyncStatus status, java.time.Instant lastAttempt, String errorMessage);
}
