package org.stemmate.repository;

import org.stemmate.model.EquipmentKitRecord;

import java.util.List;
import java.util.UUID;

public interface EquipmentKitRepository {
    void save(EquipmentKitRecord record);

    void delete(UUID kitId);

    EquipmentKitRecord findById(UUID kitId);

    List<EquipmentKitRecord> findAll();
}
