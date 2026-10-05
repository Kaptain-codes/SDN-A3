package org.stemmate.repository;

import org.stemmate.model.SessionPlan;
import org.stemmate.model.SyncStatus;

import java.util.List;
import java.util.UUID;

/**
 * Repository boundary for local-first plans, drafts, and pending synchronisation.
 */
public interface SessionPlanRepository {
    void save(SessionPlan plan);

    SessionPlan findById(UUID planId);

    List<SessionPlan> findAll();

    List<SessionPlan> findPending();

    void updateSyncStatus(UUID planId, SyncStatus status);

    void saveDraft(SessionPlan draft);

    SessionPlan findDraft(UUID planId);

    SessionPlan findLatestDraft();
}
