package org.stemmate.store;

import org.stemmate.model.SyncRecord;
import org.stemmate.model.SyncStatus;
import org.stemmate.repository.OutboxRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Local outbox adapter with one logical record per plan ID.
 */
public final class LocalOutboxRepository implements OutboxRepository {
    private static final java.nio.file.Path STORE_PATH = java.nio.file.Paths.get(
            System.getProperty("user.home"), ".stemmate", "outbox.ser");
    private final Map<UUID, SyncRecord> records = new LinkedHashMap<>();

    public LocalOutboxRepository() {
        load();
    }

    @Override
    public synchronized void enqueue(SyncRecord record) {
        Objects.requireNonNull(record, "record");
        records.put(record.planId(), record);
        save();
    }

    @Override
    public synchronized SyncRecord findByPlanId(UUID planId) {
        return records.get(Objects.requireNonNull(planId, "planId"));
    }

    @Override
    public synchronized List<SyncRecord> findPending() {
        return records.values().stream()
                .filter(record -> record.state() == SyncStatus.WAITING_TO_SYNC
                        || record.state() == SyncStatus.FAILED_RETRY)
                .toList();
    }

    @Override
    public synchronized void updateStatus(
            UUID planId, SyncStatus status, java.time.Instant lastAttempt, String errorMessage) {
        UUID id = Objects.requireNonNull(planId, "planId");
        if (!records.containsKey(id)) {
            return;
        }
        records.put(id, new SyncRecord(id, Objects.requireNonNull(status, "status"),
                lastAttempt, errorMessage));
        save();
    }

    public synchronized List<SyncRecord> findAll() {
        return new ArrayList<>(records.values());
    }

    private void load() {
        @SuppressWarnings("unchecked")
        Map<UUID, SyncRecord> restored = FileStoreSupport.read(
                STORE_PATH, LinkedHashMap.class, new LinkedHashMap<UUID, SyncRecord>());
        records.clear();
        records.putAll(restored);
    }

    private void save() {
        FileStoreSupport.write(STORE_PATH, (java.io.Serializable) new LinkedHashMap<>(records));
    }
}
