package org.stemmate.repository.exception;

public class StorageFullException extends RuntimeException {
    public StorageFullException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageFullException(String message) {
        super(message);
    }
}
