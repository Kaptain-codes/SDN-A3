package org.stemmate.repository.exception;

public class NetworkUnavailableException extends RuntimeException {
    public NetworkUnavailableException(String message) {
        super(message);
    }

    public NetworkUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
