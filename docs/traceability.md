# Traceability

| Evidence | Requirement | Prototype element | Evaluation check |
|---|---|---|---|
| Cached catalogue filters | FR1 | Activity list and criteria repository | Filter by level, topic, duration, and materials offline |
| Locally available activity | FR2 | `saveForOffline` and cached activity details | Open a saved activity without a network |
| Complete session plan | FR3 | Plan builder and local save | Select an activity, complete and save a plan |
| Five required plan elements | FR4 | Steps, timing, materials, safety notes, inclusion prompts | Confirm all five display on saved plan |
| Offline queue and confirmation | FR5 | Outbox and SyncManager | Save offline, reconnect, and observe synced status |
| Autosaved draft | FR6 | Draft table and restore flow | Close mid-plan and reopen |
| Local-first retention | QR1 | Transactional local plan/outbox write | Fail network and confirm work remains |
| Small-batch low-resource sync | QR2 | Batch size limit and status bar | Sync with throttled/failing connection |
| Failure log and help | SUS1 | Sync log and help screen | Inspect recent failure and recovery guidance |
| Accessible status and controls | AC1 | Keyboard/focus/contrast checklist | Complete accessibility checks |

## Commit convention

Every commit message starts with one allowed requirement ID: `FR1`, `FR2`, `FR3`, `FR4`, `FR5`, `FR6`, `FR7`, `FR8`, `QR1`, `QR2`, `SUS1`, or `AC1`.
