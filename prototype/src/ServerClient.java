import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends outbox items to the STEMMate server (server/ folder) and reads the answers.
 *
 * Built for a weak connection:
 *  - items go in small batches (10 at a time), so one dropped request loses little
 *  - short timeouts, so the app never hangs waiting on a dead line
 *  - answers from batches that did get through are kept, even if a later batch fails
 *
 * Settings (environment variables, optional):
 *   STEMMATE_SERVER_URL  default http://localhost:8080
 *   STEMMATE_DEVICE_ID   default laptop-<computer name>
 */
public class ServerClient {

    static final int BATCH_SIZE = 10;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final String serverUrl;
    private final String deviceId;

    public ServerClient() {
        String url = System.getenv("STEMMATE_SERVER_URL");
        serverUrl = (url == null || url.isBlank()) ? "http://localhost:8080" : url;

        String id = System.getenv("STEMMATE_DEVICE_ID");
        if (id == null || id.isBlank()) id = "laptop-" + System.getenv().getOrDefault("COMPUTERNAME", "unknown");
        deviceId = id.replaceAll("[^A-Za-z0-9_-]", "-");
    }

    /**
     * Sends the lines and returns the server's answer for each item, by item id.
     * Each answer has "status" (OK or REJECTED) and "message".
     * Throws an exception only if nothing at all got through.
     */
    public Map<String, Map<String, String>> send(List<String> lines) throws Exception {
        Map<String, Map<String, String>> answers = new HashMap<>();
        for (int start = 0; start < lines.size(); start += BATCH_SIZE) {
            List<String> batch = lines.subList(start, Math.min(start + BATCH_SIZE, lines.size()));
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(serverUrl + "/sync?deviceId=" + deviceId))
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.ofString(String.join("\n", batch)))
                        .build();
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) throw new Exception("Server answered " + response.statusCode());

                for (String line : response.body().split("\n")) {
                    if (line.isBlank()) continue;
                    Map<String, String> answer = decode(line);
                    answers.put(answer.get("id"), answer);
                }
            } catch (Exception e) {
                if (answers.isEmpty()) throw e; // nothing got through: caller keeps everything waiting
                break;                          // some got through: keep those, retry the rest later
            }
        }
        return answers;
    }

    // line format: a=1&b=2 (same as the server side)

    public static Map<String, String> decode(String text) {
        Map<String, String> map = new LinkedHashMap<>();
        if (text == null || text.isBlank()) return map;
        for (String pair : text.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) map.put(dec(pair.substring(0, eq)), dec(pair.substring(eq + 1)));
        }
        return map;
    }

    public static String encode(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getValue() == null) continue;
            if (sb.length() > 0) sb.append('&');
            sb.append(enc(e.getKey())).append('=').append(enc(e.getValue()));
        }
        return sb.toString();
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    private static String dec(String s) { return URLDecoder.decode(s, StandardCharsets.UTF_8); }
}
