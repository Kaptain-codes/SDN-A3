PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS activities (
    activity_id TEXT PRIMARY KEY,
    level TEXT NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
    topic TEXT NOT NULL,
    is_available_offline INTEGER NOT NULL DEFAULT 0 CHECK (is_available_offline IN (0, 1))
);

CREATE TABLE IF NOT EXISTS session_plans (
    plan_id TEXT PRIMARY KEY,
    activity_id TEXT NOT NULL REFERENCES activities(activity_id),
    sync_status TEXT NOT NULL CHECK (sync_status IN ('WAITING_TO_SYNC', 'SYNCING', 'SYNCED', 'FAILED_RETRY')),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS plan_steps (
    plan_id TEXT NOT NULL REFERENCES session_plans(plan_id) ON DELETE CASCADE,
    step_number INTEGER NOT NULL,
    instruction TEXT NOT NULL,
    timing_minutes INTEGER NOT NULL CHECK (timing_minutes >= 0),
    PRIMARY KEY (plan_id, step_number)
);

CREATE TABLE IF NOT EXISTS drafts (
    plan_id TEXT PRIMARY KEY,
    payload_json TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS outbox (
    plan_id TEXT PRIMARY KEY REFERENCES session_plans(plan_id) ON DELETE CASCADE,
    status TEXT NOT NULL CHECK (status IN ('WAITING_TO_SYNC', 'SYNCING', 'SYNCED', 'FAILED_RETRY')),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_attempt TEXT,
    error_message TEXT
);

CREATE TABLE IF NOT EXISTS sync_log (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    plan_id TEXT REFERENCES session_plans(plan_id),
    status TEXT NOT NULL,
    message TEXT NOT NULL,
    created_at TEXT NOT NULL
);

-- session_plans.sync_status and outbox.status are updated in the same transaction.
