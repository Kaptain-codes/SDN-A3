# Plan review

## Findings

- The scope is manageable only if FR7 and FR8 remain deferred; their exclusion is explicit in the context and implementation plan.
- Live infrastructure is not required for the prototype because the local SQLite path and fake remote store demonstrate the resilient workflow. Ballerina/PostgreSQL remain documented integration surfaces.
- Every implementation task has requirement IDs. The schema baseline and service work should not be started without the contract task.
- UI code must call services, and services must depend on repository interfaces; concrete stores belong behind adapters.
- No sign-in or AuthService is part of the prototype. Any future shared-device role work must not add identity fields to these contracts.
- The design leaves measurable QR2 timing thresholds undefined; the evaluation should record a practical device observation without inventing a new requirement ID.

## Prioritized cut list

1. Cut live Ballerina deployment and use the fake remote store if time is constrained; preserve the API contract and tests.
2. Keep Swing screens minimal and defer visual polish while retaining keyboard, focus, contrast, and text status behavior for AC1.
3. Keep the sync-failure log as SQLite rows plus a help screen; defer export and administration UI.
4. Do not implement FR7 or FR8 in the prototype.
