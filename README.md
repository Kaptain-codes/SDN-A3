# STEMMate A4 demonstrator

The selected A4 implementation is the rich Java Swing client in `client/` connected to the thin Java SQLite server in `server/`. The client keeps plans, drafts, and its sync outbox in local SQLite; the server stores shared activity/plan data and idempotent sync results. See [docs/supportability.md](docs/supportability.md) for setup and recovery and [docs/api/endpoints.md](docs/api/endpoints.md) for the selected protocol.

For step-by-step build and run commands, use [RUNNING_STEMMATE_A4.md](RUNNING_STEMMATE_A4.md).

Acceptance checklists and troubleshooting steps are in [`acceptance test/`](acceptance%20test/).

The Role 5 vertical slice in `prototype/src` is retained as a legacy prototype. Its earlier test results do not establish the rich client’s behavior unless rerun against the selected path.

The canonical requirements are in [docs/requirements.md](docs/requirements.md); current evidence and gaps are in [docs/traceability.md](docs/traceability.md). The desktop client has rich activity filters and plan/draft models, but QR3 phone support is unfinished, and offline catalogue integration, end-to-end ID compatibility, low-spec evidence, logout warning, administrator log viewing, and selected-client accessibility verification remain open.
