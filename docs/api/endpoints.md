# API endpoints

The API contains no authentication, session parameters, or pupil-identifying fields. Payloads contain synthetic catalogue and plan data only.

## GET `/activities`

Returns the synthetic activity catalogue. The client may cache the response and filter it locally while offline.

## POST `/plans`

Accepts a JSON batch of at most 20 plans. Each plan includes `planId` and `updatedAt`; the server upserts by `planId` and applies last-write-wins using `updatedAt`. Repeating a batch is idempotent. The response reports each accepted plan and its server receipt time.

The endpoint validates the five plan elements (steps, timing, materials, safety notes, and inclusion prompts) and does not accept identifying data.

## GET `/sync/health`

Returns a small health response such as `{ "status": "ok" }` for the client's connectivity check. It does not create a session or return user data.
