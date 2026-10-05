package org.stemmate.repository;

import org.stemmate.model.Activity;

import java.util.List;
import java.util.UUID;

/**
 * Repository boundary for catalogue reads and explicitly cached activities.
 * Implementations may use the local catalogue while offline.
 */
public interface ActivityRepository {
    List<Activity> findByCriteria(ActivityCriteria criteria);

    Activity findById(UUID activityId);

    void saveForOffline(Activity activity);

    void removeFromOffline(UUID activityId);

    boolean isAvailableOffline(UUID activityId);
}
