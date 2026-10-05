package org.stemmate.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

public record EquipmentKitRecord(
        UUID kitId,
        String kitName,
        String responsiblePerson,
        LocalDate intendedDate,
        String status,
        boolean returned) implements Serializable {
}
