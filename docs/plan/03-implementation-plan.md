# Implementation plan

Every task is intended to be one small commit. Essential means required for the 2 October delivery.

## 1. Skeleton and database setup

- [x] **1.1** Goal: create Maven client and Ballerina/server/db folders. Files: `client/pom.xml`, server and db placeholders. Requirement IDs: FR1, FR3, FR5. Done: both project roots have a repeatable build entry point. Essential for 2 October: yes.
- [x] **1.2** Goal: load SQLite and PostgreSQL schemas. Files: `db/sqlite/schema.sql`, `db/postgres/schema.sql`. Requirement IDs: FR3, FR5, QR1. Done: schemas load in their target engines. Essential for 2 October: yes.

## 2. Contracts and workflow 1

- [x] **2.1** Goal: define domain records and sync status. Files: `client/src/main/java/org/stemmate/model/*`. Requirement IDs: FR1, FR3, FR4, FR5. Done: records compile and contain no identifying data. Essential for 2 October: yes.
- [x] **2.2** Goal: define repository interfaces and exception contracts. Files: `client/src/main/java/org/stemmate/repository/*`. Requirement IDs: FR1, FR2, FR3, FR5, FR6, QR1. Done: services can depend on interfaces only. Essential for 2 October: yes.
- [x] **2.3** Goal: implement the contract-and-schema baseline and API description for local-first plans. Files: Java contracts, both schemas, `docs/api/endpoints.md`, and `docs/plan/02-decisions.md`. Requirement IDs: FR1-FR6, QR1, QR2. Done: Java compiles, schemas load, and API rules document idempotency. Essential for 2 October: yes.
- [x] **2.4** Goal: filter the cached activity list offline. Files: activity service, local activity store, UI list/detail screens. Requirement IDs: FR1, FR2. Done: all four filters work with network disabled. Essential for 2 October: yes.

## 3. Workflow 2

- [x] **3.1** Goal: create and validate a session plan from an activity. Files: plan service and builder UI. Requirement IDs: FR3, FR4. Done: saved plan displays all five required elements. Essential for 2 October: yes.
- [x] **3.2** Goal: autosave and restore drafts. Files: draft store, plan service, builder UI. Requirement IDs: FR6, QR1. Done: closing and reopening restores every field. Essential for 2 October: yes.

## 4. Workflow 3

- [x] **4.1** Goal: add outbox persistence and local-first save. Files: local plan store and outbox repository adapter. Requirement IDs: FR5, QR1. Done: offline save is waiting to sync and is not deleted. Essential for 2 October: yes.
- [x] **4.2** Goal: implement connectivity toggle and small-batch sync with retry. Files: `ConnectivityMonitor`, `SyncManager`, status bar. Requirement IDs: FR5, QR1, QR2. Done: reconnect changes waiting records to synced or failed-retry without duplicates. Essential for 2 October: yes.

## 5. Accessibility

- [x] **5.1** Goal: apply WCAG 2.2 AA keyboard, focus, contrast, text, and error guidance checks. Files: UI screens and `docs/plan/05-accessibility.md`. Requirement IDs: AC1. Done: checklist evidence is recorded. Essential for 2 October: yes.

## 6. Supportability

- [x] **6.1** Goal: add sync-failure log and plain-language help screen. Files: sync log store, `HelpScreen`, support documentation. Requirement IDs: SUS1, QR2. Done: an administrator can inspect recent failures and users can interpret statuses. Essential for 2 October: yes.
