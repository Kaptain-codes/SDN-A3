package org.stemmate.store;

import org.junit.jupiter.api.Test;
import org.stemmate.model.Activity;
import org.stemmate.model.InclusionPrompt;
import org.stemmate.model.Material;
import org.stemmate.model.PlanStep;
import org.stemmate.model.SafetyNote;
import org.stemmate.model.SessionPlan;
import org.stemmate.model.SyncStatus;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SessionPlanPersistenceAcceptanceTest {
    @Test
    void planFiveElementsDraftAndOutboxSurviveDatabaseReopen() throws Exception {
        Path targetDir = Paths.get(System.getProperty(
                "stemmate.acceptance.dbDir",
                System.getProperty("java.io.tmpdir")));
        java.nio.file.Files.createDirectories(targetDir);
        Path dbPath = targetDir.resolve("acceptance-" + UUID.randomUUID() + ".db");
        SessionPlan plan = samplePlan();

        try {
            try (SqliteDatabase database = new SqliteDatabase(dbPath)) {
                database.cacheActivity(new Activity(plan.activityId(), "Acceptance activity",
                        "Synthetic activity for local plan acceptance.", "Grade 8-10", 40,
                        "Engineering", plan.steps(), plan.materials(), plan.safetyNotes()));
                LocalOutboxRepository outbox = new LocalOutboxRepository(database);
                LocalSessionPlanStore store = new LocalSessionPlanStore(database, outbox);
                store.save(plan);
                store.saveDraft(plan);
            }

            try (SqliteDatabase reopened = new SqliteDatabase(dbPath)) {
                LocalOutboxRepository outbox = new LocalOutboxRepository(reopened);
                LocalSessionPlanStore store = new LocalSessionPlanStore(reopened, outbox);

                SessionPlan restoredPlan = store.findById(plan.planId());
                SessionPlan restoredDraft = store.findLatestDraft();
                assertNotNull(restoredPlan);
                assertNotNull(restoredDraft);
                assertEquals(plan.activityId(), restoredPlan.activityId());
                assertEquals(plan.title(), restoredPlan.title());
                assertEquals(plan.targetGroup(), restoredPlan.targetGroup());
                assertEquals(plan.steps(), restoredPlan.steps());
                assertEquals(plan.timingMinutes(), restoredPlan.timingMinutes());
                assertEquals(plan.materials(), restoredPlan.materials());
                assertEquals(plan.safetyNotes(), restoredPlan.safetyNotes());
                assertEquals(plan.inclusionPrompts(), restoredPlan.inclusionPrompts());
                assertEquals(plan.steps(), restoredDraft.steps());
                assertEquals(plan.timingMinutes(), restoredDraft.timingMinutes());
                assertEquals(plan.materials(), restoredDraft.materials());
                assertEquals(plan.safetyNotes(), restoredDraft.safetyNotes());
                assertEquals(plan.inclusionPrompts(), restoredDraft.inclusionPrompts());
                assertEquals(1, store.findAll().size());
                assertEquals(1, outbox.findPending().size());
                assertEquals(2, childRowCount(reopened, "plan_steps", plan.planId()));
                assertEquals(2, childRowCount(reopened, "plan_materials", plan.planId()));
                assertEquals(2, childRowCount(reopened, "plan_safety_notes", plan.planId()));
                assertEquals(2, childRowCount(reopened, "plan_inclusion_prompts", plan.planId()));
            }
        } finally {
            java.nio.file.Files.deleteIfExists(Path.of(dbPath + "-wal"));
            java.nio.file.Files.deleteIfExists(Path.of(dbPath + "-shm"));
            java.nio.file.Files.deleteIfExists(dbPath);
        }
    }

    private static int childRowCount(SqliteDatabase database, String table, UUID planId) throws Exception {
        try (var statement = database.connection().prepareStatement(
                "SELECT COUNT(*) FROM " + table + " WHERE plan_id = ?")) {
            statement.setString(1, planId.toString());
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static SessionPlan samplePlan() {
        Instant now = Instant.now();
        return new SessionPlan(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Acceptance persistence plan",
                "Grade 8-10",
                List.of(new PlanStep("Prepare", 15, "Set up materials."),
                        new PlanStep("Build", 25, "Complete the activity.")),
                List.of(15, 25),
                List.of(new Material("Cardboard", "2 sheets"), new Material("Water", "1 litre")),
                List.of(new SafetyNote("Wear eye protection."), new SafetyNote("Keep the floor dry.")),
                List.of(new InclusionPrompt("Offer a tactile example."),
                        new InclusionPrompt("Allow written or spoken responses.")),
                SyncStatus.WAITING_TO_SYNC,
                now,
                now);
    }
}
