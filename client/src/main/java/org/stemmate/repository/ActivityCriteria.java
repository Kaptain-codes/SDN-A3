package org.stemmate.repository;

import java.util.List;

public record ActivityCriteria(
        String level,
        String topic,
        Integer maximumDurationMinutes,
        List<String> materialNames) {
}
