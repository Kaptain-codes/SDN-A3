package org.stemmate.sync;

import org.stemmate.connectivity.ConnectivityMonitor;
import org.stemmate.model.SessionPlan;
import org.stemmate.model.SyncLogEntry;
import org.stemmate.model.SyncRecord;
import org.stemmate.model.SyncStatus;
import org.stemmate.repository.OutboxRepository;
import org.stemmate.repository.RemoteSessionPlanStore;
import org.stemmate.repository.SessionPlanRepository;
import org.stemmate.repository.SyncLogRepository;
import org.stemmate.repository.exception.NetworkUnavailableException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Synchronizes locally saved plans in bounded batches and preserves failures for retry.
 */
public final class SyncManager {
    public static final int MAX_BATCH_SIZE = 20;

    private final SessionPlanRepository planRepository;
    private final OutboxRepository outboxRepository;
    private final RemoteSessionPlanStore remoteStore;
    private final ConnectivityMonitor connectivityMonitor;
    private final Clock clock;
    private final SyncLogRepository syncLogRepository;
    private Consumer<SyncStatus> statusListener = status -> {
    };
    private static final SyncLogRepository NO_OP_LOG = new SyncLogRepository() {
        @Override
        public void append(SyncLogEntry entry) {
        }

        @Override
        public List<SyncLogEntry> findRecent(int limit) {
            return List.of();
        }
    };

    public SyncManager(
            SessionPlanRepository planRepository,
            OutboxRepository outboxRepository,
            RemoteSessionPlanStore remoteStore,
            ConnectivityMonitor connectivityMonitor) {
        this(planRepository, outboxRepository, remoteStore, connectivityMonitor,
                Clock.systemUTC(), NO_OP_LOG);
    }

    public SyncManager(
            SessionPlanRepository planRepository,
            OutboxRepository outboxRepository,
            RemoteSessionPlanStore remoteStore,
            ConnectivityMonitor connectivityMonitor,
            Clock clock) {
        this(planRepository, outboxRepository, remoteStore, connectivityMonitor, clock, NO_OP_LOG);
    }

    public SyncManager(
            SessionPlanRepository planRepository,
            OutboxRepository outboxRepository,
            RemoteSessionPlanStore remoteStore,
            ConnectivityMonitor connectivityMonitor,
            Clock clock,
            SyncLogRepository syncLogRepository) {
        this.planRepository = Objects.requireNonNull(planRepository, "planRepository");
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository");
        this.remoteStore = Objects.requireNonNull(remoteStore, "remoteStore");
        this.connectivityMonitor = Objects.requireNonNull(connectivityMonitor, "connectivityMonitor");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.syncLogRepository = Objects.requireNonNull(syncLogRepository, "syncLogRepository");
        connectivityMonitor.addListener(online -> {
            if (online) {
                syncPending();
            }
        });
    }

    public void setStatusListener(Consumer<SyncStatus> statusListener) {
        this.statusListener = Objects.requireNonNull(statusListener, "statusListener");
    }

    public void syncPending() {
        if (!connectivityMonitor.isOnline()) {
            return;
        }
        List<SyncRecord> pending = outboxRepository.findPending();
        for (int offset = 0; offset < pending.size(); offset += MAX_BATCH_SIZE) {
            syncBatch(pending.subList(offset, Math.min(offset + MAX_BATCH_SIZE, pending.size())));
        }
    }

    private void syncBatch(List<SyncRecord> records) {
        Instant attemptTime = Instant.now(clock);
        List<SessionPlan> plans = records.stream()
                .map(SyncRecord::planId)
                .map(planRepository::findById)
                .filter(Objects::nonNull)
                .toList();
        plans.forEach(plan -> mark(plan.planId(), SyncStatus.SYNCING, attemptTime, null));
        try {
            Set<UUID> confirmed = remoteStore.uploadBatch(plans);
            confirmed.forEach(planId -> mark(planId, SyncStatus.SYNCED, attemptTime, null));
            plans.stream()
                    .map(SessionPlan::planId)
                    .filter(planId -> !confirmed.contains(planId))
                    .forEach(planId -> mark(planId, SyncStatus.FAILED_RETRY, attemptTime,
                            "The server did not confirm this plan."));
        } catch (NetworkUnavailableException exception) {
            plans.forEach(plan -> mark(plan.planId(), SyncStatus.FAILED_RETRY, attemptTime,
                    exception.getMessage()));
        }
    }

    private void mark(UUID planId, SyncStatus status, Instant attemptTime, String errorMessage) {
        outboxRepository.updateStatus(planId, status, attemptTime, errorMessage);
        planRepository.updateSyncStatus(planId, status);
        if (status == SyncStatus.FAILED_RETRY) {
            syncLogRepository.append(new SyncLogEntry(
                    planId,
                    status,
                    attemptTime,
                    errorMessage == null || errorMessage.isBlank()
                            ? "Synchronization failed and can be retried."
                            : errorMessage));
        }
        statusListener.accept(status);
    }
}
