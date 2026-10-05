package org.stemmate.repository;

import org.stemmate.model.SessionPlan;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Remote boundary for uploading a small batch of locally saved plans.
 */
public interface RemoteSessionPlanStore {
    Set<UUID> uploadBatch(List<SessionPlan> plans);
}
