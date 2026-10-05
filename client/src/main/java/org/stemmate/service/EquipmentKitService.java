package org.stemmate.service;

import org.stemmate.model.EquipmentKitRecord;
import org.stemmate.repository.EquipmentKitRepository;
import org.stemmate.repository.exception.ValidationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class EquipmentKitService {
    private final EquipmentKitRepository repository;

    public EquipmentKitService(EquipmentKitRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<EquipmentKitRecord> findAll() {
        return repository.findAll();
    }

    public EquipmentKitRecord findById(UUID kitId) {
        return repository.findById(Objects.requireNonNull(kitId, "kitId"));
    }

    public void saveRecord(UUID kitId, String kitName, String responsiblePerson, LocalDate intendedDate, String status) {
        validate(kitName, responsiblePerson, intendedDate, status);
        repository.save(new EquipmentKitRecord(
                kitId == null ? UUID.randomUUID() : kitId,
                kitName.trim(),
                responsiblePerson.trim(),
                intendedDate,
                status.trim(),
                "Returned".equalsIgnoreCase(status)));
    }

    public void deleteRecord(UUID kitId) {
        repository.delete(Objects.requireNonNull(kitId, "kitId"));
    }

    private static void validate(String kitName, String responsiblePerson, LocalDate intendedDate, String status) {
        if (kitName == null || kitName.isBlank()) {
            throw new ValidationException("Kit name is required.");
        }
        if (responsiblePerson == null || responsiblePerson.isBlank()) {
            throw new ValidationException("Responsible person is required.");
        }
        if (intendedDate == null) {
            throw new ValidationException("Intended date is required.");
        }
        if (status == null || status.isBlank()) {
            throw new ValidationException("Status is required.");
        }
    }
}
