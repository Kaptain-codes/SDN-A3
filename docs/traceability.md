# A4 requirement traceability

The selected implementation is the rich Java Swing desktop client in `client/` using the thin Java SQLite server in `server/`. The older Role 5 vertical slice in `prototype/src` remains a legacy prototype. Status below reflects source inspection; it does not imply acceptance-test completion.

| Requirement | Selected component | Current evidence | Status | Remaining gap |
|---|---|---|---|---|
| FR1 | `DesktopShellFrame.CatalogPanel`, `ActivityCriteria`, `LocalActivityStore` | Level/topic/duration/material filters and text search are implemented. | Partial | The UI catalogue is still an in-memory `LocalActivityStore`; `SqliteDatabase.cacheActivity` is not its source. Verify offline filtering and criteria behavior. |
| FR2 | `ActivityCatalogueService`, `LocalActivityStore`, `SqliteDatabase` | Offline save persists selected IDs; SQLite schema can store activity content and children. | Partial | The UI is not backed by persisted SQLite activity content, and opening saved activities after relaunch is not verified. Server `SAVE_OFFLINE` records only selection metadata. |
| FR3 | `PlannerPanel`, `SessionPlanService`, `LocalSessionPlanStore`, thin-server `SAVE_PLAN` | Plan create/save and local persistence paths exist. | Partial | Client uses UUID activity IDs while server validator currently expects `ACT-###`; reconcile IDs and run end-to-end sync. |
| FR4 | Rich `SessionPlan`, `plan_*` tables, HTTP adapter | Model, local schema, and sync payload include steps/timing, materials, safety, and inclusion. | Implemented but not verified | Confirm server accepts the rich client’s actual activity IDs and inspect persisted child rows. |
| FR5 | `LocalOutboxRepository`, `SyncManager`, `HttpRemoteSessionPlanStore`, server `sync_log` | Local SQLite outbox, bounded batches, retries, per-item idempotency are implemented. | Implemented but not verified | Earlier failure/restart results are from the prototype client; retain fresh results from the selected rich-client path. |
| FR6 | Planner autosave timer, `LocalSessionPlanStore.drafts` | Debounced draft save and latest-draft restoration are implemented. | Implemented but not verified | Verify all fields survive close/relaunch and offline operation. |
| FR7 | Not selected | A4 scope defers kit custody. | Deferred | No claim of completion. |
| FR8 | Rich client planner | Duplication exists in the rich client. | Deferred | A4 baseline defers the feature; keep outside acceptance claims. |
| QR1 | Shared local SQLite, outbox, server idempotency | Local save precedes sync; server stores each response with operation. | Implemented but not verified | Run selected-client interruption/restart checks and retain outputs. |
| QR2 | `SyncManager`, HTTP adapter, SQLite | Client batches 10, server caps at 50; payloads contain text fields only. | Partial | No low-spec response measure/evidence and no warning on logout with unsynced work. |
| QR3 | Desktop Swing client; mobile client absent | Desktop workflow exists; no phone implementation is present. | Not implemented on phone | Implement and acceptance-test the equivalent phone workflow without downgrading QR3. |
| SUS1 | Server SQLite log; `docs/supportability.md` | Setup/recovery guide and server-side idempotency log exist. | Partial | No administrator-visible sync-failure log interface or documented admin inspection workflow. |
| AC1 | Rich desktop UI | Historic checklist exists. | Not verified | Complete WCAG 2.2 AA checks on the selected client, record issues and fixes; phone checks remain pending. |

Use these result labels consistently: implemented and verified, implemented but not verified, partial, not implemented, deferred. Existing `server/README.md` test claims concern the prototype path unless rerun against the selected rich client and attributed to its revision.

## Commit convention

Commit messages begin with an affected requirement ID: `FR1`–`FR8`, `QR1`–`QR3`, `SUS1`, or `AC1`.
