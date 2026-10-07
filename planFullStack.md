# Implementation Plan: Remote Session-Plan Sync Pipeline

**Date**: 2026-10-07 | **Spec**: `docs/design/Requirments.md`, `docs/design/STEMMate_A3_Technical_Resilience_and_Low-Resource_Design_v2.md`

## Summary

Complete the end-to-end sync path for created session plans. The Java desktop client will keep its current local-first, idempotent outbox behavior, but its `RemoteSessionPlanStore` implementation will call a real Ballerina HTTP API instead of the in-memory `LocalRemoteSessionPlanStore`. The Ballerina backend will expose a deliberately small `POST /plans` endpoint that validates a bounded batch, upserts plans and steps into PostgreSQL by `planId`, applies last-write-wins using `updatedAt`, and returns per-plan confirmations. A health endpoint and focused integration tests will make the pipeline observable without adding authentication, pupil data, or unrelated workflows.

## Technical Context

**Language/Version**: Java 17 desktop client; Ballerina backend; PostgreSQL remote database; SQLite/local serialized store currently used by the prototype.

**Primary Dependencies**:
- Existing Java repository contracts: `RemoteSessionPlanStore`, `SyncManager`, `SessionPlan`.
- Ballerina HTTP service and PostgreSQL client.
- Existing PostgreSQL schema in `db/postgres/schema.sql`.

**Storage**:
- Local client state remains local-first: plans, outbox state, retry metadata, and sync logs are written before network access.
- PostgreSQL stores `session_plans` and `plan_steps`; `plan_id` is the idempotency key.
- No pupil-identifying fields, authentication, session parameters, or additional database engines.

**Testing**:
- Java unit tests for JSON mapping, HTTP error translation, idempotent retry behavior, and batch limits.
- Ballerina tests or an executable local integration test against PostgreSQL for validation, upsert/last-write-wins, and cascade replacement of plan steps.
- End-to-end sync test: save offline, reconnect, upload, confirm `SYNCED`, and verify one remote row for repeated uploads.

**Target Platform**: Low-resource desktop client connecting to a small Ballerina service and PostgreSQL instance; local development must remain runnable without the remote service by using the existing fake store in tests only.

**Performance Goals**:
- Preserve `SyncManager.MAX_BATCH_SIZE == 20`.
- Send only pending records and remove/mark them only after server confirmation.
- Keep the API response small and bounded to accepted plan IDs plus receipt timestamps.

**Constraints**:
- Services depend on repository interfaces, never concrete stores.
- `save()` remains local-first and idempotent by UUID `planId`.
- Plans use last-write-wins by `updatedAt`.
- The prototype scope is workflows 2 and 3: create/save, autosave/restore, offline queue, reconnect sync, retry, and failure logging.
- FR7 and FR8 remain deferred; no sign-in or `AuthService` may be introduced.

## Constitution Check

*GATE: Pass.*

- **Repository boundary**: The client service and `SyncManager` continue to depend only on interfaces; HTTP details remain in a `RemoteSessionPlanStore` adapter.
- **Local-first resilience**: Network calls occur only after the local plan and outbox state are persisted.
- **Idempotency and recovery**: The server upserts by `planId`; retries are safe and failed records remain retryable.
- **Low-resource operation**: Batches remain capped at 20, payloads contain only required synthetic plan data, and no heavyweight infrastructure is added.
- **Privacy and scope**: Payload and schema contain no pupil-identifying data, authentication, or deferred kit/duplication behavior.
- **Diagnosability**: HTTP failures map to existing retry states and are recorded through the existing sync log boundary.

## Applied Guidelines

No matching migration guideline exists under `skills/guidelines/` for Ballerina, PostgreSQL, or this Java repository-pattern sync implementation. The plan follows the project’s source-of-truth contracts and existing low-resource design decisions instead.

## Implementation Steps

### Step 1: [Cross-cutting] Freeze the wire contract and environment configuration
- **Requirements**: FR3, FR4, FR5, FR6, QR1, QR2, SUS1
- **Design inputs**: Section 4.1 repository contracts, `docs/api/endpoints.md`, PostgreSQL schema, Section 5 sync rules.
- **Description**: Define the exact JSON representation for a batch of `SessionPlan` records, including nested steps, timing, materials, safety notes, inclusion prompts, `planId`, `activityId`, `createdAt`, and `updatedAt`. Document `POST /plans`, `GET /sync/health`, maximum batch size 20, response semantics, HTTP status/error mapping, and last-write-wins behavior. Add a configurable base URL with a safe local default; do not hard-code credentials or add authentication.

### Step 2: [Cross-cutting] Create the minimal Ballerina PostgreSQL service
- **Requirements**: FR3, FR4, FR5, QR1
- **Design inputs**: `db/postgres/schema.sql`, `docs/api/endpoints.md`, Section 6 backend decision.
- **Description**: Add a small `server/` Ballerina project with `Ballerina.toml`, configuration placeholders, an HTTP listener, and a plans module. Implement `POST /plans` to reject malformed or oversized batches, validate all five required plan elements, upsert `session_plans` by UUID, compare `updatedAt` for last-write-wins, and replace/upsert the associated `plan_steps` transactionally. Return one confirmation per accepted plan with server receipt time. Implement `GET /sync/health` with a lightweight database/connectivity check. Use environment/configuration for PostgreSQL connection details and never commit secrets.

### Step 3: [Cross-cutting] Replace the fake Java remote adapter with an HTTP adapter
- **Requirements**: FR5, QR1, QR2, SUS1
- **Design inputs**: `RemoteSessionPlanStore`, `SyncManager`, `SessionPlan`, `docs/api/endpoints.md`.
- **Description**: Implement an HTTP-backed `RemoteSessionPlanStore` under `client/src/main/java/org/stemmate/repository/` using the JDK HTTP client or an existing project dependency. Serialize the same batch contract, enforce the 20-record limit, parse only confirmed plan IDs, and translate timeouts, connection failures, non-2xx responses, malformed responses, and partial confirmations into `NetworkUnavailableException` or an explicit repository exception consistent with retry behavior. Keep `LocalRemoteSessionPlanStore` available for deterministic unit tests, but do not use it in the production application bootstrap.

### Step 4: [Cross-cutting] Wire production configuration and sync lifecycle
- **Requirements**: FR5, FR6, QR1, QR2, SUS1
- **Design inputs**: `client/src/main/java/org/stemmate/App.java`, `SyncManager`, local outbox and sync-log stores.
- **Description**: Update application composition to construct the HTTP remote adapter from configuration, retain the existing `ConnectivityMonitor` reconnect trigger and manual retry action, and preserve status transitions (`WAITING_TO_SYNC`, `SYNCING`, `SYNCED`, `FAILED_RETRY`). Ensure a confirmed plan is marked synced only after the server response; missing confirmations remain retryable and are logged. Keep startup usable when the endpoint is unavailable by leaving queued plans intact rather than failing application launch.

### Step 5: [Cross-cutting] Validate persistence, retries, and end-to-end behavior
- **Requirements**: FR3, FR4, FR5, FR6, QR1, QR2, SUS1, AC1
- **Design inputs**: existing local stores, `SyncManager`, Ballerina service, PostgreSQL schema, existing supportability documentation.
- **Description**: Add focused tests and a repeatable local validation procedure. Cover complete-plan validation, offline save and draft recovery, FIFO bounded batches, reconnect sync, retry after a network/database failure, idempotent duplicate upload, stale `updatedAt` rejection, partial confirmation handling, sync-log messages, and API health. Verify that the existing UI exposes truthful status text and that no identifying data is sent. Update `docs/api/endpoints.md`, `docs/supportability.md`, and build/run instructions with configuration, migration, startup, failure recovery, and cleanup steps.

### Step 6: [Cross-cutting] Preserve deferred scope and remove prototype-only production wiring
- **Requirements**: FR1, FR2, FR7, FR8
- **Design inputs**: project instructions and prototype scope.
- **Description**: Confirm that activity browsing/offline caching remain unchanged, while equipment-kit tracking and plan duplication remain explicitly deferred. Remove only the production use of the in-memory fake remote adapter; retain test doubles where needed. Review the final diff for forbidden sign-in/auth/session-parameter additions and for accidental pupil-identifying fields.

## Project Structure

```text
plan.md
checkpoints/
  spec-to-plan.yaml
  plan-to-tasks.yaml
client/
  src/main/java/org/stemmate/repository/
    HttpRemoteSessionPlanStore.java
    RemoteSessionPlanStore.java
  src/main/java/org/stemmate/
    App.java
  src/test/java/org/stemmate/
    repository/HttpRemoteSessionPlanStoreTest.java
    sync/SyncManagerIntegrationTest.java
server/
  Ballerina.toml
  Config.toml.example
  service.bal
  modules/plans/
    model.bal
    repository.bal
    service.bal
    tests.bal
db/
  postgres/schema.sql
docs/
  api/endpoints.md
  supportability.md
  ai-use-log.md
```

## Task Breakdown

### Phase 1: Setup
- [ ] T001 [P] [Plan:1.1] Define the session-plan batch JSON schema, accepted response shape, HTTP status mapping, and environment variables in `docs/api/endpoints.md`.
- [ ] T002 [P] [Plan:1.1] Add non-secret local configuration examples for the remote base URL and PostgreSQL connection in `server/Config.toml.example` and the client configuration location selected during implementation.
- [ ] T003 [P] [Plan:2.1] Create the Ballerina project manifest and module layout in `server/Ballerina.toml`, `server/service.bal`, and `server/modules/plans/`.

### Phase 2: Foundational
- [ ] T004 [Plan:2.1] Implement Ballerina request/response records and validation helpers in `server/modules/plans/model.bal` for all required session-plan fields and the 20-plan batch limit.
- [ ] T005 [Plan:2.1] Add PostgreSQL repository operations in `server/modules/plans/repository.bal` for transactional upsert, `updatedAt` last-write-wins checks, and plan-step persistence.
- [ ] T006 [Plan:2.1] Implement `POST /plans` and `GET /sync/health` in `server/modules/plans/service.bal` and `server/service.bal`, including explicit error responses and server receipt timestamps.
- [ ] T007 [Plan:3.1] Add the Java HTTP remote adapter in `client/src/main/java/org/stemmate/repository/HttpRemoteSessionPlanStore.java` with bounded batches, JSON mapping, timeouts, and repository-level failure translation.
- [ ] T008 [Plan:4.1] Update `client/src/main/java/org/stemmate/App.java` to configure the HTTP adapter in production while retaining `LocalRemoteSessionPlanStore` only for tests.

### Phase 3: Sync Pipeline Validation
- [ ] T009 [P] [Plan:3.1] Test request serialization, successful confirmation parsing, batch-size rejection, malformed responses, and network/non-2xx error translation in `client/src/test/java/org/stemmate/repository/HttpRemoteSessionPlanStoreTest.java`.
- [ ] T010 [P] [Plan:2.1] Test Ballerina validation, health behavior, first insert, repeated insert by the same `planId`, stale update rejection, and step persistence in `server/modules/plans/tests.bal`.
- [ ] T011 [Plan:4.1,5.1] Add an end-to-end sync test covering offline local save, reconnect-triggered upload, `SYNCED` confirmation, failed retry preservation, partial confirmation, and sync-log output in `client/src/test/java/org/stemmate/sync/SyncManagerIntegrationTest.java`.
- [ ] T012 [Plan:5.1] Verify PostgreSQL schema and Ballerina service startup with a disposable/local PostgreSQL database, then record the exact commands and required environment variables in `docs/supportability.md`.

### Phase 4: Polish and Scope Guard
- [ ] T013 [Plan:5.1,6.1] Update `docs/api/endpoints.md` and `docs/supportability.md` with recovery guidance for unavailable service, rejected payloads, stale updates, and retryable outbox records.
- [ ] T014 [Plan:4.1,5.1,6.1] Exercise the existing desktop status bar and saved-plan views to confirm status text remains truthful and accessible for waiting, syncing, synced, and failed-retry states.
- [ ] T015 [Plan:6.1] Review the final implementation against the deferred FR7/FR8 scope and privacy rules; confirm no authentication, session parameters, or pupil-identifying fields were introduced.

## Requirement Mapping

| REQ ID | Description | Plan Items | Implementation Evidence |
|--------|-------------|------------|--------------------------|
| FR1 | Browse/filter activities | P6.1 | Existing activity catalogue and screens remain unchanged |
| FR2 | Save activities for offline use | P6.1 | Existing local activity store remains unchanged |
| FR3 | Create and save a session plan | P1.1, P2.1, P3.1 | Ballerina plan model/service; `HttpRemoteSessionPlanStore`; existing `SessionPlanService` |
| FR4 | Preserve steps, timing, materials, safety notes, inclusion prompts | P1.1, P2.1, P3.1 | JSON contract, Ballerina validation, PostgreSQL `session_plans`/`plan_steps` |
| FR5 | Queue and synchronise completed offline plans | P3.1, P4.1, P5.1 | HTTP remote adapter, `SyncManager`, reconnect and retry tests |
| FR6 | Autosave and restore in-progress plans | P4.1, P5.1 | Existing draft store/service retained; end-to-end regression coverage |
| FR7 | Record equipment-kit custody data | P6.1 | Explicitly deferred; no implementation in this plan |
| FR8 | Duplicate a previous session plan | P6.1 | Explicitly deferred; no implementation in this plan |
| QR1 | Never lose local work; idempotent sync | P2.1, P3.1, P4.1, P5.1 | Transactional upsert, `planId` key, outbox preservation, duplicate-upload tests |
| QR2 | Low-resource, bounded and understandable operation | P1.1, P4.1, P5.1 | Batch limit 20, small responses, status UI, supportability guidance |
| SUS1 | Diagnosable sync failures | P3.1, P4.1, P5.1 | Explicit HTTP error mapping, sync-log entries, recovery documentation |
| AC1 | Accessible, usable interaction | P5.1 | Existing status bar and saved-plan UI regression checks |

## Testing Strategy

- **appType**: Mixed desktop client plus HTTP service and PostgreSQL integration.
- **Critical user journeys**:
  1. Create a complete session plan from an activity and save it while offline.
  2. Close/reopen the client and restore the draft without losing fields.
  3. Reconnect and upload a waiting plan in a bounded batch.
  4. Retry after a service/database failure without duplicate remote rows.
  5. Inspect a truthful, plain-language sync status and failure log.
- **primaryValidationStack**: Maven/JUnit for Java; Ballerina test runner; PostgreSQL integration database; HTTP-level end-to-end test.
- **fallbackMatrix**:
  - `infra-tier`: disposable PostgreSQL → local configured PostgreSQL instance → repository fake for Java-only tests. The fake cannot prove SQL behavior or last-write-wins.
  - `browser-tier`: not applicable; Swing UI checks are manual/component-level because this is not a browser application.
- **Environment requirements**: Java 17, Maven, Ballerina CLI, PostgreSQL, and a configured test database; no credentials committed to the repository.
- **knownGaps**: Repository-only tests do not validate PostgreSQL transactions; fake remote tests do not validate Ballerina serialization; manual Swing checks do not replace accessibility evaluation.
- **Test data strategy**: Synthetic activities and UUID plan IDs per test; isolate by unique plan IDs; clean inserted PostgreSQL rows after each integration test or use a disposable schema.
- **Acceptance criteria**: A saved offline plan survives local restart, reconnect uploads it once, repeated upload does not duplicate it, stale updates do not overwrite newer data, failures remain retryable and diagnosable, and the client still starts without the service.
- **Validation review expectations**: Confirm requirement mapping, repository boundaries, no forbidden data/auth fields, bounded payloads, transactionality, and truthful status transitions.

