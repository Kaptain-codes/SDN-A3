# STEMMate A4 Requirements (Final)

System Sculptors | SDN621S A4 | Updated 6 November 2026

The A4 baseline has 13 requirements: the A3 set (FR1-FR8, QR1, QR2, SUS1, AC1) plus one new requirement, QR3, which makes desktop and mobile support equal.

## 1. Scope

The prototype is built around two priority workflows and one recovery workflow. FR2, FR6 and QR1 support these workflows. FR7 and FR8 are deferred.

- **Workflow 1, find an activity:** FR1, supported by FR2.
- **Workflow 2, create and save a session plan:** FR3 and FR4, supported by FR6 and QR1.
- **Workflow 3, synchronise an offline plan (offline, failure and recovery):** FR5, supported by QR1, QR2 and SUS1.
- **All workflows:** FR1-FR6 are delivered on desktop and mobile devices equally (QR3).

## 2. Requirements

| ID | Requirement | Priority | Acceptance indicator |
| --- | --- | --- | --- |
| FR1 | Browse and filter activities by level, topic, duration and materials | Must | Filters can be applied and produce relevant activities, including offline |
| FR2 | Save suitable activities for offline use | Must | A saved activity opens without an internet connection |
| FR3 | Create and save a session plan using a selected activity | Must | A session plan can be created and saved for a chosen activity |
| FR4 | Session plan contains steps, timing, materials, safety notes and inclusion prompts | Must | A saved plan displays all five elements |
| FR5 | Plans completed offline are queued and synchronised when the connection returns | Must | A plan saved offline shows "waiting to sync", then "synced" after reconnecting, with no duplicates \[add time limit\] |
| FR6 | A plan in progress is saved automatically | Must | A draft is restored with all fields after closing the app or losing connection |
| FR7 | Equipment custodian records kit date, responsible person and return status | Should (deferred) | A kit record shows the date, responsible person and return status |
| FR8 | Duplicate a previous session plan as a starting point | Could (deferred) | A new plan can be created from an existing plan |
| QR1 | Work saved on the device is never deleted until the server confirms it | Must | Connection is lost mid-save and the record is still present and later syncs |
| QR2 | Sync uses small batches and no large assets, and the client is usable on low-specification shared devices, with a warning at logout if items are unsynced | Must | Sync completes on a throttled connection with status shown; the client responds acceptably on a low-spec device \[define measure\]; logging out with queued items shows the warning |
| QR3 | The system shall provide the core workflows (FR1-FR6) with equal priority on desktop/laptop devices and on mobile devices | Must (both device types) | The find, plan, save offline and sync workflow is completed on a laptop and on a phone. All five plan elements are shown, the status is visible, and no work is lost when the connection is removed mid-task |
| SUS1 | Sync failures are recorded in an administrator-visible log, and setup and recovery documentation is provided so a local technician can maintain the system | Should | An administrator can view recent sync failures, and a written setup and recovery guide exists |
| AC1 | The prototype meets the WCAG 2.2 AA checks listed in the brief | Must | The accessibility checklist is completed and fixes are recorded |

## 3. Change log (A3 baseline to A4)

| Date | Changed by | Change | Reason | Affected items |
| --- | --- | --- | --- | --- |
| 6 Nov 2026 | Project manager | Added QR3: desktop and mobile supported equally. All A3 requirement IDs and wording are unchanged. | A1 never stated a device type, only that devices may be shared or low-specification. External evidence shows both laptops and phones in use by Namibian teachers. | Architecture views, device-support ADR, UI layouts, tests, traceability matrix |

## 4. Evidence for QR3

- **A1 dossier:** E5 (a shared phone may be the only device; the screen must stay clear; no lost draft), the constraint "shared or low-specification devices", and the minimum viable product bullet "save an activity for offline use on a shared or low-specification device".
- **External:** a survey of 90 primary teachers in Omusati Region found laptops reported by 92.2%, smartphones by 73.3% and projectors by 70.0%. A separate survey of 72 high-school teachers found mobile devices already used for academic purposes. The studies cover general teaching, not STEM lessons specifically, and show overlapping use, not frequency.

## 5. Sources

&#91;1\] Jatileni, M. & Jatileni, C. N. (2018). [Teachers' perception on the use of ICT in Teaching and Learning: A Case of Namibian Primary Education](https://www.researchgate.net/publication/360450338_Teachers%27_perception_on_the_use_of_ICT_in_Teaching_and_Learning_A_Case_of_Namibian_Primary_Education). Master's thesis; 90 teachers in Omusati Region.

&#91;2\] Osakwe, J. O., Ujakpa, M., Iyawa, G. & Florich, K. (2019). [Enabling Quality Education in Namibia Through Mobile Learning Technologies](https://doi.org/10.23919/ISTAFRICA.2019.8764843). IST-Africa conference paper; 72 high-school teachers across three regions.
