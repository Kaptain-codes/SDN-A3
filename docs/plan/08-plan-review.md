# A4 plan review

## Selected scope

- Official demonstrator: Role 5 Java Swing prototype, file outbox, Java thin server and SQLite server database.
- Legacy/alternate: Role 5 `prototype/src` client and Ballerina/PostgreSQL path. Prototype test results are not part of selected rich-client claims unless rerun against the selected `client/` + `server/` path.
- Deferred: FR7 and FR8.
- Unfinished requirement: QR3 remains equal Must for laptop and phone; phone implementation/testing is unfinished.

## Current gaps

- The selected payload/schema does not persist all five FR4 plan elements.
- The visible autosave timer does not persist/restore the full FR6 draft.
- QR2's performance measure and FR5's time limit remain undecided.
- Authentication, logout warning and administrator UI are absent.
- Accessibility and phone checks have not been recorded for the selected client.
- Role 5's README reports useful tests, but their logs should be retained in the hand-in.

## Required follow-up

1. Keep diagrams and setup docs specific to the selected thin SQLite server.
2. Make the sync limit distinction explicit: client batches 10; server cap 50.
3. Extend plan payload and SQLite schema before claiming FR4 complete.
4. Implement durable drafts before claiming FR6 complete.
5. Keep QR3's phone work marked unfinished until built and tested.
6. Preserve test output and update all 13 traceability rows with actual results/issues.
