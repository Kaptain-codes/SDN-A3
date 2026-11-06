# STEMMate A4 acceptance checklist

Use this checklist with the selected rich Java Swing client (`client/`) and thin SQLite server (`server/`). The Role 5 app under `prototype/src` is a legacy prototype; its results do not count for this checklist.

## Record for each run

- Date/time:
- Tester:
- Client/server revision:
- Device and OS:
- Server URL:
- Client SQLite path:
- Test ID and requirement:
- Result: Not run / Pass / Fail / Blocked
- Evidence file(s):
- Defect or notes:

Use synthetic data only. Never record learner-identifying information. Save screenshots, console output, or database inspection results under a dated evidence folder and reference that path above.

## Startup check

1. Start the server using `RUNNING_STEMMATE_A4.md`.
2. Request `http://localhost:8080/health`; confirm `status=OK`.
3. Start the rich client and confirm its main window appears without a startup exception.
4. Record the client/server build revision and the database paths.

Result: ______  Evidence: ____________________

## Functional acceptance

### AT-FR1 — Find and filter activities

**Steps**

1. Open the activity catalogue.
2. Apply level, topic, duration, and materials filters individually; then combine at least two filters.
3. Search by a topic/material keyword and reset all filters.
4. Repeat the filtering while offline using activities saved for offline use.

**Pass when:** each filter yields only matching activities, combinations work together, reset restores the catalogue, and the saved offline catalogue can be filtered without a network.

**Current note:** desktop controls and in-memory filtering exist. Persistent offline catalogue integration is incomplete; the offline portion may be blocked.

Result: ______  Evidence: ____________________

### AT-FR2 — Save and open an activity offline

**Steps**

1. Save one activity for offline use.
2. Close and reopen the client; confirm the activity is still in the offline vault.
3. Disable actual network access and open the activity, including its description, steps, materials, and safety notes.
4. Restore network access.

**Pass when:** the full saved activity opens after relaunch with the network unavailable.

**Current note:** the UI catalogue is currently backed by an in-memory activity store; durable SQLite content loading is incomplete. Do not count a persisted selection ID alone as a pass.

Result: ______  Evidence: ____________________

### AT-FR3 — Create and save a plan

**Steps**

1. Create a plan from a selected activity.
2. Enter a title and target group, then save/finalize.
3. Close and reopen the client; confirm the plan remains in the saved-plan list.

**Pass when:** the saved plan remains linked to the chosen activity and its entered fields persist.

Result: ______  Evidence: ____________________

### AT-FR4 — Verify all five plan elements

**Steps**

1. Create a plan with at least two steps, distinct timing values, two materials, two safety notes, and two inclusion prompts.
2. Save it locally and reopen it.
3. Inspect the corresponding client SQLite rows. After sync is unblocked, inspect the server `session_plans` and child tables too.

**Pass when:** steps, timing, materials, safety notes, and inclusion prompts all survive save/reopen; after sync they are also present remotely.

**Current note:** the rich client model, local schema, and wire payload include these fields. Remote acceptance is blocked by the activity-ID mismatch until fixed.

Result: ______  Evidence: ____________________

### AT-FR5 — Offline queue and synchronization

**Steps**

1. Start the client and server, then disable actual connectivity or use a controlled proxy failure.
2. Save a plan and confirm its status is waiting to sync and that the local outbox contains it.
3. Close/reopen the client while offline; confirm the pending item remains.
4. Restore connectivity and allow automatic retry or select Sync.
5. Inspect server plan and sync-log rows. Retry the same item and check for duplicates.

**Pass when:** status changes from waiting to synced after server confirmation, the item remains local until confirmation, and the server stores one plan for repeated delivery.

**Current blocker:** the rich client sends UUID activity IDs while server validation expects `ACT-###`. Resolve this before expecting a pass. Prototype retry results are not evidence for this rich-client test.

Result: ______  Evidence: ____________________

### AT-FR6 — Autosave and restore a draft

**Steps**

1. Start a plan and enter values in every field, including steps, timing, materials, safety, and inclusion.
2. Wait for the autosave indicator to show saved.
3. Close and reopen the client. Repeat once while offline.

**Pass when:** the same draft is restored with every field and no network connection is required.

Result: ______  Evidence: ____________________

## Quality and recovery acceptance

### AT-QR1 — No local loss before confirmation

1. Queue a plan while the server is unavailable.
2. Terminate/restart the client and confirm the plan and outbox record remain.
3. Restore the server and network; confirm the plan syncs.

**Pass when:** the local record remains available until an `OK` confirmation and then syncs without duplication.

**Current blocker:** end-to-end rich-client sync is blocked by activity ID validation.

Result: ______  Evidence: ____________________

### AT-QR2 — Small batches, weak link, low-spec device, and exit warning

1. Queue more than 10 plans and sync through a proxy that adds latency and drops selected requests.
2. Confirm requests contain no large media assets, retries preserve unconfirmed items, and the UI remains usable.
3. Repeat on a named low-spec device and record hardware, OS, timings, and a team-approved responsiveness threshold.
4. Exit with pending items and check for a warning.

**Pass when:** all items eventually sync in small batches; UI meets the agreed performance measure; exit warns before unsynced work is abandoned.

**Current gaps:** no agreed performance threshold, no implemented logout warning, and the sync ID mismatch blocks the full run.

Result: ______  Evidence: ____________________

### AT-QR3 — Equivalent laptop and phone workflow

Run the same FR1–FR6 script on a laptop and a phone. Remove connectivity mid-task and check that status and all five plan elements remain visible and no work is lost.

**Pass when:** both devices complete the same core workflow with equivalent priority and no lost work.

**Current status:** phone implementation is unfinished; mark the phone run Blocked until a phone client exists. Do not downgrade QR3.

Laptop result: ______  Phone result: ______  Evidence: ____________________

### AT-SUS1 — Sync-failure log and recovery guide

1. Cause a sync failure using a controlled invalid item or unavailable server.
2. Confirm a recent failure is visible to an administrator, with enough context to investigate.
3. Follow `docs/supportability.md` to restart the server and recover pending client work.

**Pass when:** an administrator can inspect recent failures and a local technician can restore service without deleting unconfirmed work.

**Current gap:** the server stores idempotency responses, but no administrator-visible failure log interface is implemented.

Result: ______  Evidence: ____________________

### AT-AC1 — Accessibility checks

On the selected client, complete the main workflows using keyboard only, inspect focus order/visibility, check labels and accessible names, measure contrast, verify status is not conveyed by color alone, resize text/window, and trigger validation/network errors.

**Pass when:** the checks listed in the project brief meet WCAG 2.2 AA and every issue/fix is recorded in `docs/plan/05-accessibility.md`.

Result: ______  Evidence: ____________________

## Deferred requirements

- **FR7 — Kit custody:** Deferred by A4 scope. Do not report as passed.
- **FR8 — Duplicate plan:** Deferred by A4 scope. Do not report as passed.

## Run summary

| Requirement | Result (Not run / Pass / Fail / Blocked / Deferred) | Evidence path | Follow-up issue |
|---|---|---|---|
| FR1 | | | |
| FR2 | | | |
| FR3 | | | |
| FR4 | | | |
| FR5 | | | |
| FR6 | | | |
| FR7 | Deferred | | |
| FR8 | Deferred | | |
| QR1 | | | |
| QR2 | | | |
| QR3 | | | |
| SUS1 | | | |
| AC1 | | | |
