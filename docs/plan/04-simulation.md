# Simulation strategy

The prototype has an explicit offline/online toggle that is the deterministic implementation of `ConnectivityMonitor`. Toggling offline prevents remote calls; toggling online notifies `SyncManager` to process at most 20 outbox records. A `FakeRemoteSessionPlanStore` accepts a configured success/failure mode so the demo can show both confirmation and retry without depending on live infrastructure.

The seed catalogue contains about 15 synthetic activities. No real people, pupils, credentials, or identifying fields are used.

| Area | Real in the prototype | Simulated |
|---|---|---|
| Connectivity | Client toggle and status shown in text | Internet availability |
| Outbox and status | SQLite persistence and status transitions | None |
| Server sync | Repository contract and batch/idempotency behavior | Ballerina response and failure injection |
| Activity cache/filtering | Local catalogue and filtering | Initial catalogue download |
| Plan builder/save | Form, validation, local-first write | None |
| Logout warning | Outbox count check | Shared-device session boundary |
| Failure support | Failure records and help text | Administrator deployment |
