CREATE TABLE IF NOT EXISTS activities (
    activity_id UUID PRIMARY KEY,
    level TEXT NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
    topic TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS session_plans (
    plan_id UUID PRIMARY KEY,
    activity_id UUID NOT NULL REFERENCES activities(activity_id),
    received_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS plan_steps (
    plan_id UUID NOT NULL REFERENCES session_plans(plan_id) ON DELETE CASCADE,
    step_number INTEGER NOT NULL,
    instruction TEXT NOT NULL,
    timing_minutes INTEGER NOT NULL CHECK (timing_minutes >= 0),
    PRIMARY KEY (plan_id, step_number)
);
