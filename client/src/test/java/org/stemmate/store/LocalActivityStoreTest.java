package org.stemmate.store;

import org.junit.jupiter.api.Test;
import org.stemmate.model.Activity;
import org.stemmate.model.Material;
import org.stemmate.repository.ActivityCriteria;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalActivityStoreTest {
    @Test
    void maximumDurationIsExclusive() {
        Activity underLimit = activity("Under limit", 59);
        Activity atLimit = activity("At limit", 60);
        LocalActivityStore store = new LocalActivityStore(List.of(underLimit, atLimit));

        List<Activity> results = store.findByCriteria(new ActivityCriteria(null, null, 60, List.of()));

        assertEquals(List.of("Under limit"), results.stream().map(Activity::title).toList());
    }

    private static Activity activity(String title, int durationMinutes) {
        return new Activity(
                UUID.randomUUID(),
                title,
                "",
                "High School",
                durationMinutes,
                "Physics",
                List.of(),
                List.of(new Material("Water", "1")),
                List.of());
    }
}
