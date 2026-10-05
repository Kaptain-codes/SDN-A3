package org.stemmate.service;

import org.stemmate.model.Activity;
import org.stemmate.repository.ActivityCriteria;
import org.stemmate.repository.ActivityRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Application service for browsing and caching activities without network access.
 */
public final class ActivityCatalogueService {
    private final ActivityRepository activityRepository;

    public ActivityCatalogueService(ActivityRepository activityRepository) {
        this.activityRepository = Objects.requireNonNull(activityRepository, "activityRepository");
    }

    public List<Activity> findActivities(ActivityCriteria criteria) {
        return activityRepository.findByCriteria(Objects.requireNonNull(criteria, "criteria"));
    }

    public Activity findActivity(UUID activityId) {
        return activityRepository.findById(Objects.requireNonNull(activityId, "activityId"));
    }

    public void saveForOffline(Activity activity) {
        activityRepository.saveForOffline(Objects.requireNonNull(activity, "activity"));
    }

    public void toggleSaveOffline(UUID activityId) {
        Activity activity = findActivity(Objects.requireNonNull(activityId, "activityId"));
        if (activity == null) {
            return;
        }
        if (!activityRepository.isAvailableOffline(activityId)) {
            activityRepository.saveForOffline(activity);
        } else {
            activityRepository.removeFromOffline(activityId);
        }
    }

    public boolean isAvailableOffline(UUID activityId) {
        return activityRepository.isAvailableOffline(Objects.requireNonNull(activityId, "activityId"));
    }

    public List<Activity> findOfflineActivities(ActivityCriteria criteria) {
        return findActivities(criteria).stream()
                .filter(activity -> activityRepository.isAvailableOffline(activity.activityId()))
                .toList();
    }
}
