# Requirement test traceability
Requirement wording comes from docs/design/Requirments.md, section 3 of docs/design/STEMMate_A3_Technical_Resilience_and_Low-Resource_Design_v2.md and docs/traceability.md. Components below are actual inspected classes. FR7 and FR8 remain outside this tester scope.
| Requirement | Component | Test | Result | Issue | Evidence |
|---|---|---|---|---|---|
| FR1 | LocalActivityStore.findByCriteria | J01 | PASS | - | docs/tests/role7/evidence/client/J01.txt |
| FR2 | LocalActivityStore | J02 | PASS | - | docs/tests/role7/evidence/client/J02.txt |
| FR3, FR4 | SessionPlanService.createDraftFromActivity | J03 | PASS | - | docs/tests/role7/evidence/client/J03.txt |
| FR3, FR4, FR5 | SessionPlanService and LocalSessionPlanStore | J04 | PASS | - | docs/tests/role7/evidence/client/J04.txt |
| FR3, FR4 | SessionPlanService.validate | J05 | PASS | - | docs/tests/role7/evidence/client/J05.txt |
| FR4 | SessionPlanService.validate | J06 | PASS | - | docs/tests/role7/evidence/client/J06.txt |
| FR4 | SessionPlanService.validate | J07 | PASS | - | docs/tests/role7/evidence/client/J07.txt |
| FR6 | SessionPlanService.autosaveDraft and LocalSessionPlanStore | J08 | PASS | - | docs/tests/role7/evidence/client/J08.txt |
| FR2, FR3, FR5, FR6, QR1 | LocalSessionPlanStore, LocalOutboxRepository and LocalActivityStore | J09 | PASS | - | docs/tests/role7/evidence/client/J09.txt |
| FR3, FR5 | LocalSessionPlanStore.save | J10 | PASS | - | docs/tests/role7/evidence/client/J10.txt |
| FR5, QR1 | SyncManager and ConnectivityMonitor | J11 | PASS | - | docs/tests/role7/evidence/client/J11.txt |
| FR5 | ConnectivityMonitor and SyncManager | J12 | PASS | - | docs/tests/role7/evidence/client/J12.txt |
| FR5, QR1, SUS1 | SyncManager and LocalSyncLogStore | J13 | PASS | - | docs/tests/role7/evidence/client/J13.txt |
| FR5, QR1 | SyncManager | J14 | PASS | - | docs/tests/role7/evidence/client/J14.txt |
| QR2, FR5 | SyncManager.MAX_BATCH_SIZE | J15 | PASS | - | docs/tests/role7/evidence/client/J15.txt |
| FR4, FR5 | HttpRemoteSessionPlanStore.planJson | J16 | FAIL | D01 | docs/tests/role7/evidence/client/J16.txt |
| FR5 | HttpRemoteSessionPlanStore.parseConfirmation | J17 | PASS | - | docs/tests/role7/evidence/client/J17.txt |
| FR5, QR1 | HttpRemoteSessionPlanStore.parseConfirmation | J18 | FAIL | D02 | docs/tests/role7/evidence/client/J18.txt |
| FR5, QR1 | HttpRemoteSessionPlanStore.uploadBatch | J19 | PASS | - | docs/tests/role7/evidence/client/J19.txt |
| QR2 | HttpRemoteSessionPlanStore.uploadBatch | J20 | PASS | - | docs/tests/role7/evidence/client/J20.txt |
| FR3, QR1 | LocalSessionPlanStore.save | J21 | FAIL | D03 | docs/tests/role7/evidence/client/J21.txt |
| FR6, QR1 | FileStoreSupport.read | J22 | FAIL | D04 | docs/tests/role7/evidence/client/J22.txt |
| FR4 | SessionPlanService.validate | J23 | FAIL | D05 | docs/tests/role7/evidence/client/J23.txt |
| FR5, QR1 | LocalOutboxRepository.findPending | J24 | FAIL | D06 | docs/tests/role7/evidence/client/J24.txt |
| AC1 | DesktopShellFrame form source | J25 | FAIL | D07 | docs/tests/role7/evidence/client/J25.txt |
| FR3, FR4 | Validator.checkPlan | S01 | PASS | - | docs/tests/role7/evidence/server/S01.txt |
| FR3, FR4 | Validator.checkPlan | S02 | PASS | - | docs/tests/role7/evidence/server/S02.txt |
| FR3 | Validator.checkPlan | S03 | PASS | - | docs/tests/role7/evidence/server/S03.txt |
| FR5 | Validator.checkPlan | S04 | PASS | - | docs/tests/role7/evidence/server/S04.txt |
| FR3 | Validator.checkPlan | S05 | PASS | - | docs/tests/role7/evidence/server/S05.txt |
| FR4 | Validator.checkPlan | S06 | FAIL | D08 | docs/tests/role7/evidence/server/S06.txt |
| FR3, FR5 | Server.encode and Server.decode | S07 | PASS | - | docs/tests/role7/evidence/server/S07.txt |
| SUS1 | Server.handle GET /health | S08 | PASS | - | docs/tests/role7/evidence/server/S08.txt |
| FR5 | Server.handleSync | S09 | PASS | - | docs/tests/role7/evidence/server/S09.txt |
| QR2 | Server.handleSync | S10 | PASS | - | docs/tests/role7/evidence/server/S10.txt |
| FR5 | HttpRemoteSessionPlanStore and Server.handle | S11 | FAIL | D09 | docs/tests/role7/evidence/server/S11.txt |
| FR3, FR5 | ServerClient.encode and decode | P01 | PASS | - | docs/tests/role7/evidence/prototype/P01.txt |
| FR5, QR1 | Outbox.save and load | P02 | PASS | - | docs/tests/role7/evidence/prototype/P02.txt |
| FR5, QR1 | Outbox | P03 | PASS | - | docs/tests/role7/evidence/prototype/P03.txt |
| FR5, QR1 | Outbox.save | P04 | FAIL | D10 | docs/tests/role7/evidence/prototype/P04.txt |
| FR5, QR1 | Final GUI/server | M01 | BLOCKED HERE | Unexecuted check | REMAINING_CHECKS.md |
| AC1 | Final GUI/server | M02 | NOT RUN | Unexecuted check | REMAINING_CHECKS.md |
| AC1 | Final GUI/server | M03 | NOT RUN | Unexecuted check | REMAINING_CHECKS.md |
| AC1, QR2 | Final GUI/server | M04 | NOT RUN | Unexecuted check | REMAINING_CHECKS.md |
| FR5, QR1, QR2 | Final GUI/server | M05 | NOT RUN | Unexecuted check | REMAINING_CHECKS.md |
| QR2 | Final GUI/server | M06 | NOT RUN | Unexecuted check | REMAINING_CHECKS.md |
