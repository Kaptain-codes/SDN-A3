package org.stemmate.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record SyncLogEntry(
        UUID planId,
        SyncStatus status,
        Instant occurredAt,
        String message) implements Serializable {
}
