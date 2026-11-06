-- STEMMate Namibia: SQLite schema (A4 vertical slice)
-- Safe to run on every start  nothing is created twice.

-- FR1/FR2: the STEM activity library
CREATE TABLE IF NOT EXISTS activity (
    id            TEXT PRIMARY KEY,       -- e.g. ACT-101 (same IDs the client shows)
    title         TEXT NOT NULL,
    grade_level   TEXT NOT NULL,
    duration_min  INTEGER NOT NULL,
    equipment     TEXT NOT NULL
);

-- FR3/FR4/FR6: session plans. The id is made on the client,
-- so a plan saved offline keeps the same id when it syncs.
CREATE TABLE IF NOT EXISTS plan (
    id            TEXT PRIMARY KEY,
    device_id     TEXT NOT NULL,
    title         TEXT NOT NULL,
    activity_id   TEXT REFERENCES activity(id),
    target_group  TEXT,
    safety        TEXT NOT NULL,
    inclusion     TEXT,
    updated_at    INTEGER NOT NULL,       -- when the teacher last edited it (ms since 1970), used for last-write-wins
    saved_at      INTEGER NOT NULL        -- when the server stored it
);

-- FR2: which activities each device keeps offline
CREATE TABLE IF NOT EXISTS offline_item (
    device_id     TEXT NOT NULL,
    activity_id   TEXT NOT NULL REFERENCES activity(id),
    PRIMARY KEY (device_id, activity_id)
);

-- FR5: every outbox item the server has already handled, with its answer.
-- If an item is sent twice (bad connection), the stored answer is sent back again.
CREATE TABLE IF NOT EXISTS sync_log (
    id            TEXT PRIMARY KEY,
    device_id     TEXT NOT NULL,
    action        TEXT NOT NULL,
    status        TEXT NOT NULL,          -- OK or REJECTED
    message       TEXT NOT NULL,
    received_at   INTEGER NOT NULL
);
