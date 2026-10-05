package org.stemmate.model;

import java.io.Serializable;

public record Material(String name, String quantity) implements Serializable {
}
