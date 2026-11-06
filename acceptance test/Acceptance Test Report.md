# STEMMate A4 Acceptance Test Run Report

**Run date:** 2026-11-06 18:18 UTC  
**Tester:** Codex with the user’s running local environment  
**Selected system:** Rich Java Swing client (`client/`) with thin SQLite server (`server/`)  
**Server URL:** `http://localhost:8080`  
**Client status:** Process/window exists as “STEMCraft - Facilitator Studio”, but it was minimized and the desktop accessibility interface could not inspect it.  
**Run type:** Focused acceptance run against the services currently running; no client data was created or changed.

## Summary

- **Pass:** Server health endpoint responds; activity catalogue endpoint responds and returns four seeded activities; empty plan-list request succeeds.
- **Blocked:** Client-driven acceptance scenarios could not be exercised because the client window was minimized and unavailable to the UI inspection interface. Phone acceptance is also blocked because the phone implementation is unfinished.
- **Not run:** Plan creation and editing, persistence/reopen, offline behavior, sync/retry, accessibility, and support-failure workflow. No result is inferred from source code or legacy Role 5 prototype behavior.
- **Known sync blocker:** Rich-client activity IDs and server validation formats differ (client UUID vs server `ACT-###`), as already recorded in the project run guide. End-to-end sync cannot be accepted until this is resolved and retested.

## Executed checks and evidence

| Check | Result | Observed evidence |
|---|---|---|
| Server health (`GET /health`) | **Pass** | HTTP 200; response body `status=OK`. |
| Activity catalogue (`GET /activities`) | **Pass** | HTTP 200; four rows returned: `ACT-101` Mini Wind Turbine Generator (Energy, Grade 8-10, 45 min); `ACT-102` Soil Micro-Ecosystem Analysis (Ecology, Grade 11-12, 60 min); `ACT-103` Autonomous Obstacle Avoidance Bot (Robotics, Grade 9-12, 60 min); `ACT-104` Hydraulic Lift Physics (Physics, Grade 5-7, 30 min). The response did not include steps or materials. |
| Plan listing (`GET /plans?deviceId=acceptance-report-probe`) | **Pass** | HTTP 200; empty response body for the probe device, consistent with no plans being listed for that device. This was read-only. |
| Rich client window inspection | **Blocked** | Window inventory showed “STEMCraft - Facilitator Studio” running, but it was minimized; accessibility inspection returned “window is minimized”. No controls could be verified. |

## Requirement results

| Requirement | Result | Run notes / evidence | Follow-up |
|---|---|---|---|
| FR1 — Activity filtering | **Not run** | Server returned four catalogue entries, but level/topic/duration/material filter controls and search were not exercised. | Restore the client window and execute AT-FR1, including offline catalogue filtering. |
| FR2 — Offline activity content | **Not run** | No activity was saved, reopened, or checked offline. Catalogue API payload observed here contains no steps/materials. | Exercise save, relaunch, and offline content workflow; verify full content persists locally. |
| FR3 — Create/save plan | **Not run** | No plan was created through the client; plan-list probe was empty. | Create a synthetic plan, relaunch, and verify saved-plan persistence. |
| FR4 — Five plan elements | **Not run** | No plan fields were entered or inspected in SQLite/server tables. | Verify steps, timing, materials, safety notes, and inclusion prompts across save/reopen. |
| FR5 — Offline queue and sync | **Blocked** | Client workflow could not be accessed. Existing documented UUID vs `ACT-###` validation mismatch prevents a reliable rich-client sync pass. | Align ID format, then run queue/restart/retry and server persistence checks. |
| FR6 — Draft autosave/restore | **Not run** | No draft was edited or reopened. | Exercise autosave and restoration online and offline. |
| FR7 — Kit custody | **Deferred** | Out of A4 scope. | Do not claim acceptance for A4. |
| FR8 — Duplicate plan | **Deferred** | Out of A4 scope. | Do not claim acceptance for A4. |
| QR1 — Preserve local work until confirmation | **Not run** | No plan was queued or client restarted during this run. | Run with controlled server outage, then restore service and inspect local/outbox/server state. |
| QR2 — Batch/recovery/performance/exit warning | **Not run** | No batch, weak-link, low-spec performance, or pending-work exit scenario was exercised. | Agree measurable performance threshold and run controlled failure scenarios. |
| QR3 — Laptop and phone equivalence | **Blocked** | Laptop client interaction could not be inspected; phone implementation is unfinished. | Restore client access for laptop run; keep phone result blocked until a phone client is implemented. |
| SUS1 — Failure log and recovery | **Not run** | No failure was induced; administrator-visible failure logging was not inspected. | Exercise an unavailable-server/invalid-item failure and verify admin visibility plus recovery guide. |
| AC1 — Accessibility | **Not run** | Keyboard order, focus visibility, names, contrast, status cues, resizing, and error states were not inspected. | Perform a dedicated keyboard and visual accessibility check on the selected client. |

## Limitations

This report records only the checks executed during this run. The API checks establish that the server is reachable and serving seeded catalogue rows; they do not establish that the client filters correctly or that the catalogue is complete. The plan-list request was read-only and used a probe device ID. No plan, draft, outbox item, or database row was written.

The run guide and checklist contain previously identified implementation gaps. Those are treated as follow-up context, not as tests performed during this run. Legacy prototype results are excluded from acceptance evidence.

## Re-run entry point

Restore/unminimize the rich client and rerun the scenarios in [A4 Acceptance Checklist.md](A4%20Acceptance%20Checklist.md). Save screenshots or database inspection output alongside this report and update each requirement result only after observing the corresponding behavior.

## Run 2 — 2026-11-06 18:21 UTC

**Client:** STEMCraft - Facilitator Studio, brought to the foreground and inspected.  
**Server:** `http://localhost:8080`.  
**Data safety:** Read-only inspection. No plan was created, no field was edited, and no outbox action was triggered.

### Fresh server checks

| Check | Result | Evidence |
|---|---|---|
| `GET /health` | **Pass** | HTTP 200, `status=OK`. |
| `GET /activities` | **Pass** | HTTP 200; four activity rows (response ends with a newline). |
| `GET /plans?deviceId=acceptance-report-probe` | **Pass** | HTTP 200; empty body for this probe ID. |

### Client observations and requirement outcomes

| Requirement | Run 2 result | Evidence / notes |
|---|---|---|
| FR1 — Find and filter activities | **Fail** | Level filter “High School” reduced the catalogue to two matching cards. Combining “Environmental” topic narrowed to the soil-analysis card. Materials “Water” showed only cards listing Water. Reset restored the four-card catalogue. However, “Under 60 mins” still displayed a 60-minute card, and no keyword search field was visible. |
| FR2 — Offline activity content | **Not run** | The Saved Offline view showed one existing card with a description and materials. No offline disconnection or relaunch was performed; full steps/safety content and durable reopening were not verified. |
| FR3 — Create/save plan | **Not run** | One pre-existing saved plan was visible. No new plan was created or reopened during this run. |
| FR4 — Five plan elements | **Not run** | The planner displayed three existing steps with timings 15, 25, and 20 minutes, plus materials, safety notes, and inclusion prompts. This confirms the current screen can display all five elements, but this run did not save/reopen the record or inspect database rows. |
| FR5 — Offline queue and synchronization | **Blocked** | The existing UI showed one queued item, “Sync failed. Your work is safe. Retry when online.” No retry was triggered. Successful sync remains blocked by the documented client UUID vs server `ACT-###` ID mismatch. |
| FR6 — Autosave and restore a draft | **Not run** | Planner currently showed “Saved,” but no changes, client restart, or draft restoration were tested. |
| FR7 — Kit custody | **Deferred** | Out of A4 scope. |
| FR8 — Duplicate plan | **Deferred** | Out of A4 scope. |
| QR1 — Preserve local work until confirmation | **Not run** | Existing queued item was visible, but no restart or recovery scenario was performed. |
| QR2 — Batch/recovery/performance/exit warning | **Not run** | No controlled outage, batch test, low-spec measurement, or exit-warning scenario was performed. |
| QR3 — Laptop and phone equivalence | **Blocked** | Client catalogue was inspected on the laptop only. Phone implementation remains unfinished. |
| SUS1 — Failure log and recovery | **Not run** | Existing sync failure banner was visible, but admin visibility and the support recovery procedure were not checked. |
| AC1 — Accessibility | **Not run** | No keyboard-only, focus, accessible-name, contrast, resize, or error-state checks were performed. |

### Current conclusion

The server is reachable and returns its seeded catalogue. The client’s visible level, topic, and materials filters and reset action behaved as expected in this run. Duration filtering has a reproducible visible defect at the “Under 60 mins” boundary, and catalogue keyword search was not present on the inspected screen. The current UI shows offline and plan-related data, but those observations do not establish persistence across restart or a successful sync. The earlier run results remain above for history; Run 2 is the latest observation.

## Debug follow-up — 2026-11-06

The FR1 findings from Run 2 were traced to two defects and fixed in the selected rich client:

1. **Duration boundary:** `LocalActivityStore` treated the maximum as inclusive (`duration <= maximum`), conflicting with the UI label “Under N mins.” It now uses a strict upper bound (`duration < maximum`). The rebuilt class bytecode was inspected and contains the strict comparison.
2. **Search field hidden:** `CatalogPanel` added both the title/search header and filter card to the same `BorderLayout.NORTH` region. Swing replaced the first component, leaving the search input (and catalogue heading) hidden. The two panels now sit in a single vertically stacked header container.

**Build verification:** `mvn -Dmaven.repo.local=C:\Users\wykva\.m2\repository -f client\pom.xml -DskipTests package` completed with **BUILD SUCCESS**. The client has no test sources under `client/src/test`, and this build skipped tests. The active user client was left untouched because it already contains an unsynced plan; a separate client launch used a temporary database and was stopped, but its window was not exposed for UI inspection. Therefore these fixes are compile-verified and the duration comparison is bytecode-verified, but **the post-fix visual acceptance check is still pending**. Restart the client from the rebuilt artifact and rerun AT-FR1 to confirm the boundary and that the search box is visible and filters results.

### Loop check — 2026-11-06 21:15 UTC

- Added `LocalActivityStoreTest.maximumDurationIsExclusive`, covering a 59-minute item and an item exactly at the 60-minute boundary.
- Client test run: **1 test, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**.
- Server Maven test phase: **BUILD SUCCESS**, with no server test sources to execute.
- Client package build: **BUILD SUCCESS**.
- Read-only server checks: `/health` returned HTTP 200 with `status=OK`; `/activities` returned HTTP 200 with four seeded rows.
- No other user files or the active unsynced plan were changed. Visual verification of the restored catalogue heading/search field remains pending until the rebuilt client is opened.

### Loop check — 2026-11-06 21:21 UTC

Added a second regression test for the catalogue header composition: `DesktopShellFrameLayoutTest.catalogueKeepsTitleSearchHeaderAndFiltersVisibleTogether` confirms the title/search header and filter card occupy separate visible regions in the same vertical container.

- Client test suite: **2 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**.
- Client package build: **BUILD SUCCESS**.
- Server test phase: **BUILD SUCCESS** (server module still has no test sources).
- Server checks: `/health` and `/activities` returned HTTP 200; the read-only plan probe returned HTTP 200 with an empty body.
- Repeat check: no new issue in touched-code whitespace checks. Full visual check still requires restarting the client with the rebuilt code; the active unsynced plan was not touched.
