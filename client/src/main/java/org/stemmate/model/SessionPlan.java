package org.stemmate.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionPlan(
        UUID planId,
        UUID activityId,
        String title,
        String targetGroup,
        List<PlanStep> steps,
        List<Integer> timingMinutes,
        List<Material> materials,
        List<SafetyNote> safetyNotes,
        List<InclusionPrompt> inclusionPrompts,
        SyncStatus syncStatus,
        Instant createdAt,
        Instant updatedAt) implements Serializable {
}
