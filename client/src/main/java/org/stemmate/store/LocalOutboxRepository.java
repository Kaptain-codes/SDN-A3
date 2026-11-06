package org.stemmate.store;

import org.stemmate.model.SyncRecord;
import org.stemmate.model.SyncStatus;
import org.stemmate.repository.OutboxRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class LocalOutboxRepository implements OutboxRepository {
    private final SqliteDatabase database;

    public LocalOutboxRepository() {
        this(new SqliteDatabase());
    }

    public LocalOutboxRepository(SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    @Override
    public synchronized void enqueue(SyncRecord record) {
        Objects.requireNonNull(record, "record");
        execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO outbox(plan_id, status, last_attempt, error_message)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT(plan_id) DO UPDATE SET
                        status = excluded.status,
                        last_attempt = excluded.last_attempt,
                        error_message = excluded.error_message
                    """)) {
                bind(statement, record);
                statement.executeUpdate();
            }
        });
    }

    @Override
    public synchronized SyncRecord findByPlanId(UUID planId) {
        return queryOne("SELECT plan_id, status, last_attempt, error_message FROM outbox WHERE plan_id = ?",
                Objects.requireNonNull(planId, "planId"));
    }

    @Override
    public synchronized List<SyncRecord> findPending() {
        return query("SELECT plan_id, status, last_attempt, error_message FROM outbox " +
                "WHERE status IN ('WAITING_TO_SYNC', 'FAILED_RETRY') ORDER BY rowid");
    }

    @Override
    public synchronized void updateStatus(UUID planId, SyncStatus status, Instant lastAttempt, String errorMessage) {
        execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE outbox SET status = ?, last_attempt = ?, error_message = ? WHERE plan_id = ?")) {
                statement.setString(1, Objects.requireNonNull(status, "status").name());
                statement.setString(2, lastAttempt == null ? null : lastAttempt.toString());
                statement.setString(3, errorMessage);
                statement.setString(4, Objects.requireNonNull(planId, "planId").toString());
                statement.executeUpdate();
            }
        });
    }

    synchronized void upsert(Connection connection, SyncRecord record) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO outbox(plan_id, status, last_attempt, error_message)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(plan_id) DO UPDATE SET
                    status = excluded.status,
                    last_attempt = excluded.last_attempt,
                    error_message = excluded.error_message
                """)) {
            bind(statement, record);
            statement.executeUpdate();
        }
    }

    private SyncRecord queryOne(String sql, UUID planId) {
        List<SyncRecord> records = query(sql, planId);
        return records.isEmpty() ? null : records.get(0);
    }

    private List<SyncRecord> query(String sql, Object... parameters) {
        List<SyncRecord> records = new ArrayList<>();
        execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < parameters.length; i++) {
                    statement.setString(i + 1, parameters[i].toString());
                }
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        records.add(new SyncRecord(
                                UUID.fromString(result.getString("plan_id")),
                                SyncStatus.valueOf(result.getString("status")),
                                parseInstant(result.getString("last_attempt")),
                                result.getString("error_message")));
                    }
                }
            }
        });
        return records;
    }

    private static void bind(PreparedStatement statement, SyncRecord record) throws SQLException {
        statement.setString(1, record.planId().toString());
        statement.setString(2, record.state().name());
        statement.setString(3, record.lastAttempt() == null ? null : record.lastAttempt().toString());
        statement.setString(4, record.errorMessage());
    }

    private static Instant parseInstant(String value) {
        return value == null ? null : Instant.parse(value);
    }

    private void execute(SqlAction action) {
        try {
            action.run(database.connection());
        } catch (SQLException exception) {
            throw new IllegalStateException("SQLite outbox operation failed", exception);
        }
    }

    @FunctionalInterface
    private interface SqlAction {
        void run(java.sql.Connection connection) throws SQLException;
    }
}
