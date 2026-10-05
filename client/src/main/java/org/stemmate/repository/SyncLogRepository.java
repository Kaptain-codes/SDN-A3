package org.stemmate.repository;

import org.stemmate.model.SyncLogEntry;

import java.util.List;

/**
 * Local boundary for inspecting recent synchronization failures.
 */
public interface SyncLogRepository {
    void append(SyncLogEntry entry);

    List<SyncLogEntry> findRecent(int limit);
}
