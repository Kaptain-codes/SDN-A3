# STEMMate A3: Technical Resilience and Low-Resource Design

## 1. Purpose

This section explains how the STEMMate desktop client behaves under Namibian low-resource conditions: intermittent internet, expensive data, shared low-specification devices and limited technical support. It describes what the prototype demonstrates, so each constraint is tied to visible behaviour rather than only mentioned.

## 2. Device and bandwidth assumptions

| Constraint                | Assumption                                                                   | Design response                                                                                                                  |
|---------------------------|------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| Intermittent internet     | A facilitator may lose signal mid-task, or work for hours or days with none. | Every save succeeds on the device first. The network is never required to save.                                                  |
| Low bandwidth             | Connections may be slow or unstable.                                         | Sync sends only changed records, in small batches, with no large assets.                                                         |
| Expensive data            | Users avoid anything that uses data without a clear need.                    | Activities are cached on the device once. Sync runs only when online and does not repeat completed work.                         |
| Shared devices            | Several facilitators may use one laptop.                                     | Role-based access, a short-lived cached session, and a warning at logout if unsynced items remain.                               |
| Low-specification devices | Older laptops with limited memory and storage.                               | A lightweight desktop client with a small local database (SQLite) and simple screens. \[Confirm any measured load-time target.\] |
| Limited technical support | Facilitators cannot call IT quickly.                                         | Plain-language status messages, a retry option, and a help screen explaining each status.                                        |
| Long-term maintenance     | The system must outlast the project team.                                    | Small documented stack, versioned releases and a sync-failure log (see section 8).                                               |

## 3. Requirements that drive the design

The team baseline contains FR1 to FR8. The prototype is built around two priority end-to-end workflows (FR1 and FR3/FR4) and one offline, failure and recovery workflow (FR5), described in section 4. FR2, FR6 and QR1 support these workflows; FR7 and FR8 are outside the prototype scope. Three further requirements are proposed so the low-resource constraints are traceable: two quality requirements (QR1, QR2), one sustainability requirement (SUS1) and one accessibility requirement (AC1).

| ID   | Requirement                                                                                                                                                | Priority | Acceptance indicator                                                                                                                                                                |
|------|------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| FR1  | Browse and filter activities by level, topic, duration and materials                                                                                       | Must     | Filters can be applied and produce relevant activities, including offline                                                                                                           |
| FR2  | Save suitable activities for offline use                                                                                                                   | Must     | A saved activity opens without an internet connection                                                                                                                               |
| FR3  | Create and save a session plan using a selected activity                                                                                                   | Must     | A session plan can be created and saved for a chosen activity                                                                                                                       |
| FR4  | Session plan contains steps, timing, materials, safety notes and inclusion prompts                                                                         | Must     | A saved plan displays all five elements                                                                                                                                             |
| FR5  | Plans completed offline are queued and synchronised when the connection returns                                                                            | Must     | A plan saved offline shows "waiting to sync", then "synced" after reconnecting, with no duplicates \[add time limit\]                                                               |
| FR6  | A plan in progress is saved automatically                                                                                                                  | Must     | A draft is restored with all fields after closing the app or losing connection                                                                                                      |
| FR7  | Equipment custodian records kit date, responsible person and return status                                                                                 | Should   | A kit record shows the date, responsible person and return status                                                                                                                   |
| FR8  | Duplicate a previous session plan as a starting point                                                                                                      | Could    | A new plan can be created from an existing plan                                                                                                                                     |
| QR1  | Work saved on the device is never deleted until the server confirms it                                                                                     | Must     | Connection is lost mid-save and the record is still present and later syncs                                                                                                         |
| QR2  | Sync uses small batches and no large assets, and the client is usable on low-specification shared devices, with a warning at logout if items are unsynced  | Must     | Sync completes on a throttled connection with status shown; the client responds acceptably on a low-spec device \[define measure\]; logging out with queued items shows the warning |
| SUS1 | Sync failures are recorded in an administrator-visible log, and setup and recovery documentation is provided so a local technician can maintain the system | Should   | An administrator can view recent sync failures, and a written setup and recovery guide exists                                                                                       |
| AC1  | The prototype meets the WCAG 2.2 AA checks listed in the brief                                                                                             | Must     | The accessibility checklist is completed and fixes are recorded                                                                                                                     |

## 4. Architecture overview

The system is a Java desktop client with a local database and a Ballerina server with a remote database. The client works fully on its own; the server is only contacted when the ConnectivityMonitor reports a connection. The client follows a repository pattern: screens call services (ActivityCatalogueService, SessionPlanService), which call repository interfaces (ActivityRepository, SessionPlanRepository). Each interface has a local SQLite store and a remote store that calls the Ballerina API, and the SyncManager moves queued records to the remote store when the connection returns, so the UI and services do not change based on connection state. Section 4.1 lists the components and contracts.

The three workflows form one story: find an activity, turn it into a session plan, save it offline, then synchronise it when the connection returns. Workflows 1 and 2 are the priority workflows; workflow 3 is the offline, failure and recovery workflow required by the brief.

| **Workflow**                       | **Role**                               | **Requirements**                  | **Journey**                                                                                                                                                                                                                    | **Main components**                                                                           |
|------------------------------------|----------------------------------------|-----------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| 1\. Find an activity               | Priority workflow                      | FR1 (supported by FR2)            | Open the cached activity list, filter by level, topic, duration and materials, select an activity and view its details. Works offline.                                                                                         | Activity screens, ActivityCatalogueService, ActivityRepository (local SQLite store)                                |
| 2\. Create and save a session plan | Priority workflow                      | FR3, FR4 (supported by FR6, QR1)  | Start from the selected activity and complete steps, timing, materials, safety notes and inclusion prompts. Autosave keeps the draft; Save writes it to the device and adds it to the outbox as "Waiting to sync".             | Plan builder, SessionPlanService, SessionPlanRepository (local SQLite store), outbox queue                                |
| 3\. Synchronise an offline plan    | Offline, failure and recovery workflow | FR5 (supported by QR1, QR2, SUS1) | The plan saved in workflow 2 stays "Waiting to sync" while offline. When the connection returns it syncs in a small batch and shows "Synced". If sync fails, "Failed, retry" appears and the user retries without losing work. | ConnectivityMonitor, SyncManager, SyncRecord outbox, remote SessionPlanRepository store (Ballerina API, simulated), sync-failure log |

FR7 (kit records) and FR8 (duplicate a plan) are deferred. The core workflows do not depend on them, and the prototype stays small enough to test and improve. The reasons and trade-off are recorded in section 6.

### 4.1 Components and repository contracts

The class names below come from the team's earlier design work and are re-tied to the agreed requirements (FR1 to FR8, QR1, QR2, SUS1, AC1). Sign-in and offline authentication are not in the agreed requirements, so there is no AuthService and no session argument on the contracts. Shared-device protection is the role-based access and logout warning already described in sections 2 and 5.

**Domain classes.** `Activity` (activityId, level, duration, topic, steps, materials, safetyNotes), `SessionPlan` (planId, activityId, steps, timing, materials, safetyNotes, inclusionPrompts, syncState), `Material`, `SafetyNote`, `InclusionPrompt` and `SyncRecord` (planId, state, lastAttempt). `EquipmentKit` is deferred with FR7. No class holds pupil-identifying data.

**Services.** Services depend on repository interfaces, never on concrete stores.

| Component | Responsibility | Requirements |
|---|---|---|
| ActivityCatalogueService | Filter activities by level, topic, duration and materials; return activity details; save an activity for offline use | FR1, FR2 |
| SessionPlanService | Create and save a plan from a selected activity, validate the five required elements, autosave and restore drafts | FR3, FR4, FR6 |
| SyncManager | Queue saved plans, attempt sync when online, mark records synced or failed, allow retry, write the sync-failure log | FR5, QR1, QR2, SUS1 |
| ConnectivityMonitor | Report online/offline (a toggle in the prototype); notifies the SyncManager on reconnect | FR5 |
| EquipmentKitService (deferred) | Record kit date, responsible person and return status | FR7 |

**Repository contracts.**

| Interface | Operations | Local store (SQLite) | Remote store (Ballerina API) |
|---|---|---|---|
| ActivityRepository | `findByCriteria(criteria)`, `findById(id)`, `saveForOffline(activity)`, `isAvailableOffline(id)` | LocalActivityStore | RemoteActivityStore |
| SessionPlanRepository | `save(plan)`, `findById(id)`, `findPending()`, `saveDraft(draft)`, `findDraft()` | LocalSessionPlanStore | RemoteSessionPlanStore |
| EquipmentKitRepository (deferred) | `findAvailability()`, `markReturned(kitId)` | LocalEquipmentKitStore | RemoteEquipmentKitStore |

**Contract rules for `SessionPlanRepository.save(plan)`.**
- *Preconditions:* the plan has all five required elements (steps, timing, materials, safety notes, inclusion prompts) and no pupil-identifying fields.
- *Postconditions:* the plan is written to the local store before any network call. Offline, its state becomes `WAITING_TO_SYNC` and it is added to the outbox. After the server confirms, the state becomes `SYNCED`.
- *Idempotency:* repeating `save()` with the same planId updates the same record and never creates a duplicate (FR5).
- *Errors:* `ValidationError` names the missing fields; `StorageFullError` offers retry; `NetworkUnavailableError` keeps the plan in the outbox.
- *Conflicts:* plans use last-write-wins by timestamp (section 5). Only kit records (FR7, deferred) flag conflicts.
- *Quality:* reading a saved plan from the local store should meet the load-time target set in QR2 \[define measure\].

**Requirement to component trace.**

| Requirement | Component / contract |
|---|---|
| FR1 | ActivityCatalogueService, `ActivityRepository.findByCriteria()` |
| FR2 | `ActivityRepository.saveForOffline()`, `isAvailableOffline()` |
| FR3, FR4 | SessionPlanService, `SessionPlanRepository.save()` |
| FR5 | SyncManager, SyncRecord, `SessionPlanRepository.findPending()` |
| FR6 | `SessionPlanRepository.saveDraft()` / `findDraft()` |
| QR1 | Local-first `save()`; record removed from the outbox only after server confirmation |
| QR2 | SyncManager small batches; lightweight client |
| SUS1 | SyncManager sync-failure log |

**Future improvement.** The ConnectivityMonitor could notify the SyncManager through an observer. The prototype calls it directly, which keeps the design simpler.

**Figure 1: Desktop architecture diagram.** Caption: Offline-first desktop architecture. The UI talks to ActivityCatalogueService (workflow 1, reads the cached activity list through ActivityRepository) and SessionPlanService (workflow 2, writes to the local SQLite store first through SessionPlanRepository). The SyncManager (workflow 3) reads the outbox queue and contacts the Ballerina API only when online (FR1, FR3, FR4, FR5, FR6, QR1).

**Figure 2: Offline create-then-sync sequence diagram.** Caption: Workflow 3, the offline, failure and recovery workflow. A plan created from a selected activity (workflows 1 and 2) is saved on the device while offline, shown as "Waiting to sync", and syncs on reconnect, with a retry path if sync fails (FR3, FR4, FR5, FR6, QR1).

## 5. How offline saving, queuing and sync work

1.  **Save locally first.** When the user saves, the record is written to the local database before any network call. If the network is down, nothing is lost (QR1, FR2).

2.  **Autosave drafts.** While a plan is edited, the draft is written locally at short intervals and on each field change. After a restart, the draft is restored with all fields and the user sees "Draft restored" (FR6).

3.  **Status on every record.** Each record has one of four statuses, shown on the record and in a global status bar.

| Status          | Meaning                                | Message shown to the user                                       |
|-----------------|----------------------------------------|-----------------------------------------------------------------|
| Waiting to sync | Saved on this device, not yet uploaded | "Saved on this device. It will upload when you're back online." |
| Syncing         | Upload in progress                     | "Syncing..."                                                    |
| Synced          | Server has confirmed the record        | "All changes synced."                                           |
| Failed, retry   | Upload failed; record is still safe    | "Couldn't sync. Your work is safe. Try again."                  |

4.  **Outbox queue.** Saving a completed plan in workflow 2 adds it to a first-in, first-out queue, which workflow 3 then processes. Each record has a unique ID and timestamp so a retry cannot create a duplicate (FR5).

5.  **Sync on reconnect.** When the ConnectivityMonitor reports a connection, the SyncManager sends pending records in small batches (QR2). A record is marked synced and removed from the queue only after the server confirms it (QR1). On failure it stays queued and the user can retry.

6.  **Conflict handling.** Plans have one owner, so last-write-wins by timestamp is used. Kit records (FR7) are different: if two offline edits to the same kit collide, the conflict is flagged for the custodian to resolve rather than overwritten.

7.  **Shared devices and privacy.** Records use synthetic or coded identifiers and contain no pupil-identifying data. Access is by role. Logging out with unsynced items shows a warning (QR2).

## 6. Key design decisions

| Decision        | Options considered                                                                                       | Chosen                                                                                                         | Trade-off                                                                                                                                                                      |
|-----------------|----------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Save behaviour  | Block until online; save locally then sync                                                               | Local first, then sync                                                                                         | Device and server can briefly differ, so status labels are needed. No lost work (FR2, FR6, QR1)                                                                                |
| Sync strategy   | Manual upload button; automatic outbox queue; full database vs changed records                           | Automatic outbox with retry and a manual retry fallback, sending changed records in small batches              | More logic to build and test, but users on shared devices do not need to remember to sync and data use stays low (FR5, QR2)                                                    |
| Conflict rule   | Last-write-wins; keep both versions; flag for a person                                                   | Plans: last-write-wins. Kits: flag for the custodian                                                           | Plans can rarely lose an edit; kits cannot risk double-booking (FR7)                                                                                                           |
| Backend         | Ballerina, Java, PHP                                                                                     | Ballerina with PostgreSQL on the server and SQLite on the client                                               | Smaller local support community, so the stack must be well documented. Using more than one database engine adds maintenance work (SUS1)                                        |
| Data access pattern | Active Record (persistence on Activity and SessionPlan); a thin UI with direct database calls; Repository pattern | Java desktop client using the Repository pattern, with a State pattern rejected and Observer left as future work | More interfaces and classes to maintain, but services can be tested with fake repositories and the UI does not care whether data is local or synced. Active Record was rejected because models would need offline/online branching (FR1, FR3, FR4, FR5, QR1) |
| Prototype scope | All of FR1 to FR8; FR1 + FR5 only; FR1 + FR3/FR4 as priority workflows with FR5 as the recovery workflow | FR1 and FR3/FR4 as priority workflows; FR5 as the offline, failure and recovery workflow; FR7 and FR8 deferred | Gives a complete find-plan-save-sync story that meets the brief. FR7 and FR8 are not demonstrated, and FR6 and QR1 are shown as supporting behaviour only (FR1, FR3, FR4, FR5) |

## 7. What is real and what is simulated

The prototype is a design milestone, so some resilience behaviour is simulated. This is stated plainly so the demonstration matches the claims.

| Behaviour                          | In the prototype                                                               | In the full system                                                       |
|------------------------------------|--------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| Offline and online switching       | \[e.g. toggle control that simulates losing and regaining connection\]         | ConnectivityMonitor detects real connection state                       |
| Autosave and draft restore (FR6)   | \[real local storage or simulated state\]                                      | Written to SQLite at short intervals                                     |
| Outbox queue and status (FR5)      | \[real or simulated\]                                                          | Persistent queue in SQLite                                               |
| Server sync                        | \[simulated response, including a failure case\]                               | Ballerina API and PostgreSQL                                             |
| Logout warning (QR2)               | \[real or simulated\]                                                          | Checks the outbox before ending the session                              |
| Activity cache and filtering (FR1) | \[e.g. synthetic activity list held in the prototype, filtered on screen\]     | Activities cached in SQLite after first download; filtering runs locally |
| Plan builder and save (FR3, FR4)   | \[real form with synthetic content; save adds the plan to a simulated outbox\] | Written to SQLite, then added to the outbox queue                        |

## 8. Maintenance and support in a low-resource deployment

- **Small, documented stack.** A limited number of components, each documented, so a local IT person can maintain the system. The Ballerina backend has a smaller community than mainstream options, so setup, deployment and recovery steps should be written down.

- **Minimise database engines.** Each additional engine (PostgreSQL on the server, SQLite on the client) is more to install, back up and patch. Any engine beyond the local SQLite store and one server database needs a clear reason.

- **Versioned releases.** The client shows its version number and updates are small, so they are cheap to download. Older clients keep working with the sync API, or are clearly told to update.

- **Diagnosable failures.** A sync-failure log (SUS1) records what failed and when. It is visible to an administrator and small enough to share by message, so problems can be diagnosed without a site visit.

- **User self-service.** A help screen explains each status. Failed syncs offer a retry, so facilitators can recover without support.

- **Backups and data safety.** The server database is backed up on a schedule. Local data is kept until the server confirms it, and the logout warning prevents leaving unsynced work on a shared device.

- **Sustainability.** Cached content and small sync payloads keep running costs low. Role-based access and no pupil-identifying data limit the effort needed to keep the system compliant.

## 9. Traceability: resilience and low-resource requirements

| Evidence                                            | Requirement | Prototype element                                                                                                     | Evaluation check                                                                                                |
|-----------------------------------------------------|-------------|-----------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| Intermittent connectivity                           | FR2         | "Available offline" activity cards; saved activities open offline                                                     | Participant opens a saved activity with offline mode on                                                         |
| Intermittent connectivity                           | FR1         | Filtering works on the cached activity list                                                                           | Participant filters activities while offline, selects one and starts a plan from it                             |
| Facilitator needs a usable session plan             | FR3, FR4    | Plan builder with steps, timing, materials, safety notes and inclusion prompts; Save creates a "Waiting to sync" plan | Participant selects an activity, completes the plan and saves it; all five elements are shown on the saved plan |
| Risk of lost work on shared or unstable devices     | FR6, QR1    | Autosave and "Draft restored" message                                                                                 | Participant closes the app mid-plan and reopens it; all fields restored                                         |
| Intermittent connectivity                           | FR5, QR1    | Offline banner, "waiting to sync" status, sync confirmation                                                           | Participant completes a plan offline, reconnects and sees it sync                                               |
| Failed or interrupted sync                          | FR5, QR1    | "Failed, retry" status with retry action                                                                              | Participant recovers from a failed sync without help                                                            |
| Data cost and weak connections                      | QR2         | Small-batch sync, no large assets, visible sync status                                                                | Sync completes on a throttled connection with status shown                                                      |
| Shared, low-specification devices                   | QR2         | Lightweight screens; logout warning with queued items                                                                 | Participant logs out with items waiting and sees the warning                                                    |
| Limited technical support and long-term maintenance | SUS1        | Sync-failure log and in-app status help screen                                                                        | Administrator finds a recent failure in the log; participant finds the meaning of a status on the help screen   |
| Kit double-booking risk                             | FR7         | Conflict flagged for the custodian \[if FR7 is in scope\]                                                             | Two offline kit edits collide and the conflict is shown                                                         |
| Accessibility of status feedback                    | AC1         | Status messages as text, not colour alone; errors with guidance                                                       | Accessibility checklist, error message and colour checks                                                        |

## 10. References

- W3C (2023). Web Content Accessibility Guidelines (WCAG) 2.2. https://www.w3.org/TR/WCAG22/ (W3C document licence).

- SQLite. SQLite documentation. https://www.sqlite.org/docs.html (public domain).

- PostgreSQL Global Development Group. PostgreSQL documentation. https://www.postgresql.org/docs/ (PostgreSQL Licence).

- Ballerina. Ballerina documentation. https://ballerina.io/learn/ (Apache License 2.0).