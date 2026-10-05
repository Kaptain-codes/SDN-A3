# Context

## Stack

STEMMate is a Java desktop client using the Repository pattern, a Ballerina service, PostgreSQL as the remote database, and SQLite as the local database. The Java package root is `org.stemmate`. The client remains usable without a network connection and synchronises local work in small batches.

## Prototype workflows

1. **Find an activity** — FR1, supported by FR2. Browse the cached catalogue and filter by level, topic, duration, and materials while offline.
2. **Create and save a session plan** — FR3, FR4, supported by FR6 and QR1. Build a plan with steps, timing, materials, safety notes, and inclusion prompts; autosave drafts and save locally first.
3. **Offline sync and recovery** — FR5, supported by QR1, QR2, and SUS1. Queue offline plans, synchronise on reconnect, retry failures, and expose a failure log and status help.

## Out of scope

FR7 equipment-kit records and FR8 plan duplication are deferred. The prototype has no sign-in, AuthService, session parameters, or pupil-identifying data. Data is synthetic.

## Ambiguities found

- The design document leaves the QR2 load-time and sync-time targets to be defined.
- The design mentions role-based access and logout warnings, but authentication is explicitly outside this prototype; only the unsynchronised-item warning is actionable.
- The source design describes a remote store as simulated for the prototype, while the target architecture includes a Ballerina API and PostgreSQL.
- The requirements source has a truncated FR4 acceptance sentence; the design document supplies the complete five-element interpretation.
- The existing UI is an HTML prototype, while the target client is a Java desktop application; this plan treats the HTML as design reference rather than the implementation toolkit.
