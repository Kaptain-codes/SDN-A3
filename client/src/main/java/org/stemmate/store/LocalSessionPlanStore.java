package org.stemmate.store;

import org.stemmate.model.SessionPlan;
import org.stemmate.model.PlanStep;
import org.stemmate.model.Material;
import org.stemmate.model.SyncRecord;
import org.stemmate.model.SyncStatus;
import org.stemmate.repository.SessionPlanRepository;
import org.stemmate.repository.exception.ValidationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * SQLite-backed local-first plan repository.
 */
public final class LocalSessionPlanStore implements SessionPlanRepository {
    private final SqliteDatabase database;
    private final LocalOutboxRepository outboxRepository;

    public LocalSessionPlanStore() {
        this(new SqliteDatabase());
    }

    public LocalSessionPlanStore(SqliteDatabase database) {
        this(database, new LocalOutboxRepository(database));
    }

    public LocalSessionPlanStore(LocalOutboxRepository outboxRepository) {
        this(new SqliteDatabase(), outboxRepository);
    }

    public LocalSessionPlanStore(SqliteDatabase database, LocalOutboxRepository outboxRepository) {
        this.database = Objects.requireNonNull(database, "database");
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository");
    }

    @Override
    public synchronized void save(SessionPlan plan) {
        validateSaveInput(plan);
        SessionPlan localPlan = withWaitingStatus(plan);
        try {
            Connection connection = database.connection();
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO session_plans(plan_id, activity_id, title, target_group, sync_status, created_at, updated_at, plan_snapshot)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT(plan_id) DO UPDATE SET
                            activity_id = excluded.activity_id,
                            title = excluded.title,
                            target_group = excluded.target_group,
                            sync_status = excluded.sync_status,
                            created_at = excluded.created_at,
                            updated_at = excluded.updated_at,
                            plan_snapshot = excluded.plan_snapshot
                        """)) {
                    statement.setString(1, localPlan.planId().toString());
                    statement.setString(2, localPlan.activityId().toString());
                    statement.setString(3, localPlan.title());
                    statement.setString(4, localPlan.targetGroup());
                    statement.setString(5, localPlan.syncStatus().name());
                    statement.setString(6, localPlan.createdAt().toString());
                    statement.setString(7, localPlan.updatedAt().toString());
                    statement.setString(8, serialize(localPlan));
                    statement.executeUpdate();
                }
                replacePlanChildren(connection, localPlan);
                outboxRepository.upsert(connection, new SyncRecord(
                        localPlan.planId(), SyncStatus.WAITING_TO_SYNC, null, null));
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("SQLite plan save failed", exception);
        }
    }

    @Override
    public synchronized SessionPlan findById(UUID planId) {
        return queryOne("SELECT plan_snapshot FROM session_plans WHERE plan_id = ?", planId);
    }

    @Override
    public synchronized List<SessionPlan> findAll() {
        List<SessionPlan> plans = new ArrayList<>();
        execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT plan_snapshot FROM session_plans WHERE plan_snapshot IS NOT NULL AND plan_snapshot <> '' ORDER BY rowid");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String snapshot = result.getString(1);
                    if (snapshot != null && !snapshot.isBlank()) plans.add(deserialize(snapshot));
                }
            }
        });
        return plans;
    }

    @Override
    public synchronized List<SessionPlan> findPending() {
        return outboxRepository.findPending().stream()
                .map(SyncRecord::planId)
                .map(this::findById)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public synchronized void updateSyncStatus(UUID planId, SyncStatus status) {
        execute(connection -> {
            SessionPlan current = queryOne("SELECT plan_snapshot FROM session_plans WHERE plan_id = ?", planId);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE session_plans SET sync_status = ? WHERE plan_id = ?")) {
                statement.setString(1, Objects.requireNonNull(status, "status").name());
                statement.setString(2, Objects.requireNonNull(planId, "planId").toString());
                statement.executeUpdate();
            }
            if (current != null) {
                SessionPlan updated = new SessionPlan(current.planId(), current.activityId(), current.title(),
                        current.targetGroup(), current.steps(), current.timingMinutes(), current.materials(),
                        current.safetyNotes(), current.inclusionPrompts(), status,
                        current.createdAt(), current.updatedAt());
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE session_plans SET plan_snapshot = ? WHERE plan_id = ?")) {
                    statement.setString(1, serialize(updated));
                    statement.setString(2, planId.toString());
                    statement.executeUpdate();
                }
            }
        });
    }

    @Override
    public synchronized void saveDraft(SessionPlan draft) {
        Objects.requireNonNull(draft, "draft");
        execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO drafts(plan_id, payload_json, updated_at) VALUES (?, ?, ?)
                    ON CONFLICT(plan_id) DO UPDATE SET
                        payload_json = excluded.payload_json,
                        updated_at = excluded.updated_at
                    """)) {
                statement.setString(1, draft.planId().toString());
                statement.setString(2, serialize(draft));
                statement.setString(3, draft.updatedAt().toString());
                statement.executeUpdate();
            }
        });
    }

    @Override
    public synchronized SessionPlan findDraft(UUID planId) {
        return queryOne("SELECT payload_json FROM drafts WHERE plan_id = ?", planId);
    }

    @Override
    public synchronized SessionPlan findLatestDraft() {
        final SessionPlan[] latest = new SessionPlan[1];
        execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT payload_json FROM drafts ORDER BY updated_at DESC LIMIT 1");
                 ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    latest[0] = deserialize(result.getString(1));
                }
            }
        });
        return latest[0];
    }

    public LocalOutboxRepository outboxRepository() {
        return outboxRepository;
    }

    private SessionPlan queryOne(String sql, UUID planId) {
        final SessionPlan[] plan = new SessionPlan[1];
        execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, Objects.requireNonNull(planId, "planId").toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        plan[0] = deserialize(result.getString(1));
                    }
                }
            }
        });
        return plan[0];
    }

    private void execute(SqlAction action) {
        try {
            action.run(database.connection());
        } catch (SQLException exception) {
            throw new IllegalStateException("SQLite plan operation failed", exception);
        }
    }

    private static SessionPlan withWaitingStatus(SessionPlan plan) {
        return new SessionPlan(plan.planId(), plan.activityId(), plan.title(), plan.targetGroup(),
                List.copyOf(plan.steps()), List.copyOf(plan.timingMinutes()), List.copyOf(plan.materials()),
                List.copyOf(plan.safetyNotes()), List.copyOf(plan.inclusionPrompts()),
                SyncStatus.WAITING_TO_SYNC, plan.createdAt(), plan.updatedAt());
    }

    private static void replacePlanChildren(Connection connection, SessionPlan plan) throws SQLException {
        String planId = plan.planId().toString();
        for (String table : new String[]{"plan_steps", "plan_materials", "plan_safety_notes", "plan_inclusion_prompts"}) {
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM " + table + " WHERE plan_id = ?")) {
                delete.setString(1, planId);
                delete.executeUpdate();
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO plan_steps(plan_id, step_number, instruction, description, timing_minutes) VALUES (?, ?, ?, ?, ?)")) {
            for (int i = 0; i < plan.steps().size(); i++) {
                PlanStep step = plan.steps().get(i);
                insert.setString(1, planId);
                insert.setInt(2, i + 1);
                insert.setString(3, step.instruction());
                insert.setString(4, step.description());
                insert.setInt(5, plan.timingMinutes().get(i));
                insert.addBatch();
            }
            insert.executeBatch();
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO plan_materials(plan_id, material_number, name, quantity) VALUES (?, ?, ?, ?)")) {
            for (int i = 0; i < plan.materials().size(); i++) {
                Material material = plan.materials().get(i);
                insert.setString(1, planId);
                insert.setInt(2, i + 1);
                insert.setString(3, material.name());
                insert.setString(4, material.quantity());
                insert.addBatch();
            }
            insert.executeBatch();
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO plan_safety_notes(plan_id, note_number, text) VALUES (?, ?, ?)")) {
            for (int i = 0; i < plan.safetyNotes().size(); i++) {
                insert.setString(1, planId);
                insert.setInt(2, i + 1);
                insert.setString(3, plan.safetyNotes().get(i).text());
                insert.addBatch();
            }
            insert.executeBatch();
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO plan_inclusion_prompts(plan_id, prompt_number, prompt) VALUES (?, ?, ?)")) {
            for (int i = 0; i < plan.inclusionPrompts().size(); i++) {
                insert.setString(1, planId);
                insert.setInt(2, i + 1);
                insert.setString(3, plan.inclusionPrompts().get(i).prompt());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private static void validateSaveInput(SessionPlan plan) {
        if (plan == null || plan.planId() == null || plan.activityId() == null) {
            throw new ValidationException("A plan and its identifiers are required.");
        }
        if (plan.title() == null || plan.targetGroup() == null
                || plan.steps() == null || plan.timingMinutes() == null
                || plan.steps().size() != plan.timingMinutes().size()
                || plan.materials() == null || plan.safetyNotes() == null
                || plan.inclusionPrompts() == null || plan.createdAt() == null || plan.updatedAt() == null) {
            throw new ValidationException("A complete plan is required.");
        }
    }

    private static String serialize(SessionPlan plan) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
                output.writeObject(plan);
            }
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to serialize session plan", exception);
        }
    }

    private static SessionPlan deserialize(String value) {
        try {
            byte[] bytes = Base64.getDecoder().decode(value);
            try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                return (SessionPlan) input.readObject();
            }
        } catch (IOException | ClassNotFoundException exception) {
            throw new IllegalStateException("Unable to deserialize session plan", exception);
        }
    }

    @FunctionalInterface
    private interface SqlAction {
        void run(Connection connection) throws SQLException;
    }
}
