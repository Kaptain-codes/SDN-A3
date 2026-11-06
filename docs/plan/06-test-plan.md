# A4 selected-slice test plan

This plan targets the rich client in `client/` and thin server in `server/`. Existing pass claims in `server/README.md` came from the legacy `prototype/src` client and are not accepted as selected-path evidence. Retain commands, outputs, and the exact client/server revision for each run.

| Test | Method / evidence source | Requirement | Current result | Follow-up |
|---|---|---|---|---|
| Server startup and health | Start server; call `GET /health` | SUS1 | Server source compiled; runtime not verified in this pass | Retain response and environment details |
| Legacy server schema migration | Build databases for singular Role 5 and partial legacy schema variants; start server; compare copied rows/children | QR1, FR4 | Migration code updated; not verified | Include activity, safety, inclusion, timestamps, and device mapping |
| Local snapshot/list behavior | Mix local snapshots with remote rows whose `plan_snapshot` is null; call `findAll()` | FR3, QR1 | Null filtering added; not verified | Confirm local plans remain visible and null rows are skipped |
| Rich client plan sync | Save plan and inspect server parent/child rows | FR3, FR4, FR5 | Not verified; activity ID format mismatch remains | Reconcile UUID vs `ACT-###`, then run end-to-end |
| Client restart persistence | Save draft and pending plan, close/relaunch, inspect restored fields/outbox | FR5, FR6, QR1 | Code exists; not verified | Verify full fields and no data loss |
| Dropped connection/retry | Proxy delay/drop tests against rich client | FR5, QR1, QR2 | Prototype results only; selected path not run | Retain proxy logs and verify exactly-once persistence |
| Activity filtering | Exercise level/topic/duration/material filters online and offline | FR1 | UI filters implemented; offline data integration incomplete | Connect UI catalogue to SQLite and repeat |
| Offline activity opening | Save activity, close app, disable network, reopen activity | FR2 | Not verified; client UI still uses in-memory catalogue | Persist and load full activity content locally |
| Phone workflow | Repeat FR1-FR6 acceptance on phone | QR3 | Not implemented | Implement phone client and record same acceptance script |
| Low-spec responsiveness | Agree threshold, test named low-spec hardware | QR2 | Measure unresolved | Define threshold before claiming pass |
| Unsynced logout warning | Queue work and exit application | QR2 | No logout flow/warning | Implement and verify warning |
| Administrator log access | Inspect recent sync failures through administrator-visible interface | SUS1 | Server log exists; no viewing UI | Add inspection route/UI and document access |
| Accessibility walkthrough | Keyboard, focus, labels, contrast, status/error checks on rich client | AC1 | Not verified | Complete WCAG 2.2 AA checks and record fixes |
| Kit checkout / plan duplicate | Confirm excluded behavior matches current scope | FR7, FR8 | Deferred | No feature pass claim |

At least 12 documented and 4 automated tests are an A4 deliverable. This evidence map does not by itself satisfy that count.
