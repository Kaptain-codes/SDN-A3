package stemmate.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Checks incoming data. The server never trusts the client:
 * the same rules apply even if the form already checked them.
 * Each method returns a list of problems. An empty list means the data is fine.
 */
public class Validator {

    public static List<String> checkPlan(Map<String, String> data) {
        List<String> problems = new ArrayList<>();

        if (isBlank(data.get("planId"))) problems.add("planId is missing");

        String title = data.get("title");
        if (isBlank(title)) problems.add("Plan title is required");
        else if (title.length() > 120) problems.add("Plan title is too long (max 120)");

        // FR4: a plan must say how learners are kept safe
        if (isBlank(data.get("safety"))) problems.add("Safety controls are required (FR4)");

        String activityId = data.get("activityId");
        if (!isBlank(activityId) && !activityId.matches("ACT-\\d{3}")) problems.add("activityId must look like ACT-101");

        if (!isNumber(data.get("updatedAt"))) problems.add("updatedAt must be a number");

        return problems;
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static boolean isNumber(String s) {
        return s != null && s.matches("\\d{1,15}");
    }
}
