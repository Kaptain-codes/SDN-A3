package org.stemmate.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record SyncRecord(
        UUID planId,
        SyncStatus state,
        Instant lastAttempt,
        String errorMessage) implements Serializable {
}
