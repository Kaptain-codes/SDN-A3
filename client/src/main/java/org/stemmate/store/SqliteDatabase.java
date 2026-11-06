package org.stemmate.store;

import org.stemmate.model.Activity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Shared SQLite connection and schema bootstrap for local-first persistence.
 */
public final class SqliteDatabase implements AutoCloseable {
    private static final String SCHEMA = """
            PRAGMA foreign_keys = ON;
            CREATE TABLE IF NOT EXISTS activities (
                activity_id TEXT PRIMARY KEY,
                title TEXT NOT NULL DEFAULT '',
                description TEXT NOT NULL DEFAULT '',
                level TEXT NOT NULL DEFAULT '',
                duration_minutes INTEGER NOT NULL DEFAULT 0 CHECK (duration_minutes >= 0),
                topic TEXT NOT NULL DEFAULT '',
                is_available_offline INTEGER NOT NULL DEFAULT 0 CHECK (is_available_offline IN (0, 1))
            );
            CREATE TABLE IF NOT EXISTS activity_steps (
                activity_id TEXT NOT NULL REFERENCES activities(activity_id) ON DELETE CASCADE,
                step_number INTEGER NOT NULL,
                instruction TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                timing_minutes INTEGER NOT NULL CHECK (timing_minutes >= 0),
                PRIMARY KEY (activity_id, step_number)
            );
            CREATE TABLE IF NOT EXISTS activity_materials (
                activity_id TEXT NOT NULL REFERENCES activities(activity_id) ON DELETE CASCADE,
                material_number INTEGER NOT NULL,
                name TEXT NOT NULL,
                quantity TEXT NOT NULL DEFAULT '',
                PRIMARY KEY (activity_id, material_number)
            );
            CREATE TABLE IF NOT EXISTS activity_safety_notes (
                activity_id TEXT NOT NULL REFERENCES activities(activity_id) ON DELETE CASCADE,
                note_number INTEGER NOT NULL,
                text TEXT NOT NULL,
                PRIMARY KEY (activity_id, note_number)
            );
            CREATE TABLE IF NOT EXISTS session_plans (
                plan_id TEXT PRIMARY KEY,
                activity_id TEXT REFERENCES activities(activity_id),
                title TEXT NOT NULL DEFAULT '',
                target_group TEXT NOT NULL DEFAULT '',
                sync_status TEXT NOT NULL DEFAULT 'WAITING_TO_SYNC' CHECK (sync_status IN ('WAITING_TO_SYNC', 'SYNCING', 'SYNCED', 'FAILED_RETRY')),
                created_at TEXT NOT NULL DEFAULT (STRFTIME('%Y-%m-%dT%H:%M:%fZ', 'NOW')),
                updated_at TEXT NOT NULL DEFAULT (STRFTIME('%Y-%m-%dT%H:%M:%fZ', 'NOW')),
                plan_snapshot TEXT
            );
            CREATE TABLE IF NOT EXISTS plan_steps (
                plan_id TEXT NOT NULL REFERENCES session_plans(plan_id) ON DELETE CASCADE,
                step_number INTEGER NOT NULL,
                instruction TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                timing_minutes INTEGER NOT NULL CHECK (timing_minutes >= 0),
                PRIMARY KEY (plan_id, step_number)
            );
            CREATE TABLE IF NOT EXISTS plan_materials (
                plan_id TEXT NOT NULL REFERENCES session_plans(plan_id) ON DELETE CASCADE,
                material_number INTEGER NOT NULL,
                name TEXT NOT NULL,
                quantity TEXT NOT NULL DEFAULT '',
                PRIMARY KEY (plan_id, material_number)
            );
            CREATE TABLE IF NOT EXISTS plan_safety_notes (
                plan_id TEXT NOT NULL REFERENCES session_plans(plan_id) ON DELETE CASCADE,
                note_number INTEGER NOT NULL,
                text TEXT NOT NULL,
                PRIMARY KEY (plan_id, note_number)
            );
            CREATE TABLE IF NOT EXISTS plan_inclusion_prompts (
                plan_id TEXT NOT NULL REFERENCES session_plans(plan_id) ON DELETE CASCADE,
                prompt_number INTEGER NOT NULL,
                prompt TEXT NOT NULL,
                PRIMARY KEY (plan_id, prompt_number)
            );
            CREATE INDEX IF NOT EXISTS idx_session_plans_activity ON session_plans(activity_id);
            CREATE TABLE IF NOT EXISTS drafts (
                plan_id TEXT PRIMARY KEY,
                payload_json TEXT NOT NULL,
                updated_at TEXT NOT NULL
            );
            CREATE TABLE IF NOT EXISTS outbox (
                plan_id TEXT PRIMARY KEY REFERENCES session_plans(plan_id) ON DELETE CASCADE,
                status TEXT NOT NULL DEFAULT 'WAITING_TO_SYNC' CHECK (status IN ('WAITING_TO_SYNC', 'SYNCING', 'SYNCED', 'FAILED_RETRY')),
                attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
                last_attempt TEXT,
                error_message TEXT
            );
            CREATE TABLE IF NOT EXISTS sync_log (
                log_id INTEGER PRIMARY KEY AUTOINCREMENT,
                plan_id TEXT REFERENCES session_plans(plan_id),
                status TEXT NOT NULL,
                message TEXT NOT NULL,
                created_at TEXT NOT NULL DEFAULT (STRFTIME('%Y-%m-%dT%H:%M:%fZ', 'NOW'))
            );
            CREATE INDEX IF NOT EXISTS idx_session_plans_activity ON session_plans(activity_id);
            CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox(status);
            """;

    private final Connection connection;

    public SqliteDatabase() {
        this(defaultPath());
    }

    public SqliteDatabase(Path path) {
        try {
            if (!path.toString().equals(":memory:")) {
                Path parent = path.toAbsolutePath().getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
            }
            connection = DriverManager.getConnection(
                    path.toString().equals(":memory:") ? "jdbc:sqlite::memory:" : "jdbc:sqlite:" + path);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.executeUpdate(SCHEMA);
            }
            migrateExistingSchema();
        } catch (SQLException | java.io.IOException exception) {
            throw new IllegalStateException("Unable to open SQLite database at " + path, exception);
        }
    }

    public static Path defaultPath() {
        String configured = System.getProperty("stemmate.sqlite.path");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("STEMMATE_SQLITE_PATH");
        }
        return Paths.get(configured == null || configured.isBlank()
                ? Paths.get(System.getProperty("user.home"), ".stemmate", "stemmate.db").toString()
                : configured);
    }

    Connection connection() {
        return connection;
    }

    public synchronized void cacheActivity(Activity activity) {
        try {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            String id = activity.activityId().toString();
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO activities(activity_id, title, description, level, duration_minutes, topic, is_available_offline)
                    VALUES (?, ?, ?, ?, ?, ?, 1)
                    ON CONFLICT(activity_id) DO UPDATE SET
                        title = excluded.title,
                        description = excluded.description,
                        level = excluded.level,
                        duration_minutes = excluded.duration_minutes,
                        topic = excluded.topic,
                        is_available_offline = 1
                    """)) {
                statement.setString(1, id);
                statement.setString(2, activity.title());
                statement.setString(3, activity.description());
                statement.setString(4, activity.level());
                statement.setInt(5, activity.durationMinutes());
                statement.setString(6, activity.topic());
                statement.executeUpdate();
            }
            replaceActivityChildren(id, activity);
            connection.commit();
            connection.setAutoCommit(autoCommit);
        } catch (SQLException exception) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException rollbackFailure) {
                exception.addSuppressed(rollbackFailure);
            }
            throw new IllegalStateException("Unable to cache activity in SQLite", exception);
        }
    }

    private void replaceActivityChildren(String activityId, Activity activity) throws SQLException {
        for (String table : new String[]{"activity_steps", "activity_materials", "activity_safety_notes"}) {
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM " + table + " WHERE activity_id = ?")) {
                delete.setString(1, activityId);
                delete.executeUpdate();
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO activity_steps(activity_id, step_number, instruction, description, timing_minutes) VALUES (?, ?, ?, ?, ?)")) {
            for (int i = 0; i < activity.steps().size(); i++) {
                var step = activity.steps().get(i);
                insert.setString(1, activityId);
                insert.setInt(2, i + 1);
                insert.setString(3, step.instruction());
                insert.setString(4, step.description());
                insert.setInt(5, step.durationMinutes());
                insert.addBatch();
            }
            insert.executeBatch();
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO activity_materials(activity_id, material_number, name, quantity) VALUES (?, ?, ?, ?)")) {
            for (int i = 0; i < activity.materials().size(); i++) {
                var material = activity.materials().get(i);
                insert.setString(1, activityId);
                insert.setInt(2, i + 1);
                insert.setString(3, material.name());
                insert.setString(4, material.quantity());
                insert.addBatch();
            }
            insert.executeBatch();
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO activity_safety_notes(activity_id, note_number, text) VALUES (?, ?, ?)")) {
            for (int i = 0; i < activity.safetyNotes().size(); i++) {
                insert.setString(1, activityId);
                insert.setInt(2, i + 1);
                insert.setString(3, activity.safetyNotes().get(i).text());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void migrateExistingSchema() throws SQLException {
        if (hasTable("activity")) {
            try (Statement statement = connection.createStatement()) {
                if (hasColumn("activity", "id") && hasColumn("activity", "title")) {
                    String level = hasColumn("activity", "grade_level") ? "COALESCE(grade_level, '')" : "''";
                    String duration = hasColumn("activity", "duration_min") ? "COALESCE(duration_min, 0)" : "0";
                    String topic = hasColumn("activity", "equipment") ? "COALESCE(equipment, '')" : "''";
                    statement.executeUpdate("INSERT OR IGNORE INTO activities(activity_id, title, level, duration_minutes, topic, is_available_offline) "
                            + "SELECT id, COALESCE(title, ''), " + level + ", " + duration + ", " + topic + ", 1 FROM activity");
                }
            }
        }
        ensureColumn("activities", "title", "TEXT NOT NULL DEFAULT ''");
        ensureColumn("activities", "description", "TEXT NOT NULL DEFAULT ''");
        ensureColumn("session_plans", "title", "TEXT NOT NULL DEFAULT ''");
        ensureColumn("session_plans", "target_group", "TEXT NOT NULL DEFAULT ''");
        ensureColumn("session_plans", "plan_snapshot", "TEXT");
        ensureColumn("plan_steps", "description", "TEXT NOT NULL DEFAULT ''");
        if (hasColumn("session_plans", "payload_json")) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("UPDATE session_plans SET plan_snapshot = payload_json " +
                        "WHERE plan_snapshot IS NULL AND payload_json IS NOT NULL");
            }
        }
        if (hasTable("plan") && hasColumn("plan", "payload_json")) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        INSERT OR IGNORE INTO session_plans(plan_id, activity_id, title, target_group, sync_status, created_at, updated_at, plan_snapshot)
                        SELECT plan_id, activity_id, title, target_group, sync_status, created_at, updated_at, payload_json
                        FROM plan
                        """);
            }
        }
        if (hasTable("plan") && hasColumn("plan", "id") && !hasColumn("plan", "payload_json")) {
            String activityId = hasColumn("plan", "activity_id")
                    ? "CASE WHEN activity_id IN (SELECT activity_id FROM activities) THEN activity_id ELSE NULL END" : "NULL";
            String title = hasColumn("plan", "title") ? "COALESCE(title, '')" : "''";
            String targetGroup = hasColumn("plan", "target_group") ? "COALESCE(target_group, '')" : "''";
            String updatedAt = hasColumn("plan", "updated_at")
                    ? "STRFTIME('%Y-%m-%dT%H:%M:%fZ', updated_at / 1000.0, 'unixepoch')"
                    : "STRFTIME('%Y-%m-%dT%H:%M:%fZ', 'NOW')";
            String createdAt = hasColumn("plan", "saved_at")
                    ? "STRFTIME('%Y-%m-%dT%H:%M:%fZ', saved_at / 1000.0, 'unixepoch')"
                    : updatedAt;
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("INSERT OR IGNORE INTO session_plans(plan_id, activity_id, title, target_group, sync_status, created_at, updated_at) "
                        + "SELECT id, " + activityId + ", " + title + ", " + targetGroup + ", 'SYNCED', " + createdAt + ", " + updatedAt + " FROM plan");
                if (hasColumn("plan", "safety")) statement.executeUpdate("INSERT OR IGNORE INTO plan_safety_notes(plan_id, note_number, text) "
                        + "SELECT id, 1, safety FROM plan WHERE safety IS NOT NULL AND TRIM(safety) <> ''");
                if (hasColumn("plan", "inclusion")) statement.executeUpdate("INSERT OR IGNORE INTO plan_inclusion_prompts(plan_id, prompt_number, prompt) "
                        + "SELECT id, 1, inclusion FROM plan WHERE inclusion IS NOT NULL AND TRIM(inclusion) <> ''");
            }
        }
    }

    private boolean hasTable(String table) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            statement.setString(1, table);
            try (var result = statement.executeQuery()) { return result.next(); }
        }
    }

    private void ensureColumn(String table, String column, String definition) throws SQLException {
        if (hasColumn(table, column)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private boolean hasColumn(String table, String column) throws SQLException {
        try (Statement statement = connection.createStatement();
             var result = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (result.next()) {
                if (column.equalsIgnoreCase(result.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to close SQLite database", exception);
        }
    }
}
