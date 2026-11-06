package stemmate.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * STEMMate Namibia server (Role 5). A thin server: it checks the data, stores it in
 * SQLite and sends back a short answer. All the real work (forms, drafts, the outbox)
 * happens on the client, so the server stays small and the network traffic stays tiny.
 *
 * Messages are in a simple format (both directions): one record per line, written like a URL query:
 *     id=6f1c...&action=SAVE_PLAN&title=Wind%20Turbine
 * This is much smaller than JSON and needs no extra library to read.
 *
 * Endpoints:
 *   GET  /health                  -> status=OK
 *   GET  /activities              -> one activity per line (FR1)
 *   GET  /plans?deviceId=x        -> one plan per line (deviceId is optional)
 *   POST /sync?deviceId=x         -> body: outbox items, one per line (max 50)
 *                                    reply: id=...&status=OK|REJECTED&message=... for each item (FR5)
 */
public class Server {

    static final int MAX_ITEMS = 50;
    static final int MAX_BODY_BYTES = 256 * 1024;

    static Database db;

    public static void main(String[] args) throws Exception {
        String dbFile = System.getenv().getOrDefault("STEMMATE_DB_FILE", "stemmate.db");
        int port = Integer.parseInt(System.getenv().getOrDefault("STEMMATE_PORT", "8080"));

        db = new Database(dbFile);

        HttpServer http = HttpServer.create(new InetSocketAddress(port), 0);
        http.createContext("/", Server::handle);
        http.setExecutor(Executors.newFixedThreadPool(4)); // one slow connection cannot block everyone else
        http.start();
        System.out.println("STEMMate server running on http://localhost:" + port + "/health (database: " + dbFile + ")");
    }

    static void handle(HttpExchange ex) {
        try {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            Map<String, String> params = decode(ex.getRequestURI().getRawQuery());

            if (method.equals("GET") && path.equals("/health")) {
                reply(ex, 200, "status=OK");
            } else if (method.equals("GET") && path.equals("/activities")) {
                reply(ex, 200, encodeAll(db.query("SELECT * FROM activity ORDER BY id")));
            } else if (method.equals("GET") && path.equals("/plans")) {
                String deviceId = params.get("deviceId");
                List<Map<String, String>> rows = (deviceId == null)
                        ? db.query("SELECT * FROM plan ORDER BY updated_at DESC")
                        : db.query("SELECT * FROM plan WHERE device_id = ? ORDER BY updated_at DESC", deviceId);
                reply(ex, 200, encodeAll(rows));
            } else if (method.equals("POST") && path.equals("/sync")) {
                handleSync(ex, params.get("deviceId"));
            } else {
                reply(ex, 404, "status=ERROR&message=" + enc("Not found: " + method + " " + path));
            }
        } catch (Exception e) {
            // Database or other problem: the client keeps its items and tries again later.
            System.err.println("Error: " + e);
            reply(ex, 503, "status=ERROR&message=" + enc("Server problem, try again later"));
        }
    }

    static void handleSync(HttpExchange ex, String deviceId) throws Exception {
        if (Validator.isBlank(deviceId) || !deviceId.matches("[A-Za-z0-9_-]{1,64}")) {
            reply(ex, 400, "status=ERROR&message=" + enc("deviceId is missing or invalid"));
            return;
        }
        byte[] body = ex.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            reply(ex, 400, "status=ERROR&message=" + enc("Too much data in one request"));
            return;
        }

        List<String> lines = new ArrayList<>();
        for (String line : new String(body, StandardCharsets.UTF_8).split("\n")) {
            if (!line.isBlank()) lines.add(line.trim());
        }
        if (lines.size() > MAX_ITEMS) {
            reply(ex, 400, "status=ERROR&message=" + enc("Send at most " + MAX_ITEMS + " items at a time"));
            return;
        }

        // Each item gets its own answer, so one bad item never blocks the others.
        StringBuilder answer = new StringBuilder();
        for (String line : lines) {
            Map<String, String> item = decode(line);
            Map<String, String> result = db.handleItem(deviceId, item);
            answer.append("id=").append(enc(item.getOrDefault("id", "")))
                  .append("&status=").append(result.get("status"))
                  .append("&message=").append(enc(result.get("message")))
                  .append("\n");
        }
        reply(ex, 200, answer.toString());
    }

    // helpers 

    static void reply(HttpExchange ex, int code, String text) {
        try {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            ex.sendResponseHeaders(code, bytes.length);
            try (OutputStream out = ex.getResponseBody()) { out.write(bytes); }
        } catch (Exception ignored) {
            // the client already hung up (bad connection); it will retry
        } finally {
            ex.close();
        }
    }

    /**"a=1&b=2" -> {a=1, b=2} */
    static Map<String, String> decode(String text) {
        Map<String, String> map = new LinkedHashMap<>();
        if (text == null || text.isBlank()) return map;
        for (String pair : text.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) map.put(dec(pair.substring(0, eq)), dec(pair.substring(eq + 1)));
        }
        return map;
    }

    /** {a=1, b=2} -> "a=1&b=2" (empty values are left out) */
    static String encode(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getValue() == null) continue;
            if (sb.length() > 0) sb.append('&');
            sb.append(enc(e.getKey())).append('=').append(enc(e.getValue()));
        }
        return sb.toString();
    }

    static String encodeAll(List<Map<String, String>> rows) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> row : rows) sb.append(encode(row)).append("\n");
        return sb.toString();
    }

    static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    static String dec(String s) { return URLDecoder.decode(s, StandardCharsets.UTF_8); }
}
