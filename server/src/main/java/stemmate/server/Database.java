package stemmate.server;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All SQLite work lives here. One shared connection, and every method is
 * "synchronized", so only one request touches the database at a time.
 * That keeps it simple and avoids "database is locked" errors.
 */
public class Database {

    private final Connection conn;

    public Database(String filePath) throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite:" + filePath);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA journal_mode = WAL");   // a crash cannot corrupt the file
            st.execute("PRAGMA synchronous = FULL");   // every commit is written to disk before we answer
            st.execute("PRAGMA foreign_keys = ON");
        }
        runSqlFile("/schema.sql");
        runSqlFile("/seed.sql");
    }

    /** Runs each statement in a .sql file (statements end with ;). */
    private void runSqlFile(String name) throws Exception {
        try (InputStream in = Database.class.getResourceAsStream(name); Statement st = conn.createStatement()) {
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            for (String part : sql.split(";")) {
                if (!part.replaceAll("--.*", "").isBlank()) st.execute(part);
            }
        }
    }

    // ---------- sync ----------

    /**
     * Handles one outbox item and returns the answer {status, message}.
     * The change and its sync_log row are saved together (one transaction),
     * so a dropped connection can never leave half a change behind.
     */
    public synchronized Map<String, String> handleItem(String deviceId, Map<String, String> item) throws SQLException {
        String id = item.get("id");
        if (Validator.isBlank(id)) return answer("REJECTED", "Item id is missing");

        // Seen this item before? Send the same answer again and change nothing.
        Map<String, String> earlier = findLog(id);
        if (earlier != null) return earlier;

        String action = item.getOrDefault("action", "");
        Map<String, String> result;
        conn.setAutoCommit(false);
        try {
            if (action.equals("SAVE_PLAN")) result = savePlan(deviceId, item);
            else if (action.equals("SAVE_OFFLINE")) result = saveOfflineItem(deviceId, item.get("activityId"));
            else if (action.equals("CHECKOUT_KIT")) result = answer("REJECTED", "Kit custody (FR7) is deferred in A4");
            else result = answer("REJECTED", "Unknown action: " + action);

            writeLog(id, deviceId, action, result);
            conn.commit();
            return result;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private Map<String, String> savePlan(String deviceId, Map<String, String> data) throws SQLException {
        List<String> problems = Validator.checkPlan(data);
        String activityId = Validator.isBlank(data.get("activityId")) ? null : data.get("activityId");
        if (problems.isEmpty() && activityId != null && !activityExists(activityId)) {
            problems.add("Activity " + activityId + " does not exist");
        }
        if (!problems.isEmpty()) return answer("REJECTED", String.join("; ", problems));

        // Last-write-wins: an older edit never overwrites a newer one.
        String sql = "INSERT INTO plan (id, device_id, title, activity_id, target_group, safety, inclusion, updated_at, saved_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(id) DO UPDATE SET title = excluded.title, activity_id = excluded.activity_id, "
                + "target_group = excluded.target_group, safety = excluded.safety, inclusion = excluded.inclusion, "
                + "updated_at = excluded.updated_at, saved_at = excluded.saved_at "
                + "WHERE excluded.updated_at >= plan.updated_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, data.get("planId"));
            ps.setString(2, deviceId);
            ps.setString(3, data.get("title").trim());
            ps.setString(4, activityId);
            ps.setString(5, data.get("targetGroup"));
            ps.setString(6, data.get("safety").trim());
            ps.setString(7, data.get("inclusion"));
            ps.setLong(8, Long.parseLong(data.get("updatedAt")));
            ps.setLong(9, System.currentTimeMillis());
            int changed = ps.executeUpdate();
            return changed == 1
                    ? answer("OK", "Plan saved")
                    : answer("OK", "A newer version is already saved, kept that one");
        }
    }

    private Map<String, String> saveOfflineItem(String deviceId, String activityId) throws SQLException {
        if (Validator.isBlank(activityId) || !activityExists(activityId)) {
            return answer("REJECTED", "Unknown activity: " + activityId);
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO offline_item VALUES (?, ?)")) {
            ps.setString(1, deviceId);
            ps.setString(2, activityId);
            ps.executeUpdate();
        }
        return answer("OK", "Saved for offline use");
    }

    private boolean activityExists(String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM activity WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private Map<String, String> findLog(String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT status, message FROM sync_log WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? answer(rs.getString(1), rs.getString(2)) : null;
            }
        }
    }

    private void writeLog(String id, String deviceId, String action, Map<String, String> result) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO sync_log VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, id);
            ps.setString(2, deviceId);
            ps.setString(3, action);
            ps.setString(4, result.get("status"));
            ps.setString(5, result.get("message"));
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    private static Map<String, String> answer(String status, String message) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("status", status);
        m.put("message", message);
        return m;
    }

    // ---------- reads ----------

    /** Runs a SELECT and returns each row as a map of column name -> value. */
    public synchronized List<Map<String, String>> query(String sql, String... params) throws SQLException {
        List<Map<String, String>> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setString(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                int columns = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Map<String, String> row = new LinkedHashMap<>();
                    for (int c = 1; c <= columns; c++) row.put(rs.getMetaData().getColumnLabel(c), rs.getString(c));
                    rows.add(row);
                }
            }
        }
        return rows;
    }
}
