PRAGMA foreign_keys = ON;

-- Shared catalogue and plan tables. Keep these definitions aligned with
-- server/src/main/resources/schema.sql. Client-only queues/drafts follow below.
CREATE TABLE IF NOT EXISTS activities (
    activity_id TEXT PRIMARY KEY,
    title TEXT NOT NULL DEFAULT '',
    description TEXT NOT NULL DEFAULT '',
    level TEXT NOT NULL DEFAULT '',
    duration_minutes INTEGER NOT NULL DEFAULT 0 CHECK (duration_minutes >= 0),
    topic TEXT NOT NULL DEFAULT '',
    is_available_offline INTEGER NOT NULL DEFAULT 0 CHECK (is_available_offline IN (0, 1))
);

CREATE TABLE IF NOT EXISTS activity_steps (
    activity_id TEXT NOT NULL REFERENCES activities(activity_id) ON DELETE CASCADE,
    step_number INTEGER NOT NULL,
    instruction TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    timing_minutes INTEGER NOT NULL CHECK (timing_minutes >= 0),
    PRIMARY KEY (activity_id, step_number)
);

CREATE TABLE IF NOT EXISTS activity_materials (
    activity_id TEXT NOT NULL REFERENCES activities(activity_id) ON DELETE CASCADE,
    material_number INTEGER NOT NULL,
    name TEXT NOT NULL,
    quantity TEXT NOT NULL DEFAULT '',
    PRIMARY KEY (activity_id, material_number)
);

CREATE TABLE IF NOT EXISTS activity_safety_notes (
    activity_id TEXT NOT NULL REFERENCES activities(activity_id) ON DELETE CASCADE,
    note_number INTEGER NOT NULL,
    text TEXT NOT NULL,
    PRIMARY KEY (activity_id, note_number)
);

CREATE TABLE IF NOT EXISTS session_plans (
    plan_id TEXT PRIMARY KEY,
    activity_id TEXT REFERENCES activities(activity_id),
    title TEXT NOT NULL DEFAULT '',
    target_group TEXT NOT NULL DEFAULT '',
    sync_status TEXT NOT NULL DEFAULT 'WAITING_TO_SYNC'
        CHECK (sync_status IN ('WAITING_TO_SYNC', 'SYNCING', 'SYNCED', 'FAILED_RETRY')),
    created_at TEXT NOT NULL DEFAULT (STRFTIME('%Y-%m-%dT%H:%M:%fZ', 'NOW')),
    updated_at TEXT NOT NULL DEFAULT (STRFTIME('%Y-%m-%dT%H:%M:%fZ', 'NOW')),
    plan_snapshot TEXT
);

CREATE TABLE IF NOT EXISTS plan_steps (
    plan_id TEXT NOT NULL REFERENCES session_plans(plan_id) ON DELETE CASCADE,
    step_number INTEGER NOT NULL,
    instruction TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    timing_minutes INTEGER NOT NULL CHECK (timing_minutes >= 0),
    PRIMARY KEY (plan_id, step_number)
);

CREATE TABLE IF NOT EXISTS plan_materials (
    plan_id TEXT NOT NULL REFERENCES session_plans(plan_id) ON DELETE CASCADE,
    material_number INTEGER NOT NULL,
    name TEXT NOT NULL,
    quantity TEXT NOT NULL DEFAULT '',
    PRIMARY KEY (plan_id, material_number)
);

CREATE TABLE IF NOT EXISTS plan_safety_notes (
    plan_id TEXT NOT NULL REFERENCES session_plans(plan_id) ON DELETE CASCADE,
    note_number INTEGER NOT NULL,
    text TEXT NOT NULL,
    PRIMARY KEY (plan_id, note_number)
);

CREATE TABLE IF NOT EXISTS plan_inclusion_prompts (
    plan_id TEXT NOT NULL REFERENCES session_plans(plan_id) ON DELETE CASCADE,
    prompt_number INTEGER NOT NULL,
    prompt TEXT NOT NULL,
    PRIMARY KEY (plan_id, prompt_number)
);

CREATE INDEX IF NOT EXISTS idx_session_plans_activity ON session_plans(activity_id);

-- Client-only durable work state; these tables are intentionally absent on the server.
CREATE TABLE IF NOT EXISTS drafts (
    plan_id TEXT PRIMARY KEY,
    payload_json TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS outbox (
    plan_id TEXT PRIMARY KEY REFERENCES session_plans(plan_id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'WAITING_TO_SYNC'
        CHECK (status IN ('WAITING_TO_SYNC', 'SYNCING', 'SYNCED', 'FAILED_RETRY')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    last_attempt TEXT,
    error_message TEXT
);

CREATE TABLE IF NOT EXISTS sync_log (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    plan_id TEXT REFERENCES session_plans(plan_id) ON DELETE SET NULL,
    status TEXT NOT NULL,
    message TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (STRFTIME('%Y-%m-%dT%H:%M:%fZ', 'NOW'))
);

CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox(status);
