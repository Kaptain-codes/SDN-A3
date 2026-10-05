package org.stemmate;

import org.stemmate.connectivity.ConnectivityMonitor;
import org.stemmate.model.Activity;
import org.stemmate.model.EquipmentKitRecord;
import org.stemmate.model.PlanStep;
import org.stemmate.model.SessionPlan;
import org.stemmate.model.SyncStatus;
import org.stemmate.repository.ActivityCriteria;
import org.stemmate.service.ActivityCatalogueService;
import org.stemmate.service.EquipmentKitService;
import org.stemmate.service.SessionPlanService;
import org.stemmate.sync.SyncManager;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DesktopController {
    public enum Role {
        FACILITATOR,
        CUSTODIAN
    }

    private final ActivityCatalogueService activityService;
    private final SessionPlanService planService;
    private final EquipmentKitService kitService;
    private final ConnectivityMonitor connectivityMonitor;
    private final SyncManager syncManager;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private volatile Role role = Role.FACILITATOR;
    private volatile SyncStatus lastSyncStatus;
    private volatile SessionPlan currentDraft;

    public DesktopController(
            ActivityCatalogueService activityService,
            SessionPlanService planService,
            EquipmentKitService kitService,
            ConnectivityMonitor connectivityMonitor,
            SyncManager syncManager) {
        this.activityService = Objects.requireNonNull(activityService, "activityService");
        this.planService = Objects.requireNonNull(planService, "planService");
        this.kitService = Objects.requireNonNull(kitService, "kitService");
        this.connectivityMonitor = Objects.requireNonNull(connectivityMonitor, "connectivityMonitor");
        this.syncManager = Objects.requireNonNull(syncManager, "syncManager");
        this.syncManager.setStatusListener(status -> {
            lastSyncStatus = status;
            notifyChange();
        });
        lastSyncStatus = pendingCount() > 0 ? SyncStatus.WAITING_TO_SYNC : SyncStatus.SYNCED;
        currentDraft = planService.restoreLatestDraft();
        if (currentDraft == null) {
            currentDraft = createTemplateDraft();
        }
    }

    public void addChangeListener(Runnable listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = Objects.requireNonNull(role, "role");
        notifyChange();
    }

    public boolean isOnline() {
        return connectivityMonitor.isOnline();
    }

    public void setOnline(boolean online) {
        connectivityMonitor.setOnline(online);
        notifyChange();
    }

    public int pendingCount() {
        return planService.findPendingPlans().size();
    }

    public SyncStatus getLastSyncStatus() {
        return lastSyncStatus;
    }

    public List<Activity> findActivities(ActivityCriteria criteria) {
        return activityService.findActivities(criteria);
    }

    public List<Activity> findSavedActivities(ActivityCriteria criteria) {
        return activityService.findOfflineActivities(criteria);
    }

    public boolean isSavedOffline(UUID activityId) {
        return activityService.isAvailableOffline(activityId);
    }

    public void toggleOffline(Activity activity) {
        activityService.toggleSaveOffline(activity.activityId());
        notifyChange();
    }

    public SessionPlan getCurrentDraft() {
        if (currentDraft == null) {
            currentDraft = createTemplateDraft();
        }
        return currentDraft;
    }

    public SessionPlan createDraftFromActivity(Activity activity) {
        SessionPlan draft = planService.createDraftFromActivity(UUID.randomUUID(), activity, Instant.now());
        currentDraft = draft;
        notifyChange();
        return draft;
    }

    public SessionPlan autosaveDraft(SessionPlan draft) {
        currentDraft = Objects.requireNonNull(draft, "draft");
        planService.autosaveDraft(draft);
        notifyChange();
        return draft;
    }

    public SessionPlan finalizePlan(SessionPlan plan) {
        planService.savePlan(Objects.requireNonNull(plan, "plan"));
        currentDraft = plan;
        notifyChange();
        return plan;
    }

    public SessionPlan loadPlanAsDraft(UUID planId) {
        SessionPlan saved = planService.findPlan(Objects.requireNonNull(planId, "planId"));
        if (saved == null) {
            return null;
        }
        currentDraft = saved;
        planService.autosaveDraft(saved);
        notifyChange();
        return saved;
    }

    public SessionPlan duplicatePlan(UUID planId) {
        SessionPlan existing = planService.findPlan(Objects.requireNonNull(planId, "planId"));
        if (existing == null) {
            existing = planService.restoreDraft(planId);
        }
        if (existing == null) {
            return null;
        }
        SessionPlan duplicate = planService.duplicatePlan(existing, UUID.randomUUID(), Instant.now());
        currentDraft = duplicate;
        planService.autosaveDraft(duplicate);
        notifyChange();
        return duplicate;
    }

    public List<SessionPlan> savedPlans() {
        return planService.findAllPlans().stream()
                .sorted((left, right) -> right.updatedAt().compareTo(left.updatedAt()))
                .toList();
    }

    public List<EquipmentKitRecord> kitRecords() {
        return kitService.findAll().stream()
                .sorted((left, right) -> right.intendedDate().compareTo(left.intendedDate()))
                .toList();
    }

    public void saveKit(UUID kitId, String kitName, String responsiblePerson, java.time.LocalDate intendedDate, String status) {
        kitService.saveRecord(kitId, kitName, responsiblePerson, intendedDate, status);
        notifyChange();
    }

    public void deleteKit(UUID kitId) {
        kitService.deleteRecord(kitId);
        notifyChange();
    }

    public void syncNow() {
        syncManager.syncPending();
        notifyChange();
    }

    public SessionPlan createTemplateDraft() {
        Activity firstActivity = findActivities(new ActivityCriteria(null, null, null, List.of()))
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No activities available."));
        return planService.createDraftFromActivity(UUID.randomUUID(), firstActivity, Instant.now());
    }

    private void notifyChange() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}
