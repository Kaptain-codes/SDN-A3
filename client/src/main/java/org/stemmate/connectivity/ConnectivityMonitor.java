package org.stemmate.connectivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Prototype connectivity toggle used to trigger synchronization on reconnect.
 */
public final class ConnectivityMonitor {
    private final List<Consumer<Boolean>> listeners = new ArrayList<>();
    private boolean online;

    public ConnectivityMonitor(boolean initiallyOnline) {
        online = initiallyOnline;
    }

    public synchronized boolean isOnline() {
        return online;
    }

    public synchronized void setOnline(boolean online) {
        boolean changed = this.online != online;
        this.online = online;
        if (changed) {
            listeners.forEach(listener -> listener.accept(online));
        }
    }

    public synchronized void addListener(Consumer<Boolean> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }
}
