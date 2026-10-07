package org.stemmate.repository;

import org.stemmate.model.InclusionPrompt;
import org.stemmate.model.Material;
import org.stemmate.model.PlanStep;
import org.stemmate.model.SafetyNote;
import org.stemmate.model.SessionPlan;
import org.stemmate.repository.exception.NetworkUnavailableException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Remote repository adapter for the Ballerina sync API.
 */
public final class HttpRemoteSessionPlanStore implements RemoteSessionPlanStore {
    private static final int MAX_BATCH_SIZE = 20;
    private static final Pattern CONFIRMED_PLAN_ID =
            Pattern.compile("\"planId\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");

    private final HttpClient client;
    private final URI endpoint;
    private final Duration timeout;

    public HttpRemoteSessionPlanStore(String baseUrl) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                baseUrl, Duration.ofSeconds(15));
    }

    public HttpRemoteSessionPlanStore(HttpClient client, String baseUrl, Duration timeout) {
        this.client = Objects.requireNonNull(client, "client");
        this.endpoint = URI.create(stripTrailingSlash(Objects.requireNonNull(baseUrl, "baseUrl")) + "/plans");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
    }

    @Override
    public Set<UUID> uploadBatch(List<SessionPlan> plans) {
        Objects.requireNonNull(plans, "plans");
        if (plans.isEmpty()) {
            return Set.of();
        }
        if (plans.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("A sync batch cannot contain more than 20 plans.");
        }

        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(plans)))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new NetworkUnavailableException(
                        "The sync service rejected the upload (HTTP " + response.statusCode() + ").");
            }
            return parseConfirmation(response.body());
        } catch (IOException exception) {
            throw new NetworkUnavailableException("The sync service could not be reached.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new NetworkUnavailableException("The sync request was interrupted.", exception);
        }
    }

    private static Set<UUID> parseConfirmation(String body) {
        if (body == null || body.isBlank() || !body.contains("\"accepted\"")) {
            throw new NetworkUnavailableException("The sync service returned an invalid confirmation.");
        }
        Set<UUID> confirmed = CONFIRMED_PLAN_ID.matcher(body).results()
                .map(match -> UUID.fromString(match.group(1)))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        if (confirmed.isEmpty()) {
            throw new NetworkUnavailableException("The sync service confirmed no plan IDs.");
        }
        return confirmed;
    }

    private static String toJson(List<SessionPlan> plans) {
        return "{\"plans\":" + join(plans, HttpRemoteSessionPlanStore::planJson) + "}";
    }

    private static String planJson(SessionPlan plan) {
        StringBuilder json = new StringBuilder("{");
        field(json, "planId", plan.planId().toString());
        field(json, "activityId", plan.activityId().toString());
        field(json, "title", plan.title());
        field(json, "targetGroup", plan.targetGroup());
        field(json, "createdAt", plan.createdAt().toString());
        field(json, "updatedAt", plan.updatedAt().toString());
        field(json, "steps", join(plan.steps(), HttpRemoteSessionPlanStore::stepJson));
        field(json, "timingMinutes", join(plan.timingMinutes(), String::valueOf));
        field(json, "materials", join(plan.materials(), HttpRemoteSessionPlanStore::materialJson));
        field(json, "safetyNotes", join(plan.safetyNotes(), note -> quote(note.text())));
        field(json, "inclusionPrompts", join(plan.inclusionPrompts(), prompt -> quote(prompt.prompt())));
        return json.append('}').toString();
    }

    private static String stepJson(PlanStep step) {
        return "{\"instruction\":" + quote(step.instruction())
                + ",\"durationMinutes\":" + step.durationMinutes()
                + ",\"description\":" + quote(step.description()) + "}";
    }

    private static String materialJson(Material material) {
        return "{\"name\":" + quote(material.name()) + ",\"quantity\":" + quote(material.quantity()) + "}";
    }

    private static void field(StringBuilder json, String name, String value) {
        if (json.length() > 1) {
            json.append(',');
        }
        json.append(quote(name)).append(':').append(value);
    }

    private static <T> String join(List<T> values, Function<T, String> mapper) {
        return values.stream().map(mapper).collect(Collectors.joining(",", "[", "]"));
    }

    private static String quote(String value) {
        Objects.requireNonNull(value, "JSON value");
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t") + "\"";
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
