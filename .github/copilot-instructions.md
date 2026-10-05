# STEMMate project rules
- Stack: Java desktop client using the Repository pattern, Ballerina backend, PostgreSQL as the remote database, SQLite as the local database. Package root: org.stemmate.
- Source of truth: docs/design/STEMMate_A3_Technical_Resilience_and_Low-Resource_Design_v2.md (section 4.1 for components and contracts) and docs/design/Requirments.md.
- Requirement IDs are FR1-FR8, QR1, QR2, SUS1, AC1 only. Never invent or use other IDs.
- Prototype scope: workflow 1 Find an activity (FR1, FR2), workflow 2 Create and save a session plan (FR3, FR4, FR6), workflow 3 Offline sync and recovery (FR5, QR1, QR2, SUS1). FR7 and FR8 are deferred.
- No sign-in, AuthService, session parameters or pupil-identifying data anywhere. Synthetic data only.
- Services depend on repository interfaces, never on concrete stores. save() is local-first and idempotent by planId (UUID).
- Output rule: when asked to produce a plan, design or schema, write it to the specified file path, then reply with only the file path and a summary of at most five lines. Do not paste the whole file into chat. If a file exists, update it instead of creating a second copy.
- AI log: after each step, append one entry to docs/ai-use-log.md with the date, the step name, the files written, and a one-line note of what to verify. Do not write anything about test results or participants.
