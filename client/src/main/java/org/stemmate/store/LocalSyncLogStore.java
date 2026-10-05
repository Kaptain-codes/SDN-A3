package org.stemmate.store;

import org.stemmate.model.SyncLogEntry;
import org.stemmate.repository.SyncLogRepository;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Bounded local synchronization log for support inspection.
 */
public final class LocalSyncLogStore implements SyncLogRepository {
    private static final int MAX_ENTRIES = 200;
    private static final java.nio.file.Path STORE_PATH = java.nio.file.Paths.get(
            System.getProperty("user.home"), ".stemmate", "sync-log.ser");
    private final Deque<SyncLogEntry> entries = new ArrayDeque<>();

    public LocalSyncLogStore() {
        load();
    }

    @Override
    public synchronized void append(SyncLogEntry entry) {
        entries.addFirst(Objects.requireNonNull(entry, "entry"));
        while (entries.size() > MAX_ENTRIES) {
            entries.removeLast();
        }
        save();
    }

    @Override
    public synchronized List<SyncLogEntry> findRecent(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative");
        }
        return entries.stream().limit(limit).toList();
    }

    public synchronized List<SyncLogEntry> findAll() {
        return new ArrayList<>(entries);
    }

    @SuppressWarnings("unchecked")
    private void load() {
        Deque<SyncLogEntry> restored = FileStoreSupport.read(
                STORE_PATH, ArrayDeque.class, new ArrayDeque<SyncLogEntry>());
        entries.clear();
        entries.addAll(restored);
    }

    private void save() {
        FileStoreSupport.write(STORE_PATH, (java.io.Serializable) new ArrayDeque<>(entries));
    }
}
