package org.stemmate.repository;

import org.stemmate.model.InclusionPrompt;
import org.stemmate.model.Material;
import org.stemmate.model.PlanStep;
import org.stemmate.model.SessionPlan;
import org.stemmate.model.SafetyNote;
import org.stemmate.repository.exception.NetworkUnavailableException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Remote repository adapter for the selected Role 5 thin SQLite server. */
public final class HttpRemoteSessionPlanStore implements RemoteSessionPlanStore {
    private static final int MAX_BATCH_SIZE = 10;

    private final HttpClient client;
    private final URI endpoint;
    private final Duration timeout;

    public HttpRemoteSessionPlanStore(String baseUrl) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), baseUrl, Duration.ofSeconds(15));
    }

    public HttpRemoteSessionPlanStore(HttpClient client, String baseUrl, Duration timeout) {
        this.client = Objects.requireNonNull(client, "client");
        this.endpoint = URI.create(stripTrailingSlash(Objects.requireNonNull(baseUrl, "baseUrl")) + "/sync");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
    }

    @Override
    public Set<UUID> uploadBatch(List<SessionPlan> plans) {
        Objects.requireNonNull(plans, "plans");
        if (plans.isEmpty()) return Set.of();
        if (plans.size() > MAX_BATCH_SIZE) throw new IllegalArgumentException("A sync batch cannot contain more than 10 plans.");
        String deviceId = System.getProperty("stemmate.device.id", "desktop-client");
        URI requestUri = URI.create(endpoint + "?deviceId=" + encode(deviceId));
        HttpRequest request = HttpRequest.newBuilder(requestUri).timeout(timeout)
                .header("Content-Type", "text/plain; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(plans.stream().map(HttpRemoteSessionPlanStore::planLine)
                        .collect(Collectors.joining("\n"))))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new NetworkUnavailableException("The sync service rejected the upload (HTTP " + response.statusCode() + ").");
            return parseConfirmation(response.body());
        } catch (IOException exception) {
            throw new NetworkUnavailableException("The sync service could not be reached.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new NetworkUnavailableException("The sync request was interrupted.", exception);
        }
    }

    private static Set<UUID> parseConfirmation(String body) {
        Set<UUID> confirmed = new LinkedHashSet<>();
        if (body == null) throw new NetworkUnavailableException("The sync service returned no confirmation.");
        for (String line : body.split("\\R")) {
            if (line.isBlank()) continue;
            Map<String, String> fields = decode(line);
            if ("OK".equals(fields.get("status"))) {
                try { confirmed.add(UUID.fromString(fields.get("id"))); }
                catch (RuntimeException exception) { throw new NetworkUnavailableException("The sync service returned an invalid plan ID."); }
            }
        }
        return confirmed;
    }

    private static String planLine(SessionPlan plan) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("id", plan.planId().toString());
        fields.put("action", "SAVE_PLAN");
        fields.put("planId", plan.planId().toString());
        fields.put("activityId", plan.activityId().toString());
        fields.put("title", plan.title());
        fields.put("targetGroup", plan.targetGroup());
        fields.put("safety", plan.safetyNotes().stream().map(SafetyNote::text).collect(Collectors.joining("; ")));
        fields.put("inclusion", plan.inclusionPrompts().stream().map(InclusionPrompt::prompt).collect(Collectors.joining("; ")));
        fields.put("updatedAt", String.valueOf(plan.updatedAt().toEpochMilli()));
        fields.put("stepCount", String.valueOf(plan.steps().size()));
        for (int i = 0; i < plan.steps().size(); i++) {
            int n = i + 1; PlanStep step = plan.steps().get(i);
            fields.put("step" + n + "Instruction", step.instruction());
            fields.put("step" + n + "Description", step.description());
            fields.put("step" + n + "TimingMinutes", String.valueOf(plan.timingMinutes().get(i)));
        }
        fields.put("materialCount", String.valueOf(plan.materials().size()));
        for (int i = 0; i < plan.materials().size(); i++) {
            int n = i + 1; Material item = plan.materials().get(i);
            fields.put("material" + n + "Name", item.name()); fields.put("material" + n + "Quantity", item.quantity());
        }
        fields.put("safetyCount", String.valueOf(plan.safetyNotes().size()));
        for (int i = 0; i < plan.safetyNotes().size(); i++) fields.put("safety" + (i + 1) + "Text", plan.safetyNotes().get(i).text());
        fields.put("inclusionCount", String.valueOf(plan.inclusionPrompts().size()));
        for (int i = 0; i < plan.inclusionPrompts().size(); i++) fields.put("inclusion" + (i + 1) + "Prompt", plan.inclusionPrompts().get(i).prompt());
        return fields.entrySet().stream().map(e -> encode(e.getKey()) + "=" + encode(e.getValue())).collect(Collectors.joining("&"));
    }

    private static Map<String, String> decode(String line) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String pair : line.split("&")) {
            int equals = pair.indexOf('=');
            if (equals > 0) fields.put(URLDecoder.decode(pair.substring(0, equals), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8));
        }
        return fields;
    }

    private static String encode(String value) { return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8); }
    private static String stripTrailingSlash(String value) { return value.endsWith("/") ? value.substring(0, value.length() - 1) : value; }
}
