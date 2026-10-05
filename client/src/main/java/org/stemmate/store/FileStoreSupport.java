package org.stemmate.store;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

final class FileStoreSupport {
    private FileStoreSupport() {
    }

    static <T> T read(Path path, Class<T> type, T defaultValue) {
        if (!Files.exists(path)) {
            return defaultValue;
        }
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(path))) {
            Object value = input.readObject();
            return type.cast(value);
        } catch (IOException | ClassNotFoundException exception) {
            return defaultValue;
        }
    }

    static void write(Path path, Serializable value) {
        try {
            Files.createDirectories(path.getParent());
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(path))) {
                output.writeObject(value);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write persistent store " + path, exception);
        }
    }
}
