STEMMate A3 – STEMCraft Desktop Copilot Implementation Plan

Purpose

This plan is for implementing the remaining A3 gaps in the STEMCraft desktop application, not the HTML prototype.

The primary implementation target is the Java desktop application described in the A3 report:

Java 17

Swing UI

FlatLaf theme

Windows desktop application

packaged with jpackage

local STEMMate data stored in the user's .stemmate folder

working workflows intended to include activity browsing, offline saving, session planning, and sync

The HTML prototype may still be used as a reference for interface/accessibility behaviour, but Copilot should treat STEMCraft as the main product to modify and test.

The A3 report identifies the main remaining gaps as sync reliability, conflict handling, FR6 autosave, storage robustness, button visibility/accessibility, incomplete requirement coverage, and missing verification evidence. fileciteturn0file0L790-L869

1. First: Inspect the STEMCraft codebase

Before changing code, Copilot must inspect the project structure and identify:

application entry point;

main window/frame;

navigation/screen management;

Activity Catalog;

Saved Offline;

Session Plan Studio;

Equipment Kit Custody;

sync manager/service;

persistence/storage classes;

plan/session model classes;

activity model classes;

kit/checkout model classes;

role handling;

online/offline state handling;

status-message handling;

validation;

duplicate-plan functionality;

existing tests.

Do not assume the architecture from the HTML prototype.

Create a short internal mapping of:

UI screen -> controller/event handler -> model -> persistence -> sync

Then implement the changes using the existing architecture.

2. Priority 1 – Make STEMCraft sync deterministic

Problem

The report identifies random sync behaviour as BUG-01. The sync result must not depend on randomness because that makes the application impossible to test or trust consistently.

The report's current defect audit describes syncNow() as using Math.random() in the prototype and recommends making normal sync deterministic, with conflicts triggered deliberately for testing. The exact implementation in STEMCraft must be verified from the Java source rather than assumed.

Implementation

Inspect the actual STEMCraft sync implementation.

If sync outcome is randomly generated:

Remove random success/failure/conflict selection.

Define explicit sync states:

ONLINE

OFFLINE

SYNCING

SYNCED

CONFLICT

ERROR

Normal behaviour:

offline -> leave pending changes queued;

online + pending changes -> process the queue deterministically;

successful upload -> mark the record synced;

failure -> retain the record as pending and show an actionable status.

If the prototype has simulated sync rather than a real server, preserve the simulation but make its behaviour deterministic.

If a test/demo conflict is needed, use an explicit controlled test fixture or test mode rather than random behaviour.

Never mark a record synced before the sync operation has actually succeeded.

Acceptance criteria

Repeating the same sync operation produces the same result.

Pending offline plans remain queued until successfully synced.

Successful sync removes/marks only the records that actually synced.

Failed sync does not lose local data.

Offline state does not attempt an upload.

Sync status is visible to the facilitator.

No random number generator determines production sync outcomes.

Verification

Run the equivalent of TC-SYN-01 repeatedly.

The same setup should produce the same result every time.

3. Priority 1 – Implement safe conflict resolution

Problem

BUG-02 concerns conflicts being resolved by overwriting data without showing the facilitator what differs.

The report requires the user to see the local and server versions before choosing which version to retain. fileciteturn0file0L512-L520

Implementation

Inspect the actual STEMCraft conflict model.

When a conflict is detected:

Preserve both versions.

Do not automatically overwri
te either version.

Open a conflict-resolution UI.

Display at minimum:

local version;

remote/server version;

fields that differ.

Provide an explicit choice:

Keep Local

Keep Server

Only after a choice:

persist the selected version;

mark the conflict resolved;

mark the record synced/resolved.

Cancelling conflict resolution must not destroy either version.

Make conflict creation deterministic for testing.

Acceptance criteria

Conflict data is never silently discarded.

Both versions are visible before resolution.

Local selection preserves local changes.

Server selection preserves server changes.

Cancelling does not overwrite data.

Resolved conflicts have an auditable final state.

Verification

Create a controlled local/remote conflict and execute the conflict flow for each resolution choice.

4. Priority 1 – Implement and verify FR6 autosave

Requirement

FR6 is a Must requirement:

Autosave a plan in progress so it survives closing/restarting the app or a dropped connection.

The report currently records FR6 as not tested. fileciteturn0file0L217-L221

Implementation

Use the existing STEMCraft persistence layer if one exists.

Autosave the current session plan while it is being edited.

The autosaved draft must preserve every applicable field, including:

title;

selected activity;

target group;

steps;

timings;

materials;

safety notes;

inclusion prompts;

any additional fields present in the actual STEMCraft form.

Autosave must work while offline.

On application restart:

detect an unfinished draft;

restore it;

preserve every field;

clearly indicate that the draft was restored.

Do not overwrite a completed/finalised plan with an unfinished draft.

Acceptance criteria

Start a plan.

Fill only part of it.

Close STEMCraft.

Reopen STEMCraft.

The incomplete plan is restored.

Every entered field is present.

Dropping the connection does not destroy the draft.

Autosaved data remains clearly distinguishable from synced/finalised data.

Verification

Create:

TC-FR6-AUTOSAVE

Test:

Start plan.

Fill approximately half the fields.

Close STEMCraft.

Reopen STEMCraft.

Verify every field.

Repeat while offline.

5. Priority 1 – Verify FR3 and FR4 in STEMCraft

The report explicitly says the team must confirm whether the Session Plan Studio really saves plans and whether the FR4 fields are actually implemented/persisted. fileciteturn0file0L1034-L1047

FR3

Verify:

Create Plan from an activity works.

The plan can be saved.

The saved plan can be reopened.

The selected activity remains linked.

The plan survives application restart.

FR4

Verify that the session plan contains:

steps;

timing;

materials;

safety notes;

inclusion prompts.

Determine which fields are mandatory from the actual requirements/code. Do not invent mandatory fields if the requirements do not support them.

Acceptance criteria

A saved session plan can be closed and reopened with all entered data intact.

6. Priority 2 – Complete storage error handling

Problem

The report notes that two storage paths still write directly without protection:

saveOffline()

resolveConflict()

The report recommends routing both through safeStorageSet() or the equivalent protected mechanism. fileciteturn0file0L831-L840

For STEMCraft, first identify the actual persistence mechanism. The report describes STEMCraft as storing data in files under .stemmate, so do not blindly copy the HTML localStorage solution.

Implementation

Find every persistent write in STEMCraft.

Centralise persistence error handling.

Ensure failures do not crash the application.

Preserve the user's in-memory work when a write fails.

Show an accessible and understandable warning.

Do not report data as saved when persistence failed.

Ensure offline activity saving, plan saving, autosave, kit records, and sync state all use protected persistence.

Acceptance criteria

A simulated persistence failure:

does not crash STEMCraft;

does not silently discard current work;

produces a clear warning;

does not falsely report successful persistence.

7. Priority 2 – Improve STEMCraft button visibility

The accessibility audit identifies a known STEMCraft issue: Save Offline, Reset all, and Duplicate draft are difficult to see because their light-grey styling blends into the white page. fileciteturn0file0L761-L786

Implementation

Locate the shared FlatLaf/button styling.

Improve the visible border/background distinction.

Ensure text contrast remains compliant.

Ensure hover and keyboard focus states remain obvious.

Apply the fix consistently to affected secondary buttons.

The report gives #767676 as an example darker border, but the final colour must be measured against the actual STEMCraft background.

Acceptance criteria

A user can immediately identify the affected controls as buttons.

Button borders/backgrounds pass the required contrast check.

Focus indication is clearly visible.

Existing primary-button styling is not unnecessarily changed.

8. Priority 2 – Desktop accessibility verification

Because STEMCraft is a Swing desktop application, verify accessibility using desktop-appropriate methods rather than treating browser/Lighthouse results as evidence for STEMCraft.

The report specifically says these areas have not yet been verified:

keyboard-only walkthrough;

minimum target size;

real screen-reader testing. fileciteturn0file0L769-L774

Keyboard

Test the full facilitator workflow without a mouse:

Activity Catalog;

filtering;

activity detail;

Save Offline;

Session Plan Studio;

validation;

save/finalise;

duplicate;

sync;

conflict resolution;

navigation;

role switching where applicable.

Check:

logical focus order;

visible focus;

no focus traps;

Enter/Space activation;

keyboard access to every important action.

Screen-reader/accessibility API

Use the appropriate Windows accessibility tooling available for Swing.

Verify:

buttons have meaningful accessible names;

text fields have labels;

status messages can be perceived;

errors are associated with the relevant fields;

dialogs identify their purpose;

focus moves predictably.

Target size

Measure the actual interactive controls in the desktop UI and document/fix any failures against the chosen accessibility requirement.

9. Priority 2 – Improve validation feedback

The HTML prototype's v1.1 fix marks missing fields, but the report notes that the message still gives a count rather than naming the missing fields. fileciteturn0file0L661-L663

For STEMCraft:

Identify every required plan field.

When validation fails:

highlight each invalid field;

provide a field-specific error;

clearly identify the missing field;

move focus to the first invalid field where appropriate.

Do not rely on colour alone.

Make validation messages understandable without seeing the screen.

Acceptance criteria

A user attempting to save an incomplete plan can immediately identify exactly what must be fixed.

10. Priority 2 – Verify FR7 Equipment Kit Custody

The report says FR7 currently has no test and asks the team to confirm whether Log kit checkout actually adds a row to the Equipment Kit Custody table. fileciteturn0file0L1046-L1049

Test

Enter:

kit name/ID;

responsible person;

intended date;

return status.

Save the checkout.

Verify:

a record is created;

it appears in the custody table;

it survives application restart;

the values are correct.

Add a dedicated test case if the feature is implemented.

If it is only a static screen, do not falsely claim FR7 is implemented.

11. Priority 2 – Verify FR8 Duplicate Draft

The report asks whether Duplicate draft creates a new plan from an existing plan. fileciteturn0file0L1046-L1049

Test

Open an existing plan.

Select Duplicate draft.

Confirm a new plan is created.

Verify the original remains unchanged.

Modify the duplicate.

Verify the original still contains its original values.

Restart STEMCraft and verify both records persist correctly.

If implemented, add a repeatable test case.

12. Priority 3 – Verify roles and shared-laptop behaviour

STEMMate is intended for shared laptops, with Facilitator and Custodian roles. fileciteturn0file0L35-L40

Inspect the actual role implementation.

Verify:

Facilitator can perform facilitator workflows.

Custodian can access kit-custody functionality.

Switching roles does not lose unsaved work.

Role state is clear to the user.

Data from one role remains available where intended.

No learner-identifying data is introduced.

Do not expand role permissions beyond the requirements without confirmation.

13. Priority 3 – Offline-first verification

The report says offline saving and sync are central because schools may have unreliable internet and low-spec/shared devices. fileciteturn0file0L41-L43

Test the desktop application with the connection unavailable.

Verify:

activities already saved offline remain accessible;

plans can be created/edited;

drafts autosave;

sync items are queued;

the user can see the offline state;

reconnecting triggers or allows deterministic sync;

no data is silently lost.

14. Regression test suite

Create or update the STEMCraft test checklist around the existing report tests.

Existing functional tests

TC-FN-01 – filter Grade 7 + Water + 30 minutes.

TC-FN-02 – clear all filters.

TC-FN-03 – open activity detail.

TC-OFF-01 – save activity offline.

TC-OFF-02 – create/save plan offline.

TC-VAL-01 – required-field validation.

TC-SYN-01 – deterministic sync.

TC-SYN-02 – conflict resolution.

TC-A11Y-01 – status message behaviour.

TC-NAV-01 – navigation/back behaviour.

New/required tests

TC-FR6-AUTOSAVE – restore incomplete plan after restart.

TC-STORAGE-01 – persistence failure handling.

TC-FR7-KIT – equipment checkout persistence.

TC-FR8-DUPLICATE – duplicate plan independence.

TC-KEYBOARD-01 – keyboard-only workflow.

TC-A11Y-SR-01 – desktop accessibility/screen-reader verification.

TC-CONTRAST-01 – STEMCraft button contrast.

15. Important: distinguish STEMCraft from the HTML prototype

Do not transfer HTML-specific implementation assumptions into Java.

For example:

HTML prototype concept

STEMCraft implementation

localStorage

Existing STEMCraft file persistence

browser hashchange

Swing screen/navigation handling

browser back button

Desktop navigation/back controls if applicable

aria-invalid

Swing accessibility mechanisms

HTML live region

Swing accessible status/message mechanism

Lighthouse

Desktop accessibility testing

JavaScript Math.random()

Actual Java sync implementation must be inspected

The report describes the HTML prototype as a single HTML/CSS/JavaScript file using localStorage, while STEMCraft is a Java/Swing desktop application using local files. fileciteturn0file0L150-L163

16. Report/prototype consistency checks

After implementing the desktop fixes, update the report only to reflect verified behaviour.

Confirm:

FR1

Browse/filter works.

FR2

Saved activities can actually be opened/used offline.

FR3

Session plans can actually be created and saved.

FR4

Steps, timing, materials, safety notes, and inclusion prompts are actually persisted and behave as claimed.

FR5

Offline plans queue and sync deterministically.

FR6

Autosave survives application restart.

FR7

Kit checkout persistence works, if implemented.

FR8

Duplicate draft works, if implemented.

The report currently warns that the screenshots show FR3/FR4/FR7/FR8 screens but their actual persistence behaviour needs confirmation. fileciteturn0file0L1034-L1049

17. Build and packaging verification

Because STEMCraft is a desktop application, verify the final build itself, not only the source code.

Build using the project's normal Java 17 process.

Launch the packaged application.

Verify required runtime files are present.

Verify the .stemmate data directory is created/used correctly.

Verify data survives application restart.

Test on the target Windows environment.

Confirm SmartScreen/packaging behaviour is documented for the presentation.

Ensure no development-only paths or test data are accidentally packaged.

The report says STEMCraft is packaged with jpackage and bundles its own Java runtime. fileciteturn0file0L157-L163

18. Recommended implementation order

Phase 1 – Core correctness

Inspect STEMCraft architecture and persistence.

Fix deterministic sync.

Fix conflict resolution.

Implement/verify FR6 autosave.

Harden all persistence operations.

Verify FR3/FR4.

Phase 2 – Feature coverage

Verify FR7 Equipment Kit Custody.

Verify FR8 Duplicate Draft.

Verify facilitator/custodian roles.

Verify offline-first behaviour.

Phase 3 – Accessibility

Fix button visibility/contrast.

Improve field-specific validation.

Run keyboard-only tests.

Run desktop screen-reader/accessibility checks.

Verify target sizes and focus states.

Phase 4 – Verification

Run the complete regression suite.

Test persistence/restart.

Test offline -> online transitions.

Test controlled sync conflicts.

Package and run the final Windows build.

Phase 5 – Documentation

Update requirements traceability.

Update defect status.

Record actual test results.

Remove unsupported claims.

Resolve remaining report open questions.

19. Copilot rules

Copilot must follow these rules:

STEMCraft desktop is the primary implementation target.

Inspect existing Java code before modifying it.

Preserve the existing Swing/FlatLaf architecture where practical.

Do not rewrite the application unnecessarily.

Do not copy JavaScript/browser implementation patterns into Java.

Do not introduce random behaviour into production sync.

Never silently overwrite local user work.

Preserve offline-first operation.

Keep persistence failures recoverable.

Do not introduce learner-identifying data.

Do not claim a requirement is implemented without a reproducible test.

Do not change unrelated functionality.

Prefer small, reviewable commits/changes.

After each major change, run the relevant regression test.

If the report and code disagree, inspect the code and requirements and flag the discrepancy rather than silently choosing one.

Keep the HTML prototype unchanged unless a separate task explicitly requires changes to it.

20. Definition of done

The STEMCraft desktop implementation is ready for final verification when:

Sync is deterministic.

Offline pending plans are not lost.

Conflicts show both versions before resolution.

Conflict resolution requires an explicit choice.

FR6 autosave survives application restart.

Persistence failures do not crash the application.

FR3 has verified save/reopen behaviour.

FR4 fields are verified and persisted as required.

FR7 is either verified as working or clearly documented as not implemented.

FR8 is either verified as working or clearly documented as not implemented.

Role behaviour is verified.

Offline-first behaviour is verified.

STEMCraft button visibility is improved and contrast-checked.

Validation identifies the specific fields that need correction.

Keyboard-only workflow is tested.

Desktop accessibility/screen-reader behaviour is tested where tooling is available.

Target sizes/focus states are checked.

All regression tests pass.

Final packaged Windows build is tested.

Requirements traceability matches actual STEMCraft behaviour.

The report contains only claims supported by test evidence.

Source basis

This plan is derived from the A3 report's STEMCraft description, defect audit, remediation, accessibility audit, limitations/next steps, and open questions. The report identifies STEMCraft as the working desktop application and the HTML file as the planned web-interface prototype. fileciteturn0file0L68-L80