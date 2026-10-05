package org.stemmate.store;

import org.stemmate.model.EquipmentKitRecord;
import org.stemmate.repository.EquipmentKitRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class LocalEquipmentKitStore implements EquipmentKitRepository {
    private static final java.nio.file.Path STORE_PATH = java.nio.file.Paths.get(
            System.getProperty("user.home"), ".stemmate", "kits.ser");
    private final Map<UUID, EquipmentKitRecord> records = new LinkedHashMap<>();

    public LocalEquipmentKitStore() {
        load();
    }

    @Override
    public synchronized void save(EquipmentKitRecord record) {
        Objects.requireNonNull(record, "record");
        records.put(record.kitId(), record);
        saveState();
    }

    @Override
    public synchronized void delete(UUID kitId) {
        records.remove(Objects.requireNonNull(kitId, "kitId"));
        saveState();
    }

    @Override
    public synchronized EquipmentKitRecord findById(UUID kitId) {
        return records.get(Objects.requireNonNull(kitId, "kitId"));
    }

    @Override
    public synchronized List<EquipmentKitRecord> findAll() {
        return new ArrayList<>(records.values());
    }

    @SuppressWarnings("unchecked")
    private void load() {
        Map<UUID, EquipmentKitRecord> restored = FileStoreSupport.read(
                STORE_PATH, LinkedHashMap.class, new LinkedHashMap<UUID, EquipmentKitRecord>());
        records.clear();
        records.putAll(restored);
    }

    private void saveState() {
        FileStoreSupport.write(STORE_PATH, (java.io.Serializable) new LinkedHashMap<>(records));
    }
}
