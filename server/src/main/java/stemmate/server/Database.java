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
import java.time.Instant;

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
        migrateLegacySchema();
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

    /** Preserve Role 5's earlier singular-table data when opening an existing database. */
    private void migrateLegacySchema() throws SQLException {
        if (tableExists("activity")) migrateLegacyActivities();
        if (tableExists("plan")) migrateLegacyPlans();
        if (tableExists("offline_item")) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("""
                    INSERT OR IGNORE INTO activity_offline_selections(device_id, activity_id)
                    SELECT device_id, activity_id FROM offline_item
                    """);
            }
        }
    }

    private void migrateLegacyActivities() throws SQLException {
        if (!columnExists("activity", "id") || !columnExists("activity", "title")) return;
        String level = columnExists("activity", "grade_level") ? "COALESCE(grade_level, '')" : "''";
        String duration = columnExists("activity", "duration_min") ? "COALESCE(duration_min, 0)" : "0";
        String topic = columnExists("activity", "equipment") ? "COALESCE(equipment, '')" : "''";
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("INSERT OR IGNORE INTO activities(activity_id, title, level, duration_minutes, topic) "
                    + "SELECT id, COALESCE(title, ''), " + level + ", " + duration + ", " + topic + " FROM activity");
            if (columnExists("activity", "equipment")) {
                st.executeUpdate("INSERT OR IGNORE INTO activity_materials(activity_id, material_number, name, quantity) "
                        + "SELECT id, 1, equipment, '' FROM activity WHERE equipment IS NOT NULL AND TRIM(equipment) <> ''");
            }
        }
    }

    private void migrateLegacyPlans() throws SQLException {
        if (!columnExists("plan", "id")) return;
        String activity = columnExists("plan", "activity_id") ? "activity_id" : "NULL";
        String title = columnExists("plan", "title") ? "COALESCE(title, '')" : "''";
        String targetGroup = columnExists("plan", "target_group") ? "COALESCE(target_group, '')" : "''";
        String updated = columnExists("plan", "updated_at")
                ? "STRFTIME('%Y-%m-%dT%H:%M:%fZ', updated_at / 1000.0, 'unixepoch')"
                : "STRFTIME('%Y-%m-%dT%H:%M:%fZ', 'NOW')";
        String created = columnExists("plan", "saved_at")
                ? "STRFTIME('%Y-%m-%dT%H:%M:%fZ', saved_at / 1000.0, 'unixepoch')"
                : updated;
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("INSERT OR IGNORE INTO session_plans(plan_id, activity_id, title, target_group, sync_status, created_at, updated_at) "
                    + "SELECT id, " + activity + ", " + title + ", " + targetGroup + ", 'SYNCED', " + created + ", " + updated + " FROM plan");
            if (columnExists("plan", "safety")) {
                st.executeUpdate("INSERT OR IGNORE INTO plan_safety_notes(plan_id, note_number, text) "
                        + "SELECT id, 1, safety FROM plan WHERE safety IS NOT NULL AND TRIM(safety) <> ''");
            }
            if (columnExists("plan", "inclusion")) {
                st.executeUpdate("INSERT OR IGNORE INTO plan_inclusion_prompts(plan_id, prompt_number, prompt) "
                        + "SELECT id, 1, inclusion FROM plan WHERE inclusion IS NOT NULL AND TRIM(inclusion) <> ''");
            }
            if (columnExists("plan", "device_id")) {
                st.executeUpdate("INSERT OR IGNORE INTO session_plan_devices(plan_id, device_id) "
                        + "SELECT id, device_id FROM plan WHERE device_id IS NOT NULL");
            }
        }
    }

    private boolean tableExists(String table) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private boolean columnExists(String table, String column) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) if (column.equalsIgnoreCase(rs.getString("name"))) return true;
            return false;
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
            try {
                if (action.equals("SAVE_PLAN")) result = savePlan(deviceId, item);
                else if (action.equals("SAVE_OFFLINE")) result = saveOfflineItem(deviceId, item.get("activityId"));
                else if (action.equals("CHECKOUT_KIT")) result = answer("REJECTED", "Kit custody (FR7) is deferred in A4");
                else result = answer("REJECTED", "Unknown action: " + action);
            } catch (IllegalArgumentException invalid) {
                result = answer("REJECTED", "Invalid plan data");
            }

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
        long updatedMillis;
        try {
            updatedMillis = Long.parseLong(data.get("updatedAt"));
            validateRichFields(data);
        } catch (IllegalArgumentException invalid) {
            return answer("REJECTED", "Invalid plan data");
        }
        String updatedAt = Instant.ofEpochMilli(updatedMillis).toString();
        String now = Instant.now().toString();
        String sql = "INSERT INTO session_plans (plan_id, activity_id, title, target_group, sync_status, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, 'SYNCED', ?, ?) "
                + "ON CONFLICT(plan_id) DO UPDATE SET title = excluded.title, activity_id = excluded.activity_id, "
                + "target_group = excluded.target_group, sync_status = 'SYNCED', updated_at = excluded.updated_at "
                + "WHERE excluded.updated_at >= session_plans.updated_at";
        int changed;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, data.get("planId"));
            ps.setString(2, activityId);
            ps.setString(3, data.get("title").trim());
            ps.setString(4, data.getOrDefault("targetGroup", ""));
            ps.setString(5, now);
            ps.setString(6, updatedAt);
            changed = ps.executeUpdate();
        }
        if (changed == 1) {
            replacePlanChildren(data);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR IGNORE INTO session_plan_devices(plan_id, device_id) VALUES (?, ?)")) {
                ps.setString(1, data.get("planId"));
                ps.setString(2, deviceId);
                ps.executeUpdate();
            }
        }
        return changed == 1
                ? answer("OK", "Plan saved")
                : answer("OK", "A newer version is already saved, kept that one");
    }

    private void replacePlanChildren(Map<String, String> data) throws SQLException {
        String planId = data.get("planId");
        for (String table : new String[]{"plan_steps", "plan_materials", "plan_safety_notes", "plan_inclusion_prompts"}) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table + " WHERE plan_id = ?")) {
                ps.setString(1, planId);
                ps.executeUpdate();
            }
        }
        int steps = count(data, "stepCount");
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO plan_steps(plan_id, step_number, instruction, description, timing_minutes) VALUES (?, ?, ?, ?, ?)")) {
            for (int i = 1; i <= steps; i++) {
                ps.setString(1, planId); ps.setInt(2, i);
                ps.setString(3, data.getOrDefault("step" + i + "Instruction", ""));
                ps.setString(4, data.getOrDefault("step" + i + "Description", ""));
                ps.setInt(5, integer(data.get("step" + i + "TimingMinutes")));
                ps.addBatch();
            }
            ps.executeBatch();
        }
        int materials = count(data, "materialCount");
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO plan_materials(plan_id, material_number, name, quantity) VALUES (?, ?, ?, ?)")) {
            for (int i = 1; i <= materials; i++) {
                ps.setString(1, planId); ps.setInt(2, i);
                ps.setString(3, data.getOrDefault("material" + i + "Name", ""));
                ps.setString(4, data.getOrDefault("material" + i + "Quantity", ""));
                ps.addBatch();
            }
            ps.executeBatch();
        }
        int safety = count(data, "safetyCount");
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO plan_safety_notes(plan_id, note_number, text) VALUES (?, ?, ?)")) {
            for (int i = 1; i <= safety; i++) {
                ps.setString(1, planId); ps.setInt(2, i);
                ps.setString(3, data.getOrDefault("safety" + i + "Text", "")); ps.addBatch();
            }
            if (safety == 0 && !Validator.isBlank(data.get("safety"))) {
                ps.setString(1, planId); ps.setInt(2, 1); ps.setString(3, data.get("safety")); ps.addBatch();
            }
            ps.executeBatch();
        }
        int inclusions = count(data, "inclusionCount");
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO plan_inclusion_prompts(plan_id, prompt_number, prompt) VALUES (?, ?, ?)")) {
            for (int i = 1; i <= inclusions; i++) {
                ps.setString(1, planId); ps.setInt(2, i);
                ps.setString(3, data.getOrDefault("inclusion" + i + "Prompt", "")); ps.addBatch();
            }
            if (inclusions == 0 && !Validator.isBlank(data.get("inclusion"))) {
                ps.setString(1, planId); ps.setInt(2, 1); ps.setString(3, data.get("inclusion")); ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static int count(Map<String, String> data, String key) throws SQLException {
        String value = data.getOrDefault(key, "0");
        if (!value.matches("\\d{1,3}")) throw new SQLException("Invalid " + key);
        int count = Integer.parseInt(value);
        if (count > 100) throw new SQLException("Too many " + key + " entries");
        return count;
    }

    private static int integer(String value) throws SQLException {
        if (value == null || !value.matches("\\d{1,5}")) throw new SQLException("Invalid step timing");
        return Integer.parseInt(value);
    }

    private static void validateRichFields(Map<String, String> data) {
        int steps = parseCount(data, "stepCount");
        for (int i = 1; i <= steps; i++) parseNonNegative(data.get("step" + i + "TimingMinutes"));
        parseCount(data, "materialCount");
        parseCount(data, "safetyCount");
        parseCount(data, "inclusionCount");
    }

    private static int parseCount(Map<String, String> data, String key) {
        String value = data.getOrDefault(key, "0");
        if (!value.matches("\\d{1,3}")) throw new IllegalArgumentException("Invalid " + key);
        int count = Integer.parseInt(value);
        if (count > 100) throw new IllegalArgumentException("Too many " + key);
        return count;
    }

    private static int parseNonNegative(String value) {
        if (value == null || !value.matches("\\d{1,5}")) throw new IllegalArgumentException("Invalid timing");
        return Integer.parseInt(value);
    }

    private Map<String, String> saveOfflineItem(String deviceId, String activityId) throws SQLException {
        if (Validator.isBlank(activityId) || !activityExists(activityId)) {
            return answer("REJECTED", "Unknown activity: " + activityId);
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO activity_offline_selections VALUES (?, ?)")) {
            ps.setString(1, deviceId);
            ps.setString(2, activityId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE activities SET is_available_offline = 1 WHERE activity_id = ?")) {
            ps.setString(1, activityId);
            ps.executeUpdate();
        }
        return answer("OK", "Saved for offline use");
    }

    private boolean activityExists(String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM activities WHERE activity_id = ?")) {
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
