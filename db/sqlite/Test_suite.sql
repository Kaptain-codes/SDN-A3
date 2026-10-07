PRAGMA foreign_keys = ON;

-- ========================================================
-- SETUP: Seed Required Parent Data
-- ========================================================
INSERT INTO activities (activity_id, level, duration_minutes, topic, is_available_offline)
VALUES ('ACT-101', 'Grade 8', 45, 'Photosynthesis', 1);

INSERT INTO session_plans (plan_id, activity_id, sync_status)
VALUES ('PLAN-001', 'ACT-101', 'WAITING_TO_SYNC');

-- ========================================================
-- TEST 1: DB-TS-01 (Foreign Key Enforcement)
-- EXPECTED: Fails with "FOREIGN KEY constraint failed"
-- ========================================================
-- INSERT INTO session_plans (plan_id, activity_id) VALUES ('PLAN-999', 'INVALID-ACT');

-- ========================================================
-- TEST 2: DB-TS-02 (Negative Duration Check)
-- EXPECTED: Fails with "CHECK constraint failed"
-- ========================================================
-- INSERT INTO activities VALUES ('ACT-999', 'Grade 8', -10, 'Math', 1);

-- ========================================================
-- TEST 3: DB-TS-05 (JSON Syntax Validation)
-- EXPECTED: Fails with "CHECK constraint failed"
-- ========================================================
-- INSERT INTO drafts (plan_id, payload_json) VALUES ('PLAN-001', '{malformed_json: true');

-- ========================================================
-- TEST 4: Valid JSON Insert (Passing Case)
-- ========================================================
INSERT INTO drafts (plan_id, payload_json) 
VALUES ('PLAN-001', '{"title": "Lesson 1", "completed": false}');

-- ========================================================
-- TEST 5: DB-TS-06 (Outbox Trigger Automation Test)
-- EXPECTED: Trigger automatically updates session_plans.sync_status to 'SYNCING'
-- ========================================================
INSERT INTO outbox (plan_id, status) VALUES ('PLAN-001', 'SYNCING');

-- Verify Trigger Result (Should show 'SYNCING' in session_plans):
SELECT plan_id, sync_status, updated_at FROM session_plans WHERE plan_id = 'PLAN-001';

-- Update Outbox Status to 'SYNCED'
UPDATE outbox SET status = 'SYNCED' WHERE plan_id = 'PLAN-001';

-- Verify Trigger Result (Should show 'SYNCED' in session_plans):
SELECT plan_id, sync_status, updated_at FROM session_plans WHERE plan_id = 'PLAN-001';

-- ========================================================
-- TEST 6: DB-TS-03 & DB-TS-04 (Cascade Delete & Audit Log Preservation)
-- EXPECTED: Deleting session_plan deletes draft & outbox, but sets sync_log.plan_id to NULL
-- ========================================================
INSERT INTO sync_log (plan_id, status, message) 
VALUES ('PLAN-001', 'SYNCED', 'Payload transmitted successfully');

-- Delete the parent plan
DELETE FROM session_plans WHERE plan_id = 'PLAN-001';

-- Check Child Tables (Drafts & Outbox should be EMPTY 0 rows):
SELECT COUNT(*) AS remaining_drafts FROM drafts WHERE plan_id = 'PLAN-001';
SELECT COUNT(*) AS remaining_outbox FROM outbox WHERE plan_id = 'PLAN-001';

-- Check Audit Log (Log row exists, plan_id is now NULL):
SELECT log_id, plan_id, status, message FROM sync_log;
