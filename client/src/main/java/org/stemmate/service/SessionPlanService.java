package org.stemmate.service;

import org.stemmate.model.Activity;
import org.stemmate.model.InclusionPrompt;
import org.stemmate.model.PlanStep;
import org.stemmate.model.SessionPlan;
import org.stemmate.model.SyncStatus;
import org.stemmate.repository.SessionPlanRepository;
import org.stemmate.repository.exception.ValidationException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Creates and validates session plans from selected activities.
 */
public final class SessionPlanService {
    private final SessionPlanRepository sessionPlanRepository;

    public SessionPlanService(SessionPlanRepository sessionPlanRepository) {
        this.sessionPlanRepository = Objects.requireNonNull(
                sessionPlanRepository, "sessionPlanRepository");
    }

    public SessionPlan createDraftFromActivity(
            UUID planId,
            Activity activity,
            Instant now) {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(activity, "activity");
        Objects.requireNonNull(now, "now");
        return new SessionPlan(
                planId,
                activity.activityId(),
                "",
                "",
                copy(activity.steps()),
                activity.steps().stream().map(PlanStep::durationMinutes).toList(),
                copy(activity.materials()),
                copy(activity.safetyNotes()),
                List.of(),
                SyncStatus.WAITING_TO_SYNC,
                now,
                now);
    }

    public void savePlan(SessionPlan plan) {
        validate(plan);
        sessionPlanRepository.save(plan);
    }

    public void autosaveDraft(SessionPlan draft) {
        validateDraft(draft);
        sessionPlanRepository.saveDraft(draft);
    }

    public SessionPlan restoreDraft(UUID planId) {
        Objects.requireNonNull(planId, "planId");
        return sessionPlanRepository.findDraft(planId);
    }

    public SessionPlan restoreLatestDraft() {
        return sessionPlanRepository.findLatestDraft();
    }

    public List<SessionPlan> findAllPlans() {
        return sessionPlanRepository.findAll();
    }

    public List<SessionPlan> findPendingPlans() {
        return sessionPlanRepository.findPending();
    }

    public SessionPlan findPlan(UUID planId) {
        return sessionPlanRepository.findById(Objects.requireNonNull(planId, "planId"));
    }

    public SessionPlan duplicatePlan(SessionPlan original, UUID newPlanId, Instant now) {
        Objects.requireNonNull(original, "original");
        Objects.requireNonNull(newPlanId, "newPlanId");
        Objects.requireNonNull(now, "now");
        return new SessionPlan(
                newPlanId,
                original.activityId(),
                original.title(),
                original.targetGroup(),
                copy(original.steps()),
                List.copyOf(original.timingMinutes()),
                copy(original.materials()),
                copy(original.safetyNotes()),
                copy(original.inclusionPrompts()),
                SyncStatus.WAITING_TO_SYNC,
                now,
                now);
    }

    public void validate(SessionPlan plan) {
        if (plan == null) {
            throw new ValidationException("A session plan is required.");
        }
        if (plan.planId() == null) {
            throw new ValidationException("A plan ID is required.");
        }
        if (plan.activityId() == null) {
            throw new ValidationException("An activity is required.");
        }
        if (plan.title() == null || plan.title().isBlank()) {
            throw new ValidationException("A plan title is required.");
        }
        if (plan.targetGroup() == null || plan.targetGroup().isBlank()) {
            throw new ValidationException("A target group is required.");
        }
        if (plan.steps() == null || plan.steps().isEmpty()) {
            throw new ValidationException("At least one activity step is required.");
        }
        if (plan.timingMinutes() == null
                || plan.timingMinutes().size() != plan.steps().size()
                || plan.timingMinutes().stream().anyMatch(Objects::isNull)
                || plan.timingMinutes().stream().anyMatch(minutes -> minutes < 0)) {
            throw new ValidationException("Each step must have valid timing.");
        }
        if (plan.materials() == null || plan.materials().isEmpty()) {
            throw new ValidationException("At least one material is required.");
        }
        if (plan.safetyNotes() == null || plan.safetyNotes().isEmpty()) {
            throw new ValidationException("At least one safety note is required.");
        }
        if (plan.inclusionPrompts() == null || plan.inclusionPrompts().isEmpty()) {
            throw new ValidationException("At least one inclusion prompt is required.");
        }
        if (plan.createdAt() == null || plan.updatedAt() == null) {
            throw new ValidationException("Plan timestamps are required.");
        }
    }

    private static void validateDraft(SessionPlan draft) {
        if (draft == null) {
            throw new ValidationException("A draft is required.");
        }
        if (draft.planId() == null) {
            throw new ValidationException("A draft plan ID is required.");
        }
        if (draft.activityId() == null) {
            throw new ValidationException("A draft activity is required.");
        }
        if (draft.steps() == null || draft.timingMinutes() == null
                || draft.steps().size() != draft.timingMinutes().size()
                || draft.createdAt() == null || draft.updatedAt() == null) {
            throw new ValidationException("A draft must preserve its activity fields.");
        }
    }

    private static <T> List<T> copy(List<T> values) {
        return List.copyOf(new ArrayList<>(Objects.requireNonNull(values, "values")));
    }
}
