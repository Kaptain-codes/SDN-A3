package org.stemmate.repository;

import org.stemmate.model.SessionPlan;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class LocalRemoteSessionPlanStore implements RemoteSessionPlanStore {
    private final java.util.Set<UUID> uploadedPlans = new LinkedHashSet<>();

    @Override
    public synchronized Set<UUID> uploadBatch(List<SessionPlan> plans) {
        plans.forEach(plan -> uploadedPlans.add(plan.planId()));
        return new LinkedHashSet<>(uploadedPlans);
    }
}
