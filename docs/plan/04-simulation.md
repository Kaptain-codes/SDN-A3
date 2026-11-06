# A4 selected demonstrator: real and simulated behavior

| Area | Real in selected slice | Simulated / not implemented |
|---|---|---|
| Connectivity | HTTP client and server are real processes; server returns per-item results | Client Online/Offline toggle represents connectivity; proxy injects delay/drop conditions in resilience checks |
| Client outbox | File-backed queue with atomic replacement and restart loading | None claimed |
| Server persistence | SQLite with WAL/FULL settings; operation and sync-log answer committed transactionally | Remote/cloud deployment is not part of the demo |
| Retry/idempotency | Stable item IDs, server response log, client retry/backoff | Internet quality itself is simulated by proxy |
| Activity list | Server endpoint returns synthetic seeded activities | Production catalogue and catalogue download lifecycle are not demonstrated |
| Plan save | UI queues a plan line and server validates/persists title, target, safety and inclusion fields | Full five-element FR4 plan is not supported; steps, timing and materials are absent |
| Draft autosave | UI timer displays an autosave message | Durable full-draft storage/restore is not implemented by the visible timer |
| Mobile support | None | Phone client and QR3 phone acceptance are unfinished |
| Authentication/logout | None | Shared-user identity and unsynced logout warning are not implemented |

The Ballerina/PostgreSQL JSON endpoint and original Swing client are legacy/alternate implementation paths and are excluded from selected-slice claims.
