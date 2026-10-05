package org.stemmate.model;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record Activity(
        UUID activityId,
        String title,
        String description,
        String level,
        int durationMinutes,
        String topic,
        List<PlanStep> steps,
        List<Material> materials,
        List<SafetyNote> safetyNotes) implements Serializable {
}
