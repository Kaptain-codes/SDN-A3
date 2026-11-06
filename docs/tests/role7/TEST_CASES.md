# Documented Java test cases

Run: 06 November 2026 at 21:43 CAT. Precondition for each automated case: current source, JDK 21+, synthetic data, a fresh isolated user.home and the included runner. Test IDs J/S/P identify cases; they do not introduce new requirement IDs.



## J01 Combined catalogue filters

Requirements: FR1. Component: LocalActivityStore.findByCriteria. Type: Repository integration.

Steps: Filter by level, topic, duration and case-insensitive material name.

Expected: Only the matching cached activity is returned.

Result: PASS. Evidence: docs/tests/role7/evidence/client/J01.txt.

Observed output:

```text
Observed: matching=1; 29-minute limit excludes 30-minute activity
PASS J01
```



## J02 Offline activity selection persists

Requirements: FR2. Component: LocalActivityStore. Type: Real file storage integration.

Steps: Save the same activity twice; reconstruct the repository with the cached catalogue.

Expected: The saved-offline mark survives reload without duplicate entries.

Result: PASS. Evidence: docs/tests/role7/evidence/client/J02.txt.

Observed output:

```text
Observed: offline mark restored from activity-offline.ser
PASS J02
```



## J03 Draft is created from the selected activity

Requirements: FR3, FR4. Component: SessionPlanService.createDraftFromActivity. Type: Unit.

Steps: Create a new draft from a cached activity.

Expected: Activity ID, steps, timing, materials and safety are copied; editable title starts blank.

Result: PASS. Evidence: docs/tests/role7/evidence/client/J03.txt.

Observed output:

```text
Observed: draft activity=10000000-0000-0000-0000-000000000001; steps=1
PASS J03
```



## J04 A valid plan is saved and queued

Requirements: FR3, FR4, FR5. Component: SessionPlanService and LocalSessionPlanStore. Type: Service and real file integration.

Steps: Save a complete synthetic plan through the service.

Expected: One complete record and one WAITING_TO_SYNC outbox entry are stored.

Result: PASS. Evidence: docs/tests/role7/evidence/client/J04.txt.

Observed output:

```text
Observed: plan and outbox record saved; state=WAITING_TO_SYNC
PASS J04
```



## J05 Blank title is rejected

Requirements: FR3, FR4. Component: SessionPlanService.validate. Type: Validation unit.

Steps: Replace the plan title with whitespace and try to save.

Expected: ValidationException is raised and no plan is stored.

Result: PASS. Evidence: docs/tests/role7/evidence/client/J05.txt.

Observed output:

```text
Observed: blank title rejected before persistence
PASS J05
```



## J06 Missing required plan elements are rejected

Requirements: FR4. Component: SessionPlanService.validate. Type: Validation unit.

Steps: Individually empty steps, timing, materials, safety and inclusion.

Expected: Each incomplete plan is rejected.

Result: PASS. Evidence: docs/tests/role7/evidence/client/J06.txt.

Observed output:

```text
Observed: five missing-element variants rejected
PASS J06
```



## J07 Negative and mismatched timing are rejected

Requirements: FR4. Component: SessionPlanService.validate. Type: Validation unit.

Steps: Provide negative timing, then two timings for one step.

Expected: Both invalid timing variants are rejected.

Result: PASS. Evidence: docs/tests/role7/evidence/client/J07.txt.

Observed output:

```text
Observed: negative and mismatched timings rejected
PASS J07
```



## J08 Incomplete draft can be autosaved and restored

Requirements: FR6. Component: SessionPlanService.autosaveDraft and LocalSessionPlanStore. Type: Real file storage integration.

Steps: Autosave a draft before its title and target group are completed; reconstruct storage.

Expected: The incomplete draft is restored unchanged.

Result: PASS. Evidence: docs/tests/role7/evidence/client/J08.txt.

Observed output:

```text
Observed: empty-title draft restored from drafts.ser
PASS J08
```



## J09 Plan draft and outbox survive a real JVM restart

Requirements: FR2, FR3, FR5, FR6, QR1. Component: LocalSessionPlanStore, LocalOutboxRepository and LocalActivityStore. Type: Real separate-process restart integration.

Steps: A writer JVM saves data and exits. A new reader JVM uses the same isolated user.home.

Expected: The reader verifies the complete plan, draft, offline mark and pending outbox.

Result: PASS. Evidence: docs/tests/role7/evidence/client/J09.txt.

Observed output:

```text
Observed: write process: wrote complete plan, draft, outbox and offline mark
Observed: read process: verified exact complete plan, draft, pending outbox and offline mark in a new JVM
PASS J09
```



## J10 Saving the same plan ID is idempotent locally

Requirements: FR3, FR5. Component: LocalSessionPlanStore.save. Type: Real file storage integration.

Steps: Save the same plan twice.

Expected: There is one plan and one pending outbox record.

Result: PASS. Evidence: docs/tests/role7/evidence/client/J10.txt.

Observed output:

```text
Observed: plans=1; outbox=1
PASS J10
```



## J11 Offline state prevents remote calls

Requirements: FR5, QR1. Component: SyncManager and ConnectivityMonitor. Type: Sync integration with remote double.

Steps: Queue a plan while offline and call syncPending.

Expected: The remote is not called; the plan remains pending.

Result: PASS. Evidence: docs/tests/role7/evidence/client/J11.txt.

Observed output:

```text
Observed: remote calls=0; pending=1
PASS J11
```



## J12 Reconnect synchronises a queued plan

Requirements: FR5. Component: ConnectivityMonitor and SyncManager. Type: Sync integration with remote double.

Steps: Queue offline, register sync manager, then change connectivity to online.

Expected: Exactly one remote upload occurs; the confirmed plan becomes SYNCED.

Result: PASS. Evidence: docs/tests/role7/evidence/client/J12.txt.

Observed output:

```text
Observed: reconnect calls=1; state=SYNCED
PASS J12
```



## J13 Failure retains data logs the error and permits retry

Requirements: FR5, QR1, SUS1. Component: SyncManager and LocalSyncLogStore. Type: Failure injection and real file integration.

Steps: Inject NetworkUnavailableException, inspect stored state/log, then retry successfully.

Expected: Failure is logged and retained; retry confirms without a duplicate.

Result: PASS. Evidence: docs/tests/role7/evidence/client/J13.txt.

Observed output:

```text
Observed: FAILED_RETRY persisted; one failure log; successful retry; plans=1
PASS J13
```



## J14 Only acknowledged plans are confirmed

Requirements: FR5, QR1. Component: SyncManager. Type: Sync integration with partial acknowledgement double.

Steps: Queue two plans; remote double confirms only the first.

Expected: First is SYNCED; second is FAILED_RETRY and remains available.

Result: PASS. Evidence: docs/tests/role7/evidence/client/J14.txt.

Observed output:

```text
Observed: acknowledged=SYNCED; unconfirmed=FAILED_RETRY
PASS J14
```



## J15 Sync batches are bounded at twenty

Requirements: QR2, FR5. Component: SyncManager.MAX_BATCH_SIZE. Type: Batch integration with remote double.

Steps: Queue 45 plans and record each remote-double batch size.

Expected: Batch sizes are 20, 20 and 5; all 45 records remain stored.

Result: PASS. Evidence: docs/tests/role7/evidence/client/J15.txt.

Observed output:

```text
Observed: batch sizes=[20, 20, 5]; saved plans=45
PASS J15
```



## J16 HTTP payload quotes scalar JSON strings

Requirements: FR4, FR5. Component: HttpRemoteSessionPlanStore.planJson. Type: HTTP adapter contract with transport double.

Steps: Upload through a capturing HttpClient double and inspect the actual request body.

Expected: UUID and title fields are quoted JSON strings.

Result: FAIL. Evidence: docs/tests/role7/evidence/client/J16.txt.

Observed output:

```text
Observed: request body={"plans":[{"planId":20000000-0000-0000-0000-000000000001,"activityId":10000000-0000-0000-0000-000000000001,"title":Synthetic water plan,"targetGroup":Grade 7,"createdAt":2026-11-06T19:00:00Z,"updatedAt":2026-11-06T19:00:00Z,"steps":[{"instruction":"Build filter","durationMinutes":30,"description":"Compare layers"}],"timingMinutes":[30],"materials":[{"name":"Bottle","quantity":"1"}],"safetyNotes":["Do not drink the water"],"inclusionPrompts":["Picture instructions"]}]}
java.lang.AssertionError: planId is not a quoted JSON string
	at org.stemmate.testing.TestHarness.require(TestHarness.java:16)
	at org.stemmate.testing.Role7ClientTests.lambda$cases$24(Role7ClientTests.java:75)
	at org.stemmate.testing.TestHarness.run(TestHarness.java:32)
	at org.stemmate.testing.Role7ClientTests.main(Role7ClientTests.java:96)
```



## J17 Valid acknowledgement IDs are parsed

Requirements: FR5. Component: HttpRemoteSessionPlanStore.parseConfirmation. Type: HTTP adapter with transport double.

Steps: Return a successful response with one accepted plan ID.

Expected: The adapter returns that ID.

Result: PASS. Evidence: docs/tests/role7/evidence/client/J17.txt.

Observed output:

```text
Observed: accepted ID returned; real HTTP not used
PASS J17
```



## J18 Rejected acknowledgement IDs must not be accepted

Requirements: FR5, QR1. Component: HttpRemoteSessionPlanStore.parseConfirmation. Type: HTTP adapter negative contract with transport double.

Steps: Return accepted=[] and put the sent ID only in rejected.

Expected: The rejected plan ID is never returned as confirmed.

Result: FAIL. Evidence: docs/tests/role7/evidence/client/J18.txt.

Observed output:

```text
Observed: confirmed=[20000000-0000-0000-0000-000000000001]
java.lang.AssertionError: A rejected plan was returned as confirmed
	at org.stemmate.testing.TestHarness.require(TestHarness.java:16)
	at org.stemmate.testing.Role7ClientTests.lambda$cases$26(Role7ClientTests.java:79)
	at org.stemmate.testing.TestHarness.run(TestHarness.java:32)
	at org.stemmate.testing.Role7ClientTests.main(Role7ClientTests.java:96)
```



## J19 HTTP failure becomes a retryable network error

Requirements: FR5, QR1. Component: HttpRemoteSessionPlanStore.uploadBatch. Type: HTTP adapter failure injection.

Steps: Return HTTP 503 from the transport double.

Expected: NetworkUnavailableException is raised.

Result: PASS. Evidence: docs/tests/role7/evidence/client/J19.txt.

Observed output:

```text
Observed: HTTP 503 rejected as a network failure
PASS J19
```



## J20 Oversized HTTP batch is rejected before send

Requirements: QR2. Component: HttpRemoteSessionPlanStore.uploadBatch. Type: Boundary unit.

Steps: Try to upload 21 plans in one adapter call.

Expected: IllegalArgumentException is raised and send is not called.

Result: PASS. Evidence: docs/tests/role7/evidence/client/J20.txt.

Observed output:

```text
Observed: batch of 21 rejected; transport calls=0
PASS J20
```



## J21 Failed plan write must not leave an orphan outbox

Requirements: FR3, QR1. Component: LocalSessionPlanStore.save. Type: Real filesystem failure injection.

Steps: Make plans.ser a directory so the plan write fails, then reload the outbox.

Expected: The failed save does not leave a durable queue entry without a durable plan.

Result: FAIL. Evidence: docs/tests/role7/evidence/client/J21.txt.

Observed output:

```text
Observed: durable pending outbox=1; durable plan file is a directory
java.lang.AssertionError: Orphan durable outbox after failed save; expected=0; actual=1
	at org.stemmate.testing.TestHarness.require(TestHarness.java:16)
	at org.stemmate.testing.TestHarness.equal(TestHarness.java:18)
	at org.stemmate.testing.Role7ClientTests.lambda$cases$32(Role7ClientTests.java:83)
	at org.stemmate.testing.TestHarness.run(TestHarness.java:32)
	at org.stemmate.testing.Role7ClientTests.main(Role7ClientTests.java:96)
```



## J22 Wrong stored object type is handled on load

Requirements: FR6, QR1. Component: FileStoreSupport.read. Type: Real stored-data failure injection.

Steps: Write a serialised String where plans.ser expects a map, then open the repository.

Expected: No uncaught ClassCastException escapes; a diagnosable recovery path is available.

Result: FAIL. Evidence: docs/tests/role7/evidence/client/J22.txt.

Observed output:

```text
Observed: java.lang.ClassCastException: Cannot cast java.lang.String to java.util.LinkedHashMap
java.lang.AssertionError: Unexpected stored object type crashes startup
	at org.stemmate.testing.Role7ClientTests.lambda$cases$33(Role7ClientTests.java:85)
	at org.stemmate.testing.TestHarness.run(TestHarness.java:32)
	at org.stemmate.testing.Role7ClientTests.main(Role7ClientTests.java:96)
Caused by: java.lang.ClassCastException: Cannot cast java.lang.String to java.util.LinkedHashMap
	at java.base/java.lang.Class.cast(Class.java:4069)
	at org.stemmate.store.FileStoreSupport.read(FileStoreSupport.java:20)
	at org.stemmate.store.LocalSessionPlanStore.load(LocalSessionPlanStore.java:143)
	at org.stemmate.store.LocalSessionPlanStore.<init>(LocalSessionPlanStore.java:38)
	at org.stemmate.store.LocalSessionPlanStore.<init>(LocalSessionPlanStore.java:33)
	... 3 more
```



## J23 Whitespace safety content is rejected

Requirements: FR4. Component: SessionPlanService.validate. Type: Validation negative unit.

Steps: Provide a nonempty safety-note list whose text is only spaces.

Expected: ValidationException is raised for empty safety content.

Result: FAIL. Evidence: docs/tests/role7/evidence/client/J23.txt.

Observed output:

```text
java.lang.AssertionError: Expected ValidationException but no exception was raised
	at org.stemmate.testing.TestHarness.expect(TestHarness.java:25)
	at org.stemmate.testing.Role7ClientTests.lambda$cases$35(Role7ClientTests.java:87)
	at org.stemmate.testing.TestHarness.run(TestHarness.java:32)
	at org.stemmate.testing.Role7ClientTests.main(Role7ClientTests.java:96)
```



## J24 Interrupted SYNCING record remains retryable after reload

Requirements: FR5, QR1. Component: LocalOutboxRepository.findPending. Type: Persistent interrupted-state recovery.

Steps: Persist SYNCING to represent interruption after upload begins; reload the outbox.

Expected: The interrupted record is eligible for retry rather than stuck outside the pending queue.

Result: FAIL. Evidence: docs/tests/role7/evidence/client/J24.txt.

Observed output:

```text
Observed: stored state=SYNCING; retry queue size=0
java.lang.AssertionError: Interrupted item must be retryable; expected=1; actual=0
	at org.stemmate.testing.TestHarness.require(TestHarness.java:16)
	at org.stemmate.testing.TestHarness.equal(TestHarness.java:18)
	at org.stemmate.testing.Role7ClientTests.lambda$cases$36(Role7ClientTests.java:89)
	at org.stemmate.testing.TestHarness.run(TestHarness.java:32)
	at org.stemmate.testing.Role7ClientTests.main(Role7ClientTests.java:96)
```



## J25 Swing form labels have explicit label bindings

Requirements: AC1. Component: DesktopShellFrame form source. Type: Static source accessibility audit.

Steps: Inspect the production form helpers for JLabel.setLabelFor bindings.

Expected: Form labels are programmatically associated with their controls; manual assistive-technology checks remain separate.

Result: FAIL. Evidence: docs/tests/role7/evidence/client/J25.txt.

Observed output:

```text
Observed: setLabelFor bindings=0; named sync/list controls exist but do not bind planner field labels
java.lang.AssertionError: No setLabelFor association is present in DesktopShellFrame
	at org.stemmate.testing.TestHarness.require(TestHarness.java:16)
	at org.stemmate.testing.Role7ClientTests.lambda$cases$38(Role7ClientTests.java:91)
	at org.stemmate.testing.TestHarness.run(TestHarness.java:32)
	at org.stemmate.testing.Role7ClientTests.main(Role7ClientTests.java:96)
```



## S01 Server accepts a valid synthetic plan

Requirements: FR3, FR4. Component: Validator.checkPlan. Type: Server validation unit.

Steps: Validate a synthetic plan with required content and valid identifiers.

Expected: No validation problems.

Result: PASS. Evidence: docs/tests/role7/evidence/server/S01.txt.

Observed output:

```text
Observed: problems=[]
PASS S01
```



## S02 Server rejects blank identity title and safety

Requirements: FR3, FR4. Component: Validator.checkPlan. Type: Server validation unit.

Steps: Remove planId, blank title and blank safety.

Expected: All three required-field problems are returned.

Result: PASS. Evidence: docs/tests/role7/evidence/server/S02.txt.

Observed output:

```text
Observed: [planId is missing, Plan title is required, Safety controls are required (FR4)]
PASS S02
```



## S03 Server enforces the 120 character title limit

Requirements: FR3. Component: Validator.checkPlan. Type: Server validation boundary.

Steps: Validate title lengths of 120 and 121.

Expected: 120 is accepted; 121 is rejected.

Result: PASS. Evidence: docs/tests/role7/evidence/server/S03.txt.

Observed output:

```text
Observed: 120 accepted; 121 rejected
PASS S03
```



## S04 Server rejects invalid update timestamps

Requirements: FR5. Component: Validator.checkPlan. Type: Server validation boundary.

Steps: Try a negative value, alphabetic value and sixteen digits.

Expected: Each invalid timestamp is rejected.

Result: PASS. Evidence: docs/tests/role7/evidence/server/S04.txt.

Observed output:

```text
Observed: three invalid timestamp values rejected
PASS S04
```



## S05 Server rejects a malformed activity ID

Requirements: FR3. Component: Validator.checkPlan. Type: Server validation contract.

Steps: Replace ACT-101 with a UUID-shaped desktop activity ID.

Expected: The current server contract rejects the incompatible format.

Result: PASS. Evidence: docs/tests/role7/evidence/server/S05.txt.

Observed output:

```text
Observed: [activityId must look like ACT-101]
PASS S05
```



## S06 Server checks every FR4 element

Requirements: FR4. Component: Validator.checkPlan. Type: Server requirements negative test.

Steps: Remove steps, timing, materials and inclusion, keeping title and safety.

Expected: An incomplete FR4 plan is rejected.

Result: FAIL. Evidence: docs/tests/role7/evidence/server/S06.txt.

Observed output:

```text
Observed: missing four FR4 elements; validation problems=[]
java.lang.AssertionError: Server accepts a plan missing steps, timing, materials and inclusion
	at stemmate.server.TestHarness.require(TestHarness.java:16)
	at stemmate.server.Role7ServerTests.lambda$cases$8(Role7ServerTests.java:20)
	at stemmate.server.TestHarness.run(TestHarness.java:32)
	at stemmate.server.Role7ServerTests.main(Role7ServerTests.java:28)
```



## S07 Server line codec preserves special characters

Requirements: FR3, FR5. Component: Server.encode and Server.decode. Type: Codec unit.

Steps: Round-trip ampersands, equals, spaces, quotes and a non-ASCII character.

Expected: Every field is preserved exactly.

Result: PASS. Evidence: docs/tests/role7/evidence/server/S07.txt.

Observed output:

```text
Observed: encoded=note=Windhoek+caf%C3%A9&title=Water+%26+light+%3D+safe+%22demo%22
PASS S07
```



## S08 Health route returns its documented status

Requirements: SUS1. Component: Server.handle GET /health. Type: Route component with HttpExchange double.

Steps: Dispatch an in-memory HTTP exchange to the production route handler.

Expected: HTTP 200 and status=OK.

Result: PASS. Evidence: docs/tests/role7/evidence/server/S08.txt.

Observed output:

```text
Observed: status=200; body=status=OK
PASS S08
```



## S09 Sync route rejects invalid device ID

Requirements: FR5. Component: Server.handleSync. Type: Route validation with HttpExchange double.

Steps: Dispatch POST /sync with an invalid device identifier.

Expected: HTTP 400 before any database operation.

Result: PASS. Evidence: docs/tests/role7/evidence/server/S09.txt.

Observed output:

```text
Observed: status=400; body=status=ERROR&message=deviceId+is+missing+or+invalid
PASS S09
```



## S10 Sync route rejects an oversized body

Requirements: QR2. Component: Server.handleSync. Type: Route boundary with HttpExchange double.

Steps: Send MAX_BODY_BYTES plus one byte through an in-memory exchange.

Expected: HTTP 400 before database access.

Result: PASS. Evidence: docs/tests/role7/evidence/server/S10.txt.

Observed output:

```text
Observed: status=400; body=status=ERROR&message=Too+much+data+in+one+request
PASS S10
```



## S11 Java server supports the desktop upload route

Requirements: FR5. Component: HttpRemoteSessionPlanStore and Server.handle. Type: Cross-component route compatibility audit.

Steps: Dispatch the POST /plans route used by client/App.java to the supplied Java server.

Expected: The desktop upload route is supported rather than returning 404.

Result: FAIL. Evidence: docs/tests/role7/evidence/server/S11.txt.

Observed output:

```text
Observed: desktop uses POST /plans; Java server returned 404; status=ERROR&message=Not+found%3A+POST+%2Fplans
java.lang.AssertionError: Desktop client POST /plans is unsupported by the supplied Java server; prototype/src uses a different /sync protocol
	at stemmate.server.TestHarness.require(TestHarness.java:16)
	at stemmate.server.Role7ServerTests.lambda$cases$13(Role7ServerTests.java:25)
	at stemmate.server.TestHarness.run(TestHarness.java:32)
	at stemmate.server.Role7ServerTests.main(Role7ServerTests.java:28)
```



## P01 Prototype line codec preserves plan text

Requirements: FR3, FR5. Component: ServerClient.encode and decode. Type: Codec unit.

Steps: Round-trip punctuation and line breaks in synthetic plan fields.

Expected: The decoded fields are unchanged.

Result: PASS. Evidence: docs/tests/role7/evidence/prototype/P01.txt.

Observed output:

```text
Observed: safety=Line+1%0ALine+2&title=Water+%26+bottles+%3D+%22safe%22
PASS P01
```



## P02 Prototype outbox writes and reloads exact lines

Requirements: FR5, QR1. Component: Outbox.save and load. Type: Real file storage integration.

Steps: Save two synthetic outbox lines; construct a new Outbox and load.

Expected: The two lines are unchanged.

Result: PASS. Evidence: docs/tests/role7/evidence/prototype/P02.txt.

Observed output:

```text
Observed: two lines persisted and restored
PASS P02
```



## P03 Prototype outbox survives a real JVM restart

Requirements: FR5, QR1. Component: Outbox. Type: Real separate-process restart integration.

Steps: A writer JVM saves and exits; a separate reader JVM loads the same isolated file.

Expected: The reader verifies the exact queued item.

Result: PASS. Evidence: docs/tests/role7/evidence/prototype/P03.txt.

Observed output:

```text
Observed: write: wrote queued item
Observed: read: verified exact item in a new JVM
PASS P03
```



## P04 Outbox write failure reaches its caller

Requirements: FR5, QR1. Component: Outbox.save. Type: Real filesystem failure injection.

Steps: Create a regular file where the .stemmate directory must be, then save.

Expected: The caller receives a failure signal and can avoid claiming a durable save.

Result: FAIL. Evidence: docs/tests/role7/evidence/prototype/P04.txt.

Observed output:

```text
Could not save outbox: /workspace/scratch/e3fc6232dd39/work/java_repo/SDN-A3-main/target/role7-cases-7877260011957534989/P04/.stemmate
Observed: save returned to caller without throwing=true
java.lang.AssertionError: Outbox.save only prints the write failure and returns normally; caller receives no failure signal
	at TestHarness.require(TestHarness.java:16)
	at Role7PrototypeTests.lambda$cases$3(Role7PrototypeTests.java:14)
	at TestHarness.run(TestHarness.java:32)
	at Role7PrototypeTests.main(Role7PrototypeTests.java:16)
```
