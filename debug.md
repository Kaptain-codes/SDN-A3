# Current implementation scan

## Issues observed

1. `Activity` does not model the `title` and `inclusionPrompts` fields present in
   `client/src/main/resources/seed/activities.json`. Seed loading is not implemented,
   so those fields are currently unused.
2. There are no Java test sources yet, although `docs/plan/06-test-plan.md` defines
   focused tests for catalogue filtering and plan validation.
3. The local plan store is currently an in-process prototype adapter; it does not yet
   write the plan payload to SQLite. The schema and repository boundary are present,
   but durable persistence needs a later hardening pass.
4. `ActivityListScreen` renders the record's default `toString()` rather than a
   user-facing activity title or summary.
5. The Swing screens do not yet expose explicit accessibility labels, focus ordering,
   or non-colour status cues; those checks belong to task 5.1.
6. The database schemas and Java records are not currently connected by a persistence
   adapter, so schema constraints are not enforced by the client at runtime.
7. `ActivityCriteria` and the activity cache accept nullable/blank filter values by
   convention, but this behavior is not documented in the repository interface.

## Scope decision

This scan records issues without pulling future tasks into the current change. Task 3.1
will implement plan construction, validation, and a builder view only. Draft restoration,
concrete persistence, synchronisation, accessibility remediation, and seed parsing remain
for their planned tasks.
