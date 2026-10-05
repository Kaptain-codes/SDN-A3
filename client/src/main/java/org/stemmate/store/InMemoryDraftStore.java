package org.stemmate.store;

import org.stemmate.model.SessionPlan;
import org.stemmate.model.SyncStatus;
import org.stemmate.repository.SessionPlanRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Draft-capable repository for the desktop prototype.
 *
 * <p>The draft map represents local storage for the current process. Durable
 * SQLite persistence and outbox handling remain separate concerns.</p>
 */
public final class InMemoryDraftStore implements SessionPlanRepository {
    private final Map<UUID, SessionPlan> plans = new LinkedHashMap<>();
    private final Map<UUID, SessionPlan> drafts = new LinkedHashMap<>();

    @Override
    public synchronized void save(SessionPlan plan) {
        plans.put(plan.planId(), plan);
    }

    @Override
    public synchronized SessionPlan findById(UUID planId) {
        return plans.get(planId);
    }

    @Override
    public synchronized List<SessionPlan> findPending() {
        return new ArrayList<>();
    }

    @Override
    public synchronized List<SessionPlan> findAll() {
        return new ArrayList<>(plans.values());
    }

    @Override
    public synchronized void updateSyncStatus(UUID planId, SyncStatus status) {
    }

    @Override
    public synchronized void saveDraft(SessionPlan draft) {
        drafts.put(draft.planId(), draft);
    }

    @Override
    public synchronized SessionPlan findDraft(UUID planId) {
        return drafts.get(planId);
    }

    @Override
    public synchronized SessionPlan findLatestDraft() {
        return drafts.values().stream()
                .max(java.util.Comparator.comparing(SessionPlan::updatedAt))
                .orElse(null);
    }
}
