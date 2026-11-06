# ADR-003: Use the thin SQLite server for the A4 demonstrator

**Status:** Selected for A4 scope  
**Decision date:** 6 November 2026  
**Decision owner:** Project team; architecture owner should review and record final sign-off

## Context

Two client paths are present. The rich Java Swing application has a repository-based planner and a local SQLite conversion. Role 5 added a separate vertical slice using a line-based HTTP protocol and a thin SQLite server. These paths originally had different clients, payloads, endpoints, and storage, so the rich client adapter and shared SQLite tables are being aligned to the server contract.

## Decision

Use the rich Java Swing client in `client/` with the Role 5 thin Java SQLite server in `server/` as the sole official A4 demonstrator. Keep `prototype/src` as a legacy prototype and do not combine its evidence with the selected slice unless tests are rerun against the selected client.

## Consequences

- The report, diagrams, API guide and traceability refer to the selected path only.
- The prototype server stores data in a local SQLite file; it is not a deployed remote production database.
- The client sends 10 records per request; the server rejects requests over 50 records.
- The client keeps drafts and outbox state in its local SQLite database; shared activity/plan tables are aligned with the server schema. Server device mapping and idempotency log remain server-only.
- End-to-end integration and legacy database migrations require verification; database-column alignment alone does not establish acceptance.
- QR3 remains unsatisfied until a phone-capable implementation and acceptance test exist.
- Role 5's reported restart and bad-connection checks are evidence only after their outputs are retained and attributable.
- No authentication or administrator UI is added by this decision.
