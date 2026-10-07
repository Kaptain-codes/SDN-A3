PRAGMA foreign_keys = ON;

-- 1. Activities Table
CREATE TABLE IF NOT EXISTS activities (
    activity_id TEXT PRIMARY KEY,
    level TEXT NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
    topic TEXT NOT NULL,
    is_available_offline INTEGER NOT NULL DEFAULT 0 CHECK (is_available_offline IN (0, 1))
);

-- 2. Session Plans Table
CREATE TABLE IF NOT EXISTS session_plans (
    plan_id TEXT PRIMARY KEY,
    activity_id TEXT NOT NULL REFERENCES activities(activity_id) ON DELETE RESTRICT,
    sync_status TEXT NOT NULL DEFAULT 'WAITING_TO_SYNC' CHECK (sync_status IN ('WAITING_TO_SYNC', 'SYNCING', 'SYNCED', 'FAILED_RETRY')),
    created_at TEXT NOT NULL DEFAULT (STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW')),
    updated_at TEXT NOT NULL DEFAULT (STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW'))
);

-- 3. Plan Steps Table
CREATE TABLE IF NOT EXISTS plan_steps (
    plan_id TEXT NOT NULL REFERENCES session_plans(plan_id) ON DELETE CASCADE,
    step_number INTEGER NOT NULL,
    instruction TEXT NOT NULL,
    timing_minutes INTEGER NOT NULL CHECK (timing_minutes >= 0),
    PRIMARY KEY (plan_id, step_number)
);

-- 4. Drafts Table (Fixed: Added FK with CASCADE deletion)
CREATE TABLE IF NOT EXISTS drafts (
    plan_id TEXT PRIMARY KEY REFERENCES session_plans(plan_id) ON DELETE CASCADE,
    payload_json TEXT NOT NULL CHECK (json_valid(payload_json)),
    updated_at TEXT NOT NULL DEFAULT (STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW'))
);

-- 5. Outbox Queue Table
CREATE TABLE IF NOT EXISTS outbox (
    plan_id TEXT PRIMARY KEY REFERENCES session_plans(plan_id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'WAITING_TO_SYNC' CHECK (status IN ('WAITING_TO_SYNC', 'SYNCING', 'SYNCED', 'FAILED_RETRY')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    last_attempt TEXT,
    error_message TEXT
);

-- 6. Sync Log Audit Table (Fixed: ON DELETE SET NULL to preserve historical logs)
CREATE TABLE IF NOT EXISTS sync_log (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    plan_id TEXT REFERENCES session_plans(plan_id) ON DELETE SET NULL,
    status TEXT NOT NULL,
    message TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW'))
);

-- INDEXES FOR OPTIMIZED SYNC QUEUE PROCESSING
CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox(status);
CREATE INDEX IF NOT EXISTS idx_session_plans_activity ON session_plans(activity_id);

-- TRIGGERS TO GUARANTEE DUAL-TABLE SYNC STATUS CONSISTENCY
CREATE TRIGGER IF NOT EXISTS trg_sync_outbox_to_plan_update
AFTER UPDATE OF status ON outbox
BEGIN
    UPDATE session_plans 
    SET sync_status = NEW.status,
        updated_at = STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW')
    WHERE plan_id = NEW.plan_id;
END;

CREATE TRIGGER IF NOT EXISTS trg_sync_outbox_to_plan_insert
AFTER INSERT ON outbox
BEGIN
    UPDATE session_plans 
    SET sync_status = NEW.status,
        updated_at = STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW')
    WHERE plan_id = NEW.plan_id;
END;
