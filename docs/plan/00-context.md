# A4 context and selected demonstrator

## Product goal

STEMMate helps facilitators find STEM activities and prepare session plans in low-connectivity settings. It uses synthetic demonstration data and stores no learner-identifying data.

## A4 requirement baseline

`docs/requirements.md` is the canonical 13-requirement baseline: FR1–FR8, QR1–QR3, SUS1 and AC1. QR3 requires the FR1–FR6 workflows to have equal Must priority on laptop/desktop and mobile. The current selected demonstrator is laptop-only; phone delivery and verification are unfinished, so QR3 is not yet met. FR7 and FR8 are deferred in the selected slice.

## Official A4 vertical slice

Use the rich Java Swing application in `client/` with the thin Java SQLite server in `server/`:

1. The rich client provides activity filters and a five-element plan editor.
2. Client SQLite stores plans, drafts, and the sync outbox; the shared activity/plan schema is aligned with the server.
3. `HttpRemoteSessionPlanStore` sends URL-encoded plan records to `POST /sync` in batches of 10.
4. The server validates requests and stores shared plans and idempotent responses in SQLite (default `stemmate.db` in the server working directory).
5. Unconfirmed client outbox items remain local for retry.

This is a prototype, not a production deployment. The activity catalogue is not yet loaded from persistent SQLite, client/server activity IDs conflict, and end-to-end sync plus draft restart behavior remain to be verified.

## Legacy paths

The selected A4 implementation combines the rich Java Swing client in `client/` with the thin Java SQLite server in `server/`. The Role 5 vertical-slice client in `prototype/src` and the Ballerina/PostgreSQL service are legacy alternatives. Shared activity/plan schemas and the rich client’s SQLite plan/draft/outbox path have been aligned, but end-to-end behavior and migration variants still require verification.

## Other constraints and unresolved measures

- No authentication or account/session model is included. The selected rich client has no logout warning.
- FR5 sync-time limit and QR2 low-spec performance measure remain unresolved; do not invent thresholds.
- Connectivity toggle is a deterministic simulation. Tests through a proxy may simulate delay/drop behavior while using a real local server and database.
- Mobile support is unfinished; no phone acceptance test is claimed.
- Accessibility must be evaluated against the actual selected rich client, not inferred from historical checklist entries.
