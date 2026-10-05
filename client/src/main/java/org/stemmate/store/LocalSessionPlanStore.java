package org.stemmate.store;

import org.stemmate.model.SessionPlan;
import org.stemmate.model.SyncRecord;
import org.stemmate.model.SyncStatus;
import org.stemmate.repository.SessionPlanRepository;
import org.stemmate.repository.exception.ValidationException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Local-first plan repository for the prototype.
 *
 * <p>Saving a plan updates the plan and its outbox record as one synchronized
 * operation. The plan ID is the idempotency key, so retries replace rather
 * than duplicate local records.</p>
 */
public final class LocalSessionPlanStore implements SessionPlanRepository {
    private static final java.nio.file.Path STORE_PATH = java.nio.file.Paths.get(
            System.getProperty("user.home"), ".stemmate", "plans.ser");
    private static final java.nio.file.Path DRAFT_PATH = java.nio.file.Paths.get(
            System.getProperty("user.home"), ".stemmate", "drafts.ser");
    private final Map<UUID, SessionPlan> plans = new LinkedHashMap<>();
    private final Map<UUID, SessionPlan> drafts = new LinkedHashMap<>();
    private final LocalOutboxRepository outboxRepository;

    public LocalSessionPlanStore() {
        this(new LocalOutboxRepository());
    }

    public LocalSessionPlanStore(LocalOutboxRepository outboxRepository) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository");
        load();
    }

    @Override
    public synchronized void save(SessionPlan plan) {
        validateSaveInput(plan);
        SessionPlan localPlan = withWaitingStatus(plan);
        plans.put(localPlan.planId(), localPlan);
        outboxRepository.enqueue(new SyncRecord(
                localPlan.planId(),
                SyncStatus.WAITING_TO_SYNC,
                null,
                null));
        savePlans();
    }

    @Override
    public synchronized SessionPlan findById(UUID planId) {
        return plans.get(Objects.requireNonNull(planId, "planId"));
    }

    @Override
    public synchronized List<SessionPlan> findAll() {
        return new ArrayList<>(plans.values());
    }

    @Override
    public synchronized List<SessionPlan> findPending() {
        return outboxRepository.findPending().stream()
                .map(SyncRecord::planId)
                .map(plans::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public synchronized void updateSyncStatus(UUID planId, SyncStatus status) {
        SessionPlan plan = plans.get(Objects.requireNonNull(planId, "planId"));
        if (plan == null) {
            return;
        }
        plans.put(planId, new SessionPlan(
                plan.planId(), plan.activityId(), plan.title(), plan.targetGroup(), plan.steps(), plan.timingMinutes(),
                plan.materials(), plan.safetyNotes(), plan.inclusionPrompts(),
                Objects.requireNonNull(status, "status"), plan.createdAt(), plan.updatedAt()));
        savePlans();
    }

    @Override
    public synchronized void saveDraft(SessionPlan draft) {
        Objects.requireNonNull(draft, "draft");
        drafts.put(draft.planId(), draft);
        saveDrafts();
    }

    @Override
    public synchronized SessionPlan findDraft(UUID planId) {
        return drafts.get(Objects.requireNonNull(planId, "planId"));
    }

    @Override
    public synchronized SessionPlan findLatestDraft() {
        return drafts.values().stream()
                .max(java.util.Comparator.comparing(SessionPlan::updatedAt))
                .orElse(null);
    }

    public LocalOutboxRepository outboxRepository() {
        return outboxRepository;
    }

    private static SessionPlan withWaitingStatus(SessionPlan plan) {
        return new SessionPlan(
                plan.planId(),
                plan.activityId(),
                plan.title(),
                plan.targetGroup(),
                copy(plan.steps()),
                copy(plan.timingMinutes()),
                copy(plan.materials()),
                copy(plan.safetyNotes()),
                copy(plan.inclusionPrompts()),
                SyncStatus.WAITING_TO_SYNC,
                plan.createdAt(),
                plan.updatedAt());
    }

    private static void validateSaveInput(SessionPlan plan) {
        if (plan == null || plan.planId() == null || plan.activityId() == null) {
            throw new ValidationException("A plan and its identifiers are required.");
        }
        if (plan.title() == null || plan.targetGroup() == null) {
            throw new ValidationException("A plan title and target group are required.");
        }
        if (plan.steps() == null || plan.timingMinutes() == null
                || plan.steps().size() != plan.timingMinutes().size()
                || plan.materials() == null || plan.safetyNotes() == null
                || plan.inclusionPrompts() == null
                || plan.createdAt() == null || plan.updatedAt() == null) {
            throw new ValidationException("A complete plan is required.");
        }
    }

    @SuppressWarnings("unchecked")
    private void load() {
        Map<UUID, SessionPlan> restoredPlans = FileStoreSupport.read(
                STORE_PATH, LinkedHashMap.class, new LinkedHashMap<UUID, SessionPlan>());
        Map<UUID, SessionPlan> restoredDrafts = FileStoreSupport.read(
                DRAFT_PATH, LinkedHashMap.class, new LinkedHashMap<UUID, SessionPlan>());
        plans.clear();
        plans.putAll(restoredPlans);
        drafts.clear();
        drafts.putAll(restoredDrafts);
    }

    private void savePlans() {
        FileStoreSupport.write(STORE_PATH, (java.io.Serializable) new LinkedHashMap<>(plans));
    }

    private void saveDrafts() {
        FileStoreSupport.write(DRAFT_PATH, (java.io.Serializable) new LinkedHashMap<>(drafts));
    }

    private static <T> List<T> copy(List<T> values) {
        return List.copyOf(new ArrayList<>(Objects.requireNonNull(values, "values")));
    }
}
