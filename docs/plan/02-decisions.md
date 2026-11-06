# A4 implementation decisions and boundaries

## Selected vertical slice

The project selected the rich client in `client/` together with the Role 5 thin Java SQLite server in `server/src` as the official A4 demonstrator. `prototype/src` is retained as a legacy prototype. See [ADR-003](../architecture/ADR-003-thin-sqlite-server.md).

## Local and server persistence

- Client queue: `LocalOutboxRepository` persists the outbox in client SQLite alongside drafts and plans.
- Server: `Database.java` stores activities, plans, offline activity selections and per-item response logs in SQLite.
- Shared activity/plan tables are aligned between `db/sqlite/schema.sql` and `server/src/main/resources/schema.sql`; local drafts/outbox remain client-only, while device mapping and idempotency records remain server-only. `db/postgres/schema.sql` belongs to the legacy path.

## Sync semantics

Client batches contain at most 10 items; server accepts at most 50 and rejects oversized bodies. A stable item ID allows the server to replay the recorded response after a retry. Plan updates use last-write-wins by `updatedAt`. The SQLite operation and response log are committed together per item.

## Requirement gaps

FR4 is partial because the selected line payload does not include steps, timing, or materials. FR6 is partial because the visible 800 ms autosave timer updates the status badge but does not durably store and restore the full draft. QR3 remains a Must on both device categories; phone support is unfinished. QR2's quantitative low-spec measure and FR5's time limit remain open.
