# Defect log

Recorded 06 November 2026 at 21:43 CAT. All issues below are open in the supplied source. Proposed fixes are recommendations, not completed or retested changes. Ten failures are retained in the raw evidence.



## D01 Desktop upload body is invalid JSON

Severity: High. Owner: Role 5 / Role 6. Requirements: FR4, FR5.

Component: HttpRemoteSessionPlanStore.planJson and field. Test: J16.

Reproduction: Upload through a capturing HttpClient double and inspect the actual request body.

Expected: UUID and title fields are quoted JSON strings.

Actual: UUIDs, titles and timestamps are appended without JSON quotes. The captured request body contains title:Synthetic water plan without a string value.

Proposed action: Separate string-field serialization from raw array/number fields. Use a JSON writer or quote every scalar string. Retest J16 against the intended Ballerina API.

Status: OPEN - observed in supplied repository. Evidence: docs/tests/role7/evidence/client/J16.txt.



## D02 Rejected IDs can be parsed as accepted

Severity: High. Owner: Role 5 / Role 6. Requirements: FR5, QR1.

Component: HttpRemoteSessionPlanStore.parseConfirmation. Test: J18.

Reproduction: Return accepted=[] and put the sent ID only in rejected.

Expected: The rejected plan ID is never returned as confirmed.

Actual: A body with accepted=[] and the plan ID only under rejected returns that ID as confirmed.

Proposed action: Parse the accepted array structurally and accept only sent IDs inside that array. Reject malformed or inconsistent confirmations.

Status: OPEN - observed in supplied repository. Evidence: docs/tests/role7/evidence/client/J18.txt.



## D03 Failed plan save leaves a durable orphan queue entry

Severity: High. Owner: Role 4 / Role 5. Requirements: FR3, QR1.

Component: LocalSessionPlanStore.save. Test: J21.

Reproduction: Make plans.ser a directory so the plan write fails, then reload the outbox.

Expected: The failed save does not leave a durable queue entry without a durable plan.

Actual: When plans.ser is made unwritable as a directory, save throws, but outbox.ser already contains one pending record without a durable plan.

Proposed action: Make plan and outbox persistence one durable transaction or use a recoverable journal. Do not describe synchronized methods as a cross-file transaction.

Status: OPEN - observed in supplied repository. Evidence: docs/tests/role7/evidence/client/J21.txt.



## D04 Unexpected stored object type crashes startup

Severity: High. Owner: Role 5 / Role 6. Requirements: FR6, QR1.

Component: FileStoreSupport.read. Test: J22.

Reproduction: Write a serialised String where plans.ser expects a map, then open the repository.

Expected: No uncaught ClassCastException escapes; a diagnosable recovery path is available.

Actual: Loading a serialized String instead of a LinkedHashMap throws ClassCastException.

Proposed action: Check the restored type, preserve the original file, and return a clear recovery result or visible warning. Do not silently overwrite the old data.

Status: OPEN - observed in supplied repository. Evidence: docs/tests/role7/evidence/client/J22.txt.



## D05 Blank safety-note content passes validation

Severity: Medium. Owner: Role 5. Requirements: FR4.

Component: SessionPlanService.validate. Test: J23.

Reproduction: Provide a nonempty safety-note list whose text is only spaces.

Expected: ValidationException is raised for empty safety content.

Actual: A nonempty list containing SafetyNote("   ") passes validation.

Proposed action: Validate the text of each required element as well as collection size. Apply equivalent checks to steps, materials and inclusion.

Status: OPEN - observed in supplied repository. Evidence: docs/tests/role7/evidence/client/J23.txt.



## D06 Interrupted SYNCING records are excluded from retry

Severity: High. Owner: Role 4 / Role 5. Requirements: FR5, QR1.

Component: LocalOutboxRepository.findPending. Test: J24.

Reproduction: Persist SYNCING to represent interruption after upload begins; reload the outbox.

Expected: The interrupted record is eligible for retry rather than stuck outside the pending queue.

Actual: A persisted SYNCING record reloads with retry queue size zero, because only WAITING_TO_SYNC and FAILED_RETRY are selected.

Proposed action: On startup recover interrupted SYNCING items to a retryable state, or explicitly include them in a safe idempotent retry strategy.

Status: OPEN - observed in supplied repository. Evidence: docs/tests/role7/evidence/client/J24.txt.



## D07 Planner label bindings are missing in source

Severity: Medium. Owner: Role 6. Requirements: AC1.

Component: DesktopShellFrame form helpers. Test: J25.

Reproduction: Inspect the production form helpers for JLabel.setLabelFor bindings.

Expected: Form labels are programmatically associated with their controls; manual assistive-technology checks remain separate.

Actual: The inspected form source contains zero JLabel.setLabelFor calls. Some sync/list controls have accessible names; that does not associate planner captions with their fields.

Proposed action: Bind labels to their actual fields and provide accessible names where needed. Then run keyboard and screen-reader checks. This static result is not a full accessibility verdict.

Status: OPEN - observed in supplied repository. Evidence: docs/tests/role7/evidence/client/J25.txt.



## D08 Java server accepts plans missing FR4 content

Severity: High. Owner: Role 4 / Role 5. Requirements: FR4.

Component: Validator.checkPlan and server plan data model. Test: S06.

Reproduction: Remove steps, timing, materials and inclusion, keeping title and safety.

Expected: An incomplete FR4 plan is rejected.

Actual: Removing steps, timing, materials and inclusion produces an empty validation-problem list. The current plan table also has no steps, timing or materials columns.

Proposed action: Agree the complete FR4 payload for the selected final slice, validate it at the server, and preserve every required element in durable storage.

Status: OPEN - observed in supplied repository. Evidence: docs/tests/role7/evidence/server/S06.txt.



## D09 Desktop client and Java server use incompatible upload contracts

Severity: High if paired. Owner: Role 1 / Role 5 / Role 6. Requirements: FR5.

Component: client/App.java, HttpRemoteSessionPlanStore and server/Server.java. Test: S11.

Reproduction: Dispatch the POST /plans route used by client/App.java to the supplied Java server.

Expected: The desktop upload route is supported rather than returning 404.

Actual: The desktop client targets the Ballerina JSON POST /plans interface on port 9090. The supplied Java server accepts line-format POST /sync on port 8080 and returns 404 for POST /plans. The separate prototype client uses /sync.

Proposed action: The PM and developers must name the final client/server pair. If the desktop client is paired with the Java server, adapt the route, payload, IDs and timestamps. Do not treat this failure as proof that the correctly paired standalone prototype cannot sync.

Status: OPEN - observed in supplied repository. Evidence: docs/tests/role7/evidence/server/S11.txt.



## D10 Prototype outbox does not report write failure to its caller

Severity: High. Owner: Role 5 / Role 6. Requirements: FR5, QR1.

Component: prototype/src/Outbox.save. Test: P04.

Reproduction: Create a regular file where the .stemmate directory must be, then save.

Expected: The caller receives a failure signal and can avoid claiming a durable save.

Actual: An injected filesystem failure is printed to stderr, but save returns normally and gives the calling UI no exception or result indicating failure.

Proposed action: Return a success/failure result or throw a handled storage exception. Only show a durable-save message after a successful write.

Status: OPEN - observed in supplied repository. Evidence: docs/tests/role7/evidence/prototype/P04.txt.
