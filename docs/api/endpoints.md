# API endpoints

The API contains no authentication, session parameters, or pupil-identifying fields. Payloads contain synthetic catalogue and plan data only.

## GET `/activities`

Returns the synthetic activity catalogue. The client may cache the response and filter it locally while offline.

## POST `/plans`

Accepts `{"plans": [...]}` with at most 20 plans. Each plan contains `planId`, `activityId`, `title`, `targetGroup`, `createdAt`, `updatedAt`, `steps`, `timingMinutes`, `materials`, `safetyNotes`, and `inclusionPrompts`. The server upserts by `planId` and applies last-write-wins using `updatedAt`. Repeating a batch is idempotent. The response reports each accepted plan and its server receipt time:

```json
{"accepted":[{"planId":"00000000-0000-0000-0000-000000000000","receivedAt":"2026-10-07T20:00:00Z"}]}
```

The endpoint validates the five plan elements (steps, timing, materials, safety notes, and inclusion prompts) and does not accept identifying data.

`400` indicates an invalid payload or a batch larger than 20. `503` indicates that the service or PostgreSQL is unavailable, so the client keeps the plan retryable. The client base URL is configured through `STEMMATE_REMOTE_BASE_URL` or `-Dstemmate.remote.baseUrl`, defaulting to `http://localhost:9090`.

## GET `/sync/health`

Returns a small health response such as `{ "status": "ok" }` for the client's connectivity check. It does not create a session or return user data.
