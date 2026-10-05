package org.stemmate.store;

import org.stemmate.model.Activity;
import org.stemmate.model.Material;
import org.stemmate.repository.ActivityCriteria;
import org.stemmate.repository.ActivityRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local activity catalogue used by the client when the network is unavailable.
 */
public final class LocalActivityStore implements ActivityRepository {
    private static final java.nio.file.Path STORE_PATH = java.nio.file.Paths.get(
            System.getProperty("user.home"), ".stemmate", "activity-offline.ser");
    private final Map<UUID, Activity> activities = new LinkedHashMap<>();
    private final Set<UUID> availableOffline = ConcurrentHashMap.newKeySet();

    public LocalActivityStore() {
        loadOfflineState();
    }

    public LocalActivityStore(Collection<Activity> cachedActivities) {
        Objects.requireNonNull(cachedActivities, "cachedActivities");
        cachedActivities.forEach(this::cache);
        loadOfflineState();
    }

    /**
     * Adds or replaces an activity in the local catalogue cache.
     */
    public synchronized void cache(Activity activity) {
        Objects.requireNonNull(activity, "activity");
        activities.put(activity.activityId(), activity);
    }

    @Override
    public synchronized List<Activity> findByCriteria(ActivityCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria");
        return activities.values().stream()
                .filter(activity -> matches(activity, criteria))
                .toList();
    }

    @Override
    public synchronized Activity findById(UUID activityId) {
        Objects.requireNonNull(activityId, "activityId");
        return activities.get(activityId);
    }

    @Override
    public synchronized void saveForOffline(Activity activity) {
        Objects.requireNonNull(activity, "activity");
        cache(activity);
        availableOffline.add(activity.activityId());
        FileStoreSupport.write(STORE_PATH, (java.io.Serializable) new java.util.LinkedHashSet<>(availableOffline));
    }

    @Override
    public synchronized void removeFromOffline(UUID activityId) {
        Objects.requireNonNull(activityId, "activityId");
        availableOffline.remove(activityId);
        FileStoreSupport.write(STORE_PATH, (java.io.Serializable) new java.util.LinkedHashSet<>(availableOffline));
    }

    @Override
    public boolean isAvailableOffline(UUID activityId) {
        Objects.requireNonNull(activityId, "activityId");
        return availableOffline.contains(activityId);
    }

    @SuppressWarnings("unchecked")
    private void loadOfflineState() {
        Set<UUID> restored = FileStoreSupport.read(
                STORE_PATH,
                java.util.LinkedHashSet.class,
                new java.util.LinkedHashSet<UUID>());
        availableOffline.clear();
        availableOffline.addAll(restored);
    }

    private static boolean matches(Activity activity, ActivityCriteria criteria) {
        return matchesText(criteria.level(), activity.level())
                && matchesText(criteria.topic(), activity.topic())
                && matchesDuration(criteria.maximumDurationMinutes(), activity.durationMinutes())
                && matchesMaterials(criteria.materialNames(), activity.materials());
    }

    private static boolean matchesText(String criterion, String value) {
        return criterion == null || criterion.isBlank()
                || value.equalsIgnoreCase(criterion.trim());
    }

    private static boolean matchesDuration(Integer maximumDurationMinutes, int durationMinutes) {
        return maximumDurationMinutes == null || durationMinutes <= maximumDurationMinutes;
    }

    private static boolean matchesMaterials(List<String> requestedNames, List<Material> materials) {
        if (requestedNames == null || requestedNames.isEmpty()) {
            return true;
        }
        List<String> availableNames = materials == null
                ? Collections.emptyList()
                : materials.stream().map(Material::name).map(String::trim)
                .map(String::toLowerCase).toList();
        return requestedNames.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .allMatch(availableNames::contains);
    }
}
