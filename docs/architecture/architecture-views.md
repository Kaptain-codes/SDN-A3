# A4 architecture views

These views describe the selected rich Java Swing client in `client/`, its local SQLite database, and the thin Java server backed by SQLite. The Role 5 `prototype/src` client remains a legacy prototype. “Present” means visible in the repository, not verified against acceptance criteria.

## 1. System context

The facilitator uses the desktop client to browse activities, prepare plans, save drafts, and queue plans for synchronization. Client SQLite stores local drafts/outbox and mirrors shared activity/plan tables. The server validates sync requests, stores shared plan data and idempotent results. Phone delivery remains unfinished under QR3.

## 2. Containers and deployment

| Container | Technology / location | Data and role |
|---|---|---|
| Rich client | Java Swing, `client/src/main/java/org/stemmate` | Activity filters, five-element plan form, draft autosave, visible sync status |
| Client database | SQLite, configurable by `STEMMATE_SQLITE_PATH` | Shared activity/plan tables plus client-only drafts, outbox, sync status |
| HTTP adapter | `HttpRemoteSessionPlanStore` | URL-encoded line protocol to `/sync`; sends batches of 10 |
| Thin server | Java HTTP server, `server/src/main/java/stemmate/server` | `/health`, `/activities`, `/plans`, `/sync`; accepts at most 50 items and 256 KiB |
| Server database | SQLite, default `stemmate.db` in server working directory | Shared activities/plans plus server-only device associations and idempotency log; WAL and FULL synchronous mode |

The common schemas are `db/sqlite/schema.sql` and `server/src/main/resources/schema.sql`; shared table definitions should remain aligned. Client-only state does not belong on the server.

## 3. Component responsibilities

- `DesktopShellFrame` and `DesktopController`: desktop UI, activity filtering, plan editing, offline state, and user-visible sync status.
- `SqliteDatabase`: client SQLite schema, local activity caching, and legacy migration.
- `LocalSessionPlanStore` and `LocalOutboxRepository`: local plans, drafts, outbox records, and state transitions.
- `HttpRemoteSessionPlanStore`: serializes plan steps/timing, materials, safety notes, and inclusion prompts to the thin server protocol.
- `Server`: HTTP routes and request size/count limits.
- `Validator` and `Database`: validate writes, store shared plan data transactionally, and return idempotent results.

## 4. Sync and recovery

The client saves plans and outbox state locally before attempting sync. It sends stable item IDs in batches of 10. The server records each operation and response in a transaction, so a retry with the same ID receives the original result. Unconfirmed work remains local for retry. The server caps requests at 50 items and 256 KiB. The older prototype’s restart/bad-connection results must not be presented as rich-client verification unless rerun on this selected path.

## 5. Requirements and current gaps

The desktop rich client contains filters and a complete plan model/form, but its UI catalogue remains sourced from an in-memory `LocalActivityStore`; durable SQLite activity cache integration needs work. Client activity UUIDs also conflict with the server’s current `ACT-###` validation. Offline open, full plan sync, and draft restart restore need selected-path verification. There is no phone client, logout warning, administrator log interface, low-spec performance measure, or selected-client accessibility result. FR7 and FR8 remain deferred by scope.

The requirements baseline is [`docs/requirements.md`](../requirements.md), with detailed evidence in [`docs/traceability.md`](../traceability.md). ADR-001 preserves QR3 as a Must requirement; ADR-002 and ADR-003 describe implementation choices and should be read with the updated selected path.
