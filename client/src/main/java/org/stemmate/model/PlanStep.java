package org.stemmate.model;

import java.io.Serializable;

public record PlanStep(String instruction, int durationMinutes, String description) implements Serializable {
}
