# Supportability guide

## User status messages

- **Waiting to sync:** The plan is safely saved on the device and will upload when
  the connection returns.
- **Syncing:** The client is sending plans in a small batch.
- **All changes synced:** The server confirmed the plan.
- **Sync failed. Retry when online:** The local copy was kept. Restore the connection
  and retry.

## Administrator sync log

The local sync log records a failed plan ID, the `FAILED_RETRY` status, the time of
the attempt, and the plain-language failure message. The log is bounded to the most
recent 200 entries so it remains suitable for low-resource devices. A support
administrator should use the plan ID and attempt time when investigating a repeated
failure; no pupil-identifying data is recorded.

## Recovery steps

1. Check the connection status.
2. Leave the application open while a retry runs.
3. Confirm that the status changes to **All changes synced**.
4. If it remains failed, retain the plan ID and message for support.
# Remote sync service

The desktop client writes plans and outbox state locally before it calls the
remote service. Configure the service URL with `STEMMATE_REMOTE_BASE_URL` or
`-Dstemmate.remote.baseUrl`; the default is `http://localhost:9090`.

Start the Ballerina service from `server/` with a local `Config.toml` copied
from `Config.toml.example`. Apply `db/postgres/schema.sql` to the configured
database before starting it. Do not commit `Config.toml` or database
credentials.

`POST /plans` accepts at most 20 plans. A `503` response leaves the plan in
`FAILED_RETRY`; retrying the same `planId` is safe because PostgreSQL
upserts by `plan_id`. `GET /sync/health` verifies that the service can reach
PostgreSQL.

If the service is unavailable, the desktop application still starts and local
plans remain available. The Sync status bar and local sync log are the source
of truth for whether a plan is waiting, syncing, synced, or retryable.
