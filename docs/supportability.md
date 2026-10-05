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
