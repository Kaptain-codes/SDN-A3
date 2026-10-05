# Test plan

All result columns intentionally remain empty until execution.

| Test | Method | Requirement ID | Expected evidence | Result |
|---|---|---|---|---|
| Filter cached catalogue by all criteria offline | JUnit fake activity repository | FR1 | Relevant activities are returned with network disabled | |
| Open saved activity offline | JUnit fake activity repository | FR2 | Saved activity remains available without remote access | |
| Save a complete plan | JUnit fake session-plan repository | FR3, FR4 | Plan contains steps, timing, materials, safety notes, inclusion prompts | |
| Repeat save with same planId | JUnit repository fake | FR5 | One logical record remains and latest updatedAt wins | |
| Local-first write | JUnit repository fake with failing remote | QR1 | Local plan and outbox record exist before remote attempt | |
| Restore autosaved draft | JUnit draft fake | FR6 | All draft fields are restored | |
| Offline evaluation checks | Manual script using connectivity toggle | FR5, QR1, QR2 | Waiting-to-sync, recovery, small batch, and logout warning are visible | |
| POST /plans validation and upsert | Ballerina tests | FR3, FR5 | Batch limit, idempotency, and updatedAt last-write-wins behavior work | |
| Three-to-five classmate evaluation tasks | Manual script | FR1, FR2, FR3, FR5, AC1 | Each participant completes task and evaluator records observations | |
