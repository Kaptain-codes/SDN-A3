# A4 selected demonstrator HTTP contract

This file documents the selected rich Swing client and Role 5 thin Java server. The `prototype/src` client and Ballerina/PostgreSQL JSON API are legacy/alternate paths. The server defaults to port 8080; `STEMMATE_PORT` overrides it. Client URL is configured by `STEMMATE_SERVER_URL` or `-Dstemmate.server.url`.

## Wire format

Sync requests and record-list responses use UTF-8 plain text. Each record is one URL-encoded query-style line (`key=value&key=value`). The client sends at most 10 items per request. The server accepts at most 50 items and a 256 KiB body. Each sync response line contains the item ID, `OK` or `REJECTED`, and a URL-encoded message. The rich client serializes plan steps/timing, materials, safety notes, and inclusion prompts as numbered fields on each `SAVE_PLAN` line.

## Endpoints

| Method and path | Behavior |
|---|---|
| `GET /health` | Returns `status=OK` if the server is responding. |
| `GET /activities` | Returns synthetic activities, one encoded record per line. |
| `GET /plans?deviceId=x` | Returns plans; `deviceId` is optional in the implementation. |
| `POST /sync?deviceId=x` | Accepts up to 50 item lines and returns a result for each. `deviceId` is required and validated. |

## Supported actions

- `SAVE_PLAN`: `planId`, `title`, `activityId`, `targetGroup`, `safety`, `inclusion`, `updatedAt`, and optional indexed child fields (`stepCount` and `stepN*`, `materialCount` and `materialN*`, `safetyCount` and `safetyNText`, `inclusionCount` and `inclusionNPrompt`).
- `SAVE_OFFLINE`: `activityId`.
- `CHECKOUT_KIT`: rejected because FR7 is deferred.

Each operation has a stable item ID. The server records its response in `sync_log` transactionally, then returns the same response if the item is retried. Plan writes use last-write-wins on `updatedAt`.

## Data limitation

The selected server and rich client use matching activity and session-plan tables, including child tables for steps/timing, materials, safety notes, and inclusion prompts. Rich plan fields are sent as indexed query keys. The server currently validates activity IDs using the legacy `ACT-###` pattern while the rich client emits UUIDs; until those are reconciled, plan sync from the rich client is rejected and FR3/FR4 end-to-end behavior is not complete.

Shared tables: `activities`, `activity_steps`, `activity_materials`, `activity_safety_notes`, `session_plans`, `plan_steps`, `plan_materials`, `plan_safety_notes`, and `plan_inclusion_prompts`. Client-only state: `drafts`, `outbox`, and its `sync_log`. Server-only operational state: `sync_log`, `session_plan_devices`, and `activity_offline_selections`.

Shared schema reference: `db/sqlite/schema.sql`; the server copy is `server/src/main/resources/schema.sql`. The unrelated `db/postgres/schema.sql` belongs to the legacy path.
